package net.lecousin.framework.application.libraries.classloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.lecousin.framework.application.libraries.classpath.DefaultLibrariesManager;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.provider.FileIOProvider;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.memory.IMemoryManageable;
import net.lecousin.framework.memory.MemoryManager;

/**
 * JAR class loader.
 */
@SuppressWarnings("squid:S5042") // the zip file contains the classes...
public class ZipClassLoader extends AbstractClassLoader implements IMemoryManageable {

	static {
		ClassLoader.registerAsParallelCapable();
	}
	
	/** Constructor. */
	public ZipClassLoader(AppClassLoader appClassLoader, IOProvider.Readable provider) {
		super(appClassLoader);
		this.zipProvider = provider;
		MemoryManager.register(this);
	}
	
	private IOProvider.Readable zipProvider;
	private ZipFile zip = null;
	private long lastAccess = 0;

	@SuppressWarnings("squid:S2093") // io is closed but asynchronously
	private synchronized ZipFile getZip() throws IOException {
		lastAccess = System.currentTimeMillis();
		if (zip != null) return zip;
		if (zipProvider instanceof FileIOProvider) {
			zip = new ZipFile(((FileIOProvider)zipProvider).getFile());
			return zip;
		}
		IO.Readable io = null;
		try {
			io = zipProvider.provideIOReadable(Task.PRIORITY_URGENT);
			IO i = io;
			do {
				if (i instanceof FileIO) {
					this.zip = new ZipFile(((FileIO)i).getFile());
					break;
				}
				i = i.getWrappedIO();
			} while (i != null);
			if (i == null) {
				try {
					File temp = IOUtil.toTempFile(io).blockResult(0);
					this.zip = new ZipFile(temp);
				} catch (CancelException e) {
					throw IO.error(e);
				}
			}
		} finally {
			if (io != null) io.closeAsync();
		}
		return zip;
	}
	
	@Override
	public String getDescription() {
		return "ZipClassLoader: " + zipProvider.getDescription() + " (" + (zip == null ? "unloaded" : "loaded") + ")";
	}
	
	@Override
	public List<String> getItemsDescription() {
		return null;
	}
	
	@Override
	public String toString() { return getDescription(); }
	
	@Override
	public synchronized void freeMemory(FreeMemoryLevel level) {
		if (zip == null) return;
		long max;
		// skip checkstyle: OneStatementPerLine
		switch (level) {
		default:
		case EXPIRED_ONLY:	max = 30L * 60 * 1000; break;
		case LOW:			max = 15L * 60 * 1000; break;
		case MEDIUM:		max =  5L * 60 * 1000; break;
		case URGENT:		max = 0; break;
		}
		long now = System.currentTimeMillis();
		if (now - lastAccess < max) return;
		try { zip.close(); }
		catch (IOException e) { /* ignore */ }
		zip = null;
	}

	@Override
	protected byte[] loadFile(String name) throws IOException {
		ZipFile zipFile = getZip();
		ZipEntry entry = zipFile.getEntry(name);
		if (entry == null) throw new FileNotFoundException(name + " in zip file " + zipProvider.getDescription());
		try (InputStream in = zipFile.getInputStream(entry)) {
			int size = (int)entry.getSize();
			byte[] buffer = new byte[size];
			int pos = 0;
			do {
				int nb = in.read(buffer, pos, size - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < size);
			if (pos < size) throw new IOException("File truncated: " + entry.getName());
			return buffer;
		}
	}

	@Override
	public IO.Readable loadResourceAsIO(String name, byte priority) throws IOException {
		ZipFile zipFile = getZip();
		ZipEntry entry = zipFile.getEntry(name);
		if (entry == null) throw new FileNotFoundException(name + " in zip file " + zipProvider.getDescription());
		return new IOFromInputStream.KnownSize(
			zipFile.getInputStream(entry), entry.getSize(),
			zipProvider.getDescription() + "/" + name,
			Threading.getUnmanagedTaskManager(), priority);
	}
	
	@Override
	protected Object getResourcePointer(String path) {
		ZipFile zipFile;
		try  { zipFile = getZip(); } catch (IOException e) { return null; }
		ZipEntry entry = zipFile.getEntry(path);
		if (entry == null) return null;
		if (entry.isDirectory()) return null;
		return entry;
	}
	
	@Override
	protected IO.Readable openResourcePointer(Object pointer, byte priority) throws IOException {
		ZipFile zipFile = getZip();
		ZipEntry entry = (ZipEntry)pointer;
		return new IOFromInputStream.KnownSize(
			zipFile.getInputStream(entry), entry.getSize(),
			zipProvider.getDescription() + "/" + entry.getName(),
			Threading.getUnmanagedTaskManager(), priority);
	}
	
	@Override
	protected URL loadResourceURL(String name) {
		try {
			ZipFile zipFile = getZip();
			ZipEntry entry = zipFile.getEntry(name);
			if (entry == null) return null;
			return new URL((URL)null, "classpath://lc/" + name, new ZipURLStreamHandler());
		} catch (IOException e) {
			return null;
		}
	}
	
	private class ZipURLStreamHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) {
			return new ZipURLConnection(u);
		}
	}
	
	private class ZipURLConnection extends URLConnection {
		ZipURLConnection(URL u) {
			super(u);
		}
		
		@Override
		public void connect() {
			// nothing to do
		}
		
		@Override
		public int getContentLength() {
			try {
				ZipFile zipFile = getZip();
				ZipEntry entry = zipFile.getEntry(url.getPath().substring(1));
				return (int)entry.getSize();
			} catch (IOException e) {
				return 0;
			}
		}
		
		@Override
		public InputStream getInputStream() throws IOException {
			ZipFile zipFile = getZip();
			ZipEntry entry = zipFile.getEntry(url.getPath().substring(1));
			return zipFile.getInputStream(entry);
		}
	}
	
	@Override
	protected void scan(
		String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		try {
			DefaultLibrariesManager.scanJarLibrary(this, getZip(),
				rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
		} catch (Exception t) {
			// ignore
		}
	}

}

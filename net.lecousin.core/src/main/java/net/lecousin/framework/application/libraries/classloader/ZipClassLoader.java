package net.lecousin.framework.application.libraries.classloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.lecousin.framework.application.libraries.classpath.DefaultLibrariesManager;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.provider.FileIOProvider;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.memory.IMemoryManageable;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.util.Filter;

/**
 * JAR class loader.
 */
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

	@SuppressWarnings("resource")
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
		case EXPIRED_ONLY:	max = 30 * 60 * 1000; break;
		case LOW:			max = 15 * 60 * 1000; break;
		case MEDIUM:		max =  5 * 60 * 1000; break;
		case URGENT:		max = 0; break;
		}
		long now = System.currentTimeMillis();
		if (now - lastAccess < max) return;
		try { zip.close(); }
		catch (IOException e) { /* ignore */ }
		zip = null;
	}

	@SuppressWarnings("resource")
	@Override
	protected byte[] loadFile(String name) throws IOException {
		ZipFile zip = getZip();
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) throw new FileNotFoundException(name + " in zip file " + zipProvider.getDescription());
		try (InputStream in = zip.getInputStream(entry)) {
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

	@SuppressWarnings("resource")
	@Override
	public IO.Readable loadResourceAsIO(String name, byte priority) throws IOException {
		ZipFile zip = getZip();
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) throw new FileNotFoundException(name + " in zip file " + zipProvider.getDescription());
		return new IOFromInputStream.KnownSize(
			zip.getInputStream(entry), entry.getSize(),
			zipProvider.getDescription() + "/" + name,
			Threading.getUnmanagedTaskManager(), priority);
	}
	
	@Override
	protected Object getResourcePointer(String path) {
		ZipFile zip;
		try  { zip = getZip(); } catch (IOException e) { return null; }
		ZipEntry entry = zip.getEntry(path);
		if (entry == null) return null;
		if (entry.isDirectory()) return null;
		return entry;
	}
	
	@Override
	protected IO.Readable openResourcePointer(Object pointer, byte priority) throws IOException {
		ZipFile zip = getZip();
		ZipEntry entry = (ZipEntry)pointer;
		return new IOFromInputStream.KnownSize(
			zip.getInputStream(entry), entry.getSize(),
			zipProvider.getDescription() + "/" + entry.getName(),
			Threading.getUnmanagedTaskManager(), priority);
	}
	
	@SuppressWarnings("resource")
	@Override
	protected URL loadResourceURL(String name) {
		try {
			ZipFile zip = getZip();
			ZipEntry entry = zip.getEntry(name);
			if (entry == null) return null;
			try { return new URL((URL)null, "classpath://lc/" + name, new ZipURLStreamHandler()); }
			catch (MalformedURLException e) { return null; }
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
		}
		
		@SuppressWarnings("resource")
		@Override
		public int getContentLength() {
			try {
				ZipFile zip = getZip();
				ZipEntry entry = zip.getEntry(url.getPath().substring(1));
				return (int)entry.getSize();
			} catch (IOException e) {
				return 0;
			}
		}
		
		@SuppressWarnings("resource")
		@Override
		public InputStream getInputStream() throws IOException {
			ZipFile zip = getZip();
			ZipEntry entry = zip.getEntry(url.getPath().substring(1));
			return zip.getInputStream(entry);
		}
	}
	
	@Override
	protected void scan(
		String rootPackage, boolean includeSubPackages,
		Filter<String> packageFilter, Filter<String> classFilter, Listener<Class<?>> classScanner
	) {
		try {
			DefaultLibrariesManager.scanJarLibrary(this, getZip(),
				rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
		} catch (Throwable t) {
			// ignore
		}
	}

}

package net.lecousin.framework.application.libraries.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

/**
 * Class loader from a directory containing class files.
 */
public class DirectoryClassLoader extends AbstractClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}
	
	/** Constructor. */
	public DirectoryClassLoader(AppClassLoader appClassLoader, File dir) {
		super(appClassLoader);
		this.dir = dir;
	}
	
	private File dir;
	
	@Override
	public String getDescription() { return "DirectoryClassLoader from " + dir.getAbsolutePath(); }
	
	@Override
	public String toString() { return getDescription(); }
	
	@Override
	protected byte[] loadFile(String name) throws IOException {
		File file = new File(dir, name);
		if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
		try (FileInputStream in = new FileInputStream(file)) {
			int size = (int)file.length();
			byte[] buffer = new byte[size];
			int pos = 0;
			do {
				int nb = in.read(buffer, pos, size - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < size);
			if (pos < size) throw new IOException("File truncated: " + file.getAbsolutePath());
			return buffer;
		}
	}
	
	@Override
	public IO.Readable loadResourceAsIO(String name, byte priority) throws IOException {
		File file = new File(dir, name);
		if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
		return new FileIO.ReadOnly(file, priority);
	}
	
	@Override
	protected URL loadResourceURL(String name) {
		File file = new File(dir, name);
		if (!file.exists()) return null;
		try { return file.toURI().toURL(); }
		catch (MalformedURLException e) { return null; }
	}
}

package net.lecousin.framework.io.provider;

import java.io.File;

import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

/** IOProvider from a file. */
public class FileIOProvider implements IOProvider.ReadWrite.Seekable {

	/** IOProvider from a file. */
	public FileIOProvider(File file) {
		if (file.isDirectory()) throw new IllegalArgumentException("Cannot provide IO on a directory");
		this.file = file;
	}
	
	private File file;
	
	public File getFile() { return file; }
	
	@Override
	public IO.Readable.Seekable provideIOReadableSeekable(byte priority) {
		return new FileIO.ReadOnly(file, priority);
	}
	
	@Override
	public IO.Writable.Seekable provideIOWritableSeekable(byte priority) {
		return new FileIO.WriteOnly(file, priority);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> T provideIOReadWriteSeekable(byte priority) {
		return (T)new FileIO.ReadWrite(file, priority);
	}
	
	@Override
	public String getDescription() { return file.getAbsolutePath(); }
}

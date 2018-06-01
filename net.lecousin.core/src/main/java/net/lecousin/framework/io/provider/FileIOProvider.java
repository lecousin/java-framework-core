package net.lecousin.framework.io.provider;

import java.io.File;

import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

/** IOProvider from a file. */
public class FileIOProvider implements IOProvider.ReadWrite.Seekable.KnownSize.Resizable {

	/** IOProvider from a file. */
	public FileIOProvider(File file) {
		if (file.isDirectory()) throw new IllegalArgumentException("Cannot provide IO on a directory");
		this.file = file;
	}
	
	private File file;
	
	public File getFile() { return file; }
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IO.Readable.Seekable & IO.KnownSize> T provideIOReadableSeekableKnownSize(byte priority) {
		return (T)new FileIO.ReadOnly(file, priority);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IO.Writable.Seekable & IO.Resizable> T provideIOWritableSeekableResizable(byte priority) {
		return (T)new FileIO.WriteOnly(file, priority);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable>
	T provideIOReadWriteSeekableResizable(byte priority) {
		return (T)new FileIO.ReadWrite(file, priority);
	}
	
	@Override
	public String getDescription() { return file.getAbsolutePath(); }
}

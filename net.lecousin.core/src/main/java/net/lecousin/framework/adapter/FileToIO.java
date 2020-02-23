package net.lecousin.framework.adapter;

import java.io.File;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

/**
 * Contains classes to convert a file into an IO.
 */
public final class FileToIO {
	
	private FileToIO() { /* no instance */ }
	
	/** Convert a File into an IO.Readable by opening it. */
	public static class Readable implements Adapter<File,IO.Readable.Seekable> {
		@Override
		public Class<File> getInputType() { return File.class; }
		
		@Override
		public Class<IO.Readable.Seekable> getOutputType() { return IO.Readable.Seekable.class; }
		
		@Override
		public boolean canAdapt(File input) {
			return input.isFile();
		}
		
		@Override
		public IO.Readable.Seekable adapt(File input) {
			return new FileIO.ReadOnly(input, Priority.NORMAL);
		}
	}
	
	/** Convert a File into an IO.Writable by opening it. */
	public static class Writable implements Adapter<File,IO.Writable.Seekable> {
		@Override
		public Class<File> getInputType() { return File.class; }
		
		@Override
		public Class<IO.Writable.Seekable> getOutputType() { return IO.Writable.Seekable.class; }
		
		@Override
		public boolean canAdapt(File input) {
			return input.isFile();
		}
		
		@Override
		public IO.Writable.Seekable adapt(File input) {
			return new FileIO.ReadWrite(input, Priority.NORMAL);
		}
	}
	
}

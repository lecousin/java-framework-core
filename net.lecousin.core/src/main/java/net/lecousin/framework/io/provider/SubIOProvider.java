package net.lecousin.framework.io.provider;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.SubIO;

/** IOProvider based on an existing IO, providing a SubIO on it. */
public interface SubIOProvider {

	/** Readable SubIO provider. */
	public static class Readable implements IOProvider.Readable.Seekable {
		
		/** Constructor. */
		public Readable(IO.Readable.Seekable src, long start, long size, String description) {
			this.src = src;
			this.start = start;
			this.size = size;
			this.description = description;
		}
		
		private IO.Readable.Seekable src;
		private long start;
		private long size;
		private String description;
		
		@Override
		public String getDescription() {
			return description;
		}
		
		@Override
		public IO.Readable.Seekable provideIOReadableSeekable(byte priority) {
			return new SubIO.Readable.Seekable(src, start, size, description, false);
		}
		
	}
	
}

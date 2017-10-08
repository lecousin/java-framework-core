package net.lecousin.framework.io.provider;

import java.io.IOException;

import net.lecousin.framework.io.IO;

/**
 * An IO provider is simply capable to open an IO.
 * This allows to open an IO on demand.
 */
public interface IOProvider {

	/** Description. */
	public String getDescription();
	
	/** Provide a IO.Readable. */
	public static interface Readable extends IOProvider {
		
		/** Provide a IO.Readable. */
		public IO.Readable provideIOReadable(byte priority) throws IOException;
		
		/** Provide a IO.Readable.Seekable. */
		public static interface Seekable extends Readable {
			@Override
			public default IO.Readable provideIOReadable(byte priority) throws IOException {
				return provideIOReadableSeekable(priority);
			}

			/** Provide a IO.Readable.Seekable. */
			public IO.Readable.Seekable provideIOReadableSeekable(byte priority) throws IOException;
			
			/** Provide a IO.Readable.Seekable with DeterminedSize. */
			public static interface DeterminedSize extends Seekable {
				@Override
				public default IO.Readable.Seekable provideIOReadableSeekable(byte priority) throws IOException {
					return provideIOReadableSeekableDeterminedSize(priority);
				}

				/** Provide a IO.Readable.Seekable with DeterminedSize. */
				public <T extends IO.Readable.Seekable & IO.KnownSize>
				T provideIOReadableSeekableDeterminedSize(byte priority) throws IOException;
			}
		}
	}
	
	/** Provider a IO.Writable. */
	public static interface Writable extends IOProvider {
		/** Provider a IO.Writable. */
		public IO.Writable provideIOWritable(byte priority) throws IOException;
		
		/** Provider a IO.Writable.Seekable. */
		public static interface Seekable extends Writable {
			@Override
			public default IO.Writable provideIOWritable(byte priority) throws IOException {
				return provideIOWritableSeekable(priority);
			}

			/** Provider a IO.Writable.Seekable. */
			public IO.Writable.Seekable provideIOWritableSeekable(byte priority) throws IOException;
		}
	}
	
	/** Provider a IO Readable and Writable. */
	public static interface ReadWrite extends Readable, Writable {
		/** Provider a IO Readable and Writable. */
		public <T extends IO.Readable & IO.Writable> T provideIOReadWrite(byte priority);
		
		/** Provider a IO Readable.Seekable and Writable.Seekable. */
		public static interface Seekable extends ReadWrite, Readable.Seekable, Writable.Seekable {
			/** Provider a IO Readable.Seekable and Writable.Seekable. */
			public <T extends IO.Readable.Seekable & IO.Writable.Seekable> T provideIOReadWriteSeekable(byte priority);
			
			@Override
			public default <T extends IO.Readable & IO.Writable> T provideIOReadWrite(byte priority) {
				return provideIOReadWriteSeekable(priority);
			}
		}
	}
	
}

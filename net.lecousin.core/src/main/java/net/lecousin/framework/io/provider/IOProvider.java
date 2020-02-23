package net.lecousin.framework.io.provider;

import java.io.IOException;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.IO;

/**
 * An IO provider is simply capable to open an IO.
 * This allows to open an IO on demand.
 */
public interface IOProvider {

	/** Description. */
	String getDescription();
	
	/** Provider for a IO.Readable. */
	interface Readable extends IOProvider {
		
		/** Provide a IO.Readable. */
		IO.Readable provideIOReadable(Priority priority) throws IOException;
		

		/** Provider for a IO.Readable with KnownSize. */
		interface KnownSize extends Readable {
			@Override
			default IO.Readable provideIOReadable(Priority priority) throws IOException {
				return provideIOReadableKnownSize(priority);
			}

			/** Provide a IO.Readable with KnownSize. */
			<T extends IO.Readable & IO.KnownSize>
			T provideIOReadableKnownSize(Priority priority) throws IOException;
		}
		
		/** Provider for a IO.Readable.Seekable. */
		interface Seekable extends Readable {
			@Override
			default IO.Readable provideIOReadable(Priority priority) throws IOException {
				return provideIOReadableSeekable(priority);
			}

			/** Provide a IO.Readable.Seekable. */
			IO.Readable.Seekable provideIOReadableSeekable(Priority priority) throws IOException;
			
			/** Provider for a IO.Readable.Seekable with KnownSize. */
			interface KnownSize extends Seekable, IOProvider.Readable.KnownSize {
				@Override
				default IO.Readable.Seekable provideIOReadableSeekable(Priority priority) throws IOException {
					return provideIOReadableSeekableKnownSize(priority);
				}
				
				@Override
				default <T extends IO.Readable & IO.KnownSize> T provideIOReadableKnownSize(Priority priority) throws IOException {
					return provideIOReadableSeekableKnownSize(priority);
				}

				@Override
				default IO.Readable provideIOReadable(Priority priority) throws IOException {
					return provideIOReadableSeekableKnownSize(priority);
				}

				/** Provide a IO.Readable.Seekable with KnownSize. */
				<T extends IO.Readable.Seekable & IO.KnownSize>
				T provideIOReadableSeekableKnownSize(Priority priority) throws IOException;
			}
		}
	}
	
	/** Provider for a IO.Writable. */
	interface Writable extends IOProvider {
		/** Provide a IO.Writable. */
		IO.Writable provideIOWritable(Priority priority) throws IOException;


		/** Provider for a IO.Writable with KnownSize. */
		interface KnownSize extends Writable {
			@Override
			default IO.Writable provideIOWritable(Priority priority) throws IOException {
				return provideIOWritableKnownSize(priority);
			}
			
			/** Provide a IO.Writable with IO.KnownSize. */
			<T extends IO.Writable & IO.KnownSize>
			T provideIOWritableKnownSize(Priority priority) throws IOException;
			
			/** Provider for a IO.Writable with Resizable. */
			interface Resizable extends KnownSize {
				@Override
				default <T extends IO.Writable & IO.KnownSize>
				T provideIOWritableKnownSize(Priority priority) throws IOException {
					return provideIOWritableResizable(priority);
				}
				
				/** Provide a IO.Writable with Resizable. */
				<T extends IO.Writable & IO.Resizable>
				T provideIOWritableResizable(Priority priority) throws IOException;
			}
		}
		
		/** Provider for a IO.Writable.Seekable. */
		interface Seekable extends Writable {
			@Override
			default IO.Writable provideIOWritable(Priority priority) throws IOException {
				return provideIOWritableSeekable(priority);
			}

			/** Provide a IO.Writable.Seekable. */
			IO.Writable.Seekable provideIOWritableSeekable(Priority priority) throws IOException;
			
			/** Provider for a IO.Writable.Seekable with KnownSize. */
			interface KnownSize extends Seekable, IOProvider.Writable.KnownSize {
				@Override
				default IO.Writable.Seekable provideIOWritableSeekable(Priority priority) throws IOException {
					return provideIOWritableSeekableKnownSize(priority);
				}
				
				@Override
				default <T extends IO.Writable & IO.KnownSize> T provideIOWritableKnownSize(Priority priority) throws IOException {
					return provideIOWritableSeekableKnownSize(priority);
				}
				
				@Override
				default IO.Writable provideIOWritable(Priority priority) throws IOException {
					return Seekable.super.provideIOWritable(priority);
				}
				
				/** Provide a IO.Writable.Seekable with IO.KnownSize. */
				<T extends IO.Writable.Seekable & IO.KnownSize>
				T provideIOWritableSeekableKnownSize(Priority priority) throws IOException;
				
				/** Provider for a IO.Writable.Seekable with Resizable. */
				interface Resizable extends IOProvider.Writable.Seekable.KnownSize, IOProvider.Writable.KnownSize.Resizable {
					@Override
					default <T extends IO.Writable.Seekable & IO.KnownSize>
					T provideIOWritableSeekableKnownSize(Priority priority) throws IOException {
						return provideIOWritableSeekableResizable(priority);
					}
					
					@Override
					default <T extends IO.Writable & IO.Resizable>
					T provideIOWritableResizable(Priority priority) throws IOException {
						return provideIOWritableSeekableResizable(priority);
					}

					@Override
					default <T extends IO.Writable & IO.KnownSize>
					T provideIOWritableKnownSize(Priority priority) throws IOException {
						return provideIOWritableSeekableResizable(priority);
					}
					
					/** Provide a IO.Writable.Seekable with Resizable. */
					<T extends IO.Writable.Seekable & IO.Resizable>
					T provideIOWritableSeekableResizable(Priority priority) throws IOException;
				}
			}
		}
	}
	
	/** Provider a IO Readable and Writable. */
	interface ReadWrite extends Readable, Writable {
		@Override
		default IO.Readable provideIOReadable(Priority priority) throws IOException {
			return provideIOReadWrite(priority);
		}
		
		@Override
		default IO.Writable provideIOWritable(Priority priority) throws IOException {
			return provideIOReadWrite(priority);
		}
		
		/** Provider a IO Readable and Writable. */
		<T extends IO.Readable & IO.Writable> T provideIOReadWrite(Priority priority) throws IOException;
		
		interface KnownSize extends ReadWrite, Readable.KnownSize, Writable.KnownSize {
			@Override
			default <T extends IO.Readable & IO.Writable> T provideIOReadWrite(Priority priority) throws IOException {
				return provideIOReadWriteKnownSize(priority);
			}
			
			@Override
			default IO.Readable provideIOReadable(Priority priority) throws IOException {
				return provideIOReadWriteKnownSize(priority);
			}
			
			@Override
			default <T extends IO.Readable & IO.KnownSize> T provideIOReadableKnownSize(Priority priority) throws IOException {
				return provideIOReadWriteKnownSize(priority);
			}

			@Override
			default IO.Writable provideIOWritable(Priority priority) throws IOException {
				return provideIOReadWriteKnownSize(priority);
			}
			
			@Override
			default <T extends IO.Writable & IO.KnownSize> T provideIOWritableKnownSize(Priority priority) throws IOException {
				return provideIOReadWriteKnownSize(priority);
			}
			
			<T extends IO.Readable & IO.Writable & IO.KnownSize>
			T provideIOReadWriteKnownSize(Priority priority) throws IOException;

			interface Resizable extends IOProvider.ReadWrite.KnownSize, IOProvider.Writable.KnownSize.Resizable {
				@Override
				default <T extends IO.Readable & IO.Writable & IO.KnownSize>
				T provideIOReadWriteKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteResizable(priority);
				}

				@Override
				default <T extends IO.Writable & IO.KnownSize> T provideIOWritableKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteResizable(priority);
				}

				<T extends IO.Readable & IO.Writable & IO.Resizable>
				T provideIOReadWriteResizable(Priority priority) throws IOException;
			}
		}
		
		/** Provider a IO Readable.Seekable and Writable.Seekable. */
		interface Seekable extends ReadWrite, Readable.Seekable, Writable.Seekable {
			/** Provider a IO Readable.Seekable and Writable.Seekable. */
			<T extends IO.Readable.Seekable & IO.Writable.Seekable> T provideIOReadWriteSeekable(Priority priority) throws IOException;

			@Override
			default IO.Readable provideIOReadable(Priority priority) throws IOException {
				return provideIOReadWriteSeekable(priority);
			}

			@Override
			default IO.Readable.Seekable provideIOReadableSeekable(Priority priority) throws IOException {
				return provideIOReadWriteSeekable(priority);
			}
			
			@Override
			default IO.Writable provideIOWritable(Priority priority) throws IOException {
				return provideIOReadWriteSeekable(priority);
			}
			
			@Override
			default IO.Writable.Seekable provideIOWritableSeekable(Priority priority) throws IOException {
				return provideIOReadWriteSeekable(priority);
			}
			
			@Override
			default <T extends IO.Readable & IO.Writable> T provideIOReadWrite(Priority priority) throws IOException {
				return provideIOReadWriteSeekable(priority);
			}
			
			interface KnownSize
			extends IOProvider.ReadWrite.Seekable, IOProvider.ReadWrite.KnownSize,
				IOProvider.Readable.Seekable.KnownSize, IOProvider.Writable.Seekable.KnownSize {
				@Override
				default <T extends IO.Readable.Seekable & IO.Writable.Seekable>
				T provideIOReadWriteSeekable(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Readable & IO.Writable> T provideIOReadWrite(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Readable & IO.Writable & IO.KnownSize>
				T provideIOReadWriteKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}
				
				@Override
				default IO.Readable provideIOReadable(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default IO.Writable provideIOWritable(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default IO.Readable.Seekable provideIOReadableSeekable(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Readable & IO.KnownSize> T provideIOReadableKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Readable.Seekable & IO.KnownSize>
				T provideIOReadableSeekableKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Writable & IO.KnownSize> T provideIOWritableKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default <T extends IO.Writable.Seekable & IO.KnownSize>
				T provideIOWritableSeekableKnownSize(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}

				@Override
				default IO.Writable.Seekable provideIOWritableSeekable(Priority priority) throws IOException {
					return provideIOReadWriteSeekableKnownSize(priority);
				}
				
				<T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize>
				T provideIOReadWriteSeekableKnownSize(Priority priority) throws IOException;

				interface Resizable
				extends IOProvider.ReadWrite.Seekable.KnownSize, IOProvider.ReadWrite.KnownSize.Resizable,
					IOProvider.Writable.Seekable.KnownSize.Resizable {
					@Override
					default <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.KnownSize>
					T provideIOReadWriteSeekableKnownSize(Priority priority) throws IOException {
						return provideIOReadWriteSeekableResizable(priority);
					}

					@Override
					default <T extends IO.Readable & IO.Writable & IO.KnownSize>
					T provideIOReadWriteKnownSize(Priority priority) throws IOException {
						return provideIOReadWriteSeekableResizable(priority);
					}

					@Override
					default <T extends IO.Writable & IO.KnownSize>
					T provideIOWritableKnownSize(Priority priority) throws IOException {
						return provideIOReadWriteSeekableResizable(priority);
					}

					@Override
					default <T extends IO.Writable.Seekable & IO.KnownSize>
					T provideIOWritableSeekableKnownSize(Priority priority) throws IOException {
						return provideIOReadWriteSeekableResizable(priority);
					}
					
					@Override
					default <T extends IO.Readable & IO.Writable & IO.Resizable>
					T provideIOReadWriteResizable(Priority priority) throws IOException {
						return provideIOReadWriteSeekableResizable(priority);
					}
					
					<T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable>
					T provideIOReadWriteSeekableResizable(Priority priority) throws IOException;
				}
			}
		}
	}
	
}

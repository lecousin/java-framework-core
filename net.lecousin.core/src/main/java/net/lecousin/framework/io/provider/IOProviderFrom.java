package net.lecousin.framework.io.provider;

/**
 * Get a IOProvider from an information.
 * @param <T> type of information
 */
public interface IOProviderFrom<T> {

	/** Get a IOProvider. */
	IOProvider get(T from);
	
	/** Get a IOProvider.Readable from an information.
	 * @param <T> type of information
	 */
	interface Readable<T> extends IOProviderFrom<T> {
		@Override
		IOProvider.Readable get(T from);

		interface KnownSize<T> extends Readable<T> {
			@Override
			IOProvider.Readable.KnownSize get(T from);
		}
		
		interface Seekable<T> extends Readable<T> {
			@Override
			IOProvider.Readable.Seekable get(T from);
			
			interface KnownSize<T> extends Seekable<T>, IOProviderFrom.Readable.KnownSize<T> {
				@Override
				IOProvider.Readable.Seekable.KnownSize get(T from);
			}
		}
	}
	
	/** Get a IOProvider.Writable from an information.
	 * @param <T> type of information
	 */
	interface Writable<T> extends IOProviderFrom<T> {
		@Override
		IOProvider.Writable get(T from);

		interface KnownSize<T> extends Writable<T> {
			@Override
			IOProvider.Writable.KnownSize get(T from);
			
			interface Resizable<T> extends KnownSize<T> {
				@Override
				IOProvider.Writable.KnownSize.Resizable get(T from);
			}
		}
		
		interface Seekable<T> extends Writable<T> {
			@Override
			IOProvider.Writable.Seekable get(T from);
			
			interface KnownSize<T> extends Seekable<T> {
				@Override
				IOProvider.Writable.Seekable.KnownSize get(T from);
				
				interface Resizable<T> extends KnownSize<T> {
					@Override
					IOProvider.Writable.Seekable.KnownSize.Resizable get(T from);
				}
			}
		}
	}
	
	/** Get a IOProvider.ReadWrite from an information.
	 * @param <T> type of information
	 */
	interface ReadWrite<T> extends Readable<T>, Writable<T> {
		@Override
		IOProvider.ReadWrite get(T from);

		interface KnownSize<T> extends ReadWrite<T> {
			@Override
			IOProvider.ReadWrite.KnownSize get(T from);
			
			interface Resizable<T> extends KnownSize<T> {
				@Override
				IOProvider.ReadWrite.KnownSize.Resizable get(T from);
			}
		}
		
		interface Seekable<T> extends ReadWrite<T> {
			@Override
			IOProvider.ReadWrite.Seekable get(T from);
			
			interface KnownSize<T> extends Seekable<T> {
				@Override
				IOProvider.ReadWrite.Seekable.KnownSize get(T from);
				
				interface Resizable<T> extends KnownSize<T> {
					@Override
					IOProvider.ReadWrite.Seekable.KnownSize.Resizable get(T from);
				}
			}
		}
	}
	
}

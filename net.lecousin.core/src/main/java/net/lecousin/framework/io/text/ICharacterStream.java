package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.IConcurrentCloseable;
import net.lecousin.framework.util.UnprotectedString;

/** Character stream. */
public interface ICharacterStream extends IConcurrentCloseable {
	
	/** Return the priority. */
	byte getPriority();
	
	/** Change the priority (see Task priorities). */
	void setPriority(byte priority);

	/** Description. */
	String getDescription();
	
	/** Return the encoding of the stream. */
	Charset getEncoding();
	
	/** Readable character stream. */
	public interface Readable extends ICharacterStream {
		/** Read characters.
		 * @return the number of character read which may be 0 or -1 in case the end of stream has been reached
		 */
		int readSync(char[] buf, int offset, int length) throws IOException;
		
		/** Read characters.
		 * @return the number of character read which may be 0 or -1 in case the end of stream has been reached
		 */
		AsyncWork<Integer, IOException> readAsync(char[] buf, int offset, int length);
		
		/** Return true if the end of the stream has been reached, and no more character can be read. */
		boolean endReached();
		
		/** Read all requested characters. */
		default int readFullySync(char[] buf, int offset, int length) throws IOException {
			int done = 0;
			do {
				int nb = readSync(buf, offset, length);
				if (nb <= 0) return done;
				done += nb;
				if (length == nb) return done;
				length -= nb;
				offset += nb;
			} while (true);
		}

		/** Read all requested characters. */
		default AsyncWork<Integer, IOException> readFullyAsync(char[] buf, int offset, int length) {
			MutableInteger done = new MutableInteger(0);
			AsyncWork<Integer, IOException> result = new AsyncWork<>();
			Runnable next = new Runnable() {
				@Override
				public void run() {
					Runnable that = this;
					AsyncWork<Integer, IOException> read = readAsync(buf, offset + done.get(), length - done.get());
					read.listenInline(
						() -> {
							int nb = read.getResult().intValue();
							if (nb <= 0) {
								result.unblockSuccess(Integer.valueOf(done.get()));
								return;
							}
							if (done.add(nb) == length) {
								result.unblockSuccess(Integer.valueOf(done.get()));
								return;
							}
							that.run();
						},
						result
					);
				}
			};
			next.run();
			return result;
		}
		
		/** Buffered readable character stream. */
		public interface Buffered extends Readable {
			/** Read one character. */
			char read() throws IOException;
			
			/** Read one character if possible.
			 * If the end of stream is reached, -1 is returned.
			 * If no more character is available, but the end of stream is not yet reached, -2 is returned.
			 */
			int readAsync() throws IOException;
			
			/** Put back one character. */
			void back(char c);
			
			/** Return a synchronization point which is unblocked once some characters have been buffered. */
			ISynchronizationPoint<IOException> canStartReading();
			
			/** Return the next buffer as soon as available, or null if then end of stream has been reached. */
			AsyncWork<UnprotectedString, IOException> readNextBufferAsync();
		}
	}
	
	/** Asynchronous writable character stream. */
	public interface WriterAsync {
		/** Write characters. */
		ISynchronizationPoint<IOException> writeAsync(char[] c, int offset, int length);
		
		/** Write characters. */
		default ISynchronizationPoint<IOException> writeAsync(char[] c) {
			return writeAsync(c, 0, c.length);
		}
		
		/** Write characters of the given string. */
		default ISynchronizationPoint<IOException> writeAsync(String s) {
			return writeAsync(s.toCharArray());
		}
	}
	
	/** Writable character stream. */
	public interface Writable extends ICharacterStream, WriterAsync {
		/** Write characters. */
		void writeSync(char[] c, int offset, int length) throws IOException;
		
		/** Write characters. */
		default void writeSync(char[] c) throws IOException {
			writeSync(c, 0, c.length);
		}
		
		/** Write characters of the given string. */
		default void writeSync(String s) throws IOException {
			writeSync(s.toCharArray());
		}
		
		/** Buffered writable character stream. */
		public interface Buffered extends Writable {
			/** Write one character. */
			void writeSync(char c) throws IOException;

			/** Write one character. */
			ISynchronizationPoint<IOException> writeAsync(char c);
			
			/** Flush any buffered character. */
			ISynchronizationPoint<IOException> flush();
		}
	}
	
}

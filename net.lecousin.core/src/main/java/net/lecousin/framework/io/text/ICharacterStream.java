package net.lecousin.framework.io.text;

import java.io.IOException;
import java.nio.charset.Charset;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.IConcurrentCloseable;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/** Character stream. */
public interface ICharacterStream extends IConcurrentCloseable<IOException> {
	
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
		AsyncSupplier<Integer, IOException> readAsync(char[] buf, int offset, int length);
		
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
		default AsyncSupplier<Integer, IOException> readFullyAsync(char[] buf, int offset, int length) {
			MutableInteger done = new MutableInteger(0);
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
			Runnable next = new Runnable() {
				@Override
				public void run() {
					Runnable that = this;
					AsyncSupplier<Integer, IOException> read = readAsync(buf, offset + done.get(), length - done.get());
					read.onDone(
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
		
		/** Readable character stream with known line and position in line. */
		public interface PositionInText extends Readable {
			/** Return the current line number. */
			int getLine();
			
			/** Return the current column number in line. */
			int getPositionInLine();
		}
		
		/** Buffered readable character stream. */
		public interface Buffered extends Readable {
			
			/** Return a synchronization point which is unblocked once some characters have been buffered. */
			IAsync<IOException> canStartReading();
			
			/** Put back one character. */
			void back(char c);

			/** Read one character. */
			char read() throws IOException;
			
			/** Read one character if possible.
			 * If the end of stream is reached, -1 is returned.
			 * If no more character is available, but the end of stream is not yet reached, -2 is returned.
			 */
			int readAsync() throws IOException;
			
			/** Return the next buffer, or null if the end of stream has been reached. */
			Chars.Readable readNextBuffer() throws IOException;
			
			/** Return the next buffer as soon as available, or null if then end of stream has been reached. */
			AsyncSupplier<Chars.Readable, IOException> readNextBufferAsync();
			
			/** Read characters until the given end, and put them in the given string.
			 * It returns true if the endChar has been found (and is not included in the string),
			 * or false if the end of stream has been reached without the ending character.
			 */
			boolean readUntil(char endChar, UnprotectedStringBuffer string) throws IOException;
			
			/** Read characters until the given end, and put them in the given string.
			 * It returns true if the endChar has been found (and is not included in the string),
			 * or false if the end of stream has been reached without the ending character.
			 */
			AsyncSupplier<Boolean, IOException> readUntilAsync(char endChar, UnprotectedStringBuffer string);
		}
	}
	
	/** Asynchronous writable character stream. */
	public interface WriterAsync {
		/** Write characters. */
		IAsync<IOException> writeAsync(char[] c, int offset, int length);
		
		/** Write characters. */
		default IAsync<IOException> writeAsync(char[] c) {
			return writeAsync(c, 0, c.length);
		}
		
		/** Write characters of the given string. */
		default IAsync<IOException> writeAsync(String s) {
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
			IAsync<IOException> writeAsync(char c);
			
			/** Flush any buffered character. */
			IAsync<IOException> flush();
		}
	}
	
}

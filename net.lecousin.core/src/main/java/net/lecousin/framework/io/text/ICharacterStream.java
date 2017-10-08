package net.lecousin.framework.io.text;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.util.AsyncCloseable;

/** Character stream. */
public interface ICharacterStream extends AutoCloseable, AsyncCloseable<IOException> {

	/** Readable character stream. */
	public interface Readable extends ICharacterStream {
		/** Description. */
		public String getSourceDescription();
		
		/** Read characters. */
		public int read(char[] buf, int offset, int length) throws IOException;
		
		/** Return true if the end of the stream has been reached, and no more character can be read. */
		public boolean endReached();
		
		/** Read all requested characters. */
		public default int readFully(char[] buf, int offset, int length) throws IOException {
			int done = 0;
			do {
				int nb = read(buf, offset, length);
				if (nb < 0) return done;
				done += nb;
				if (length == nb) return done;
				length -= nb;
				offset += nb;
			} while (true);
		}
		
		/** Buffered readable character stream. */
		public interface Buffered extends Readable {
			/** Read one character. */
			public char read() throws EOFException, IOException;
			
			/** Put back one character. */
			public void back(char c);
			
			/** Return a synchronization point which is unblocked once some characters have been buffered. */
			public ISynchronizationPoint<IOException> canStartReading();
		}
	}
	
	/** Writable character stream. */
	public interface Writable extends ICharacterStream {
		/** Write characters. */
		public void write(char[] c, int offset, int length) throws IOException;
		
		/** Write characters. */
		public default void write(char[] c) throws IOException {
			write(c, 0, c.length);
		}
		
		/** Write characters of the given string. */
		public default void write(String s) throws IOException {
			write(s.toCharArray());
		}
		
		/** Buffered writable character stream. */
		public interface Buffered extends Writable {
			/** Write one character. */
			public void write(char c) throws IOException;
			
			/** Flush any buffered character. */
			public ISynchronizationPoint<IOException> flush();
		}
	}
	
}

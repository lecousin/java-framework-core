package net.lecousin.framework.io.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.data.Bytes;

/** Utility method to encode or decode numbers in little or big endian. */
public interface DataUtil {
	
	/** Read 16-bit short value. */
	interface Read16 {

		interface BE {
			
			static short read(byte[] buffer, int offset) {
				return (short)(((buffer[offset] & 0xFF) << 8) | (buffer[offset + 1] & 0xFF));
			}
			
			static short read(ByteBuffer buf) {
				short value;
				value = (short)((buf.get() & 0xFF) << 8);
				value |= (buf.get() & 0xFF);
				return value;
			}
			
			static short read(Bytes.Readable buf) {
				short value;
				value = (short)((buf.get() & 0xFF) << 8);
				value |= (buf.get() & 0xFF);
				return value;
			}

			static short read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return (short)(i | (value << 8));
			}

			static short read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return (short)(i | (value << 8));
			}

		}
		
		interface LE {
			
			static short read(byte[] buffer, int offset) {
				return (short)((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
			}
			
			static short read(ByteBuffer buf) {
				return (short)((buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8));
			}
			
			static short read(Bytes.Readable buf) {
				return (short)((buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8));
			}
			
			static short read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return (short)(value | (i << 8));
			}
			
			static short read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return (short)(value | (i << 8));
			}

		}
		
	}
	
	/** Write 16-bit short value. */
	interface Write16 {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, short value) {
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}

			static void write(ByteBuffer buf, short value) {
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(Bytes.Writable buf, short value) {
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(IO.WritableByteStream io, short value) throws IOException {
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}

			static void write(OutputStream io, short value) throws IOException {
				io.write((value >> 8) & 0xFF);
				io.write(value & 0xFF);
			}

		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, short value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset] = (byte)((value >> 8) & 0xFF);
			}
			
			static void write(ByteBuffer buf, short value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
			}

			static void write(Bytes.Writable buf, short value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
			}

			static void write(IO.WritableByteStream io, short value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
			}

			static void write(OutputStream io, short value) throws IOException {
				io.write(value & 0xFF);
				io.write((value >> 8) & 0xFF);
			}

		}
		
	}
	
	/** Read 16-bit unsigned value. */
	interface Read16U {
		
		interface BE {
			
			static int read(byte[] buffer, int offset) {
				return ((buffer[offset] & 0xFF) << 8) | (buffer[offset + 1] & 0xFF);
			}
			
			static int read(ByteBuffer buf) {
				int value;
				value = (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}
			
			static int read(Bytes.Readable buf) {
				int value;
				value = (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 8;
				int i = io.read();
				if (i < 0) throw new EOFException();
				return value | i;
			}
			

			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 8;
				int i = io.read();
				if (i < 0) throw new EOFException();
				return value | i;
			}

		}
		
		interface LE {
			
			static int read(byte[] buffer, int offset) {
				return (buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8);
			}
			
			static int read(ByteBuffer buf) {
				return (buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8);
			}

			static int read(Bytes.Readable buf) {
				return (buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8);
			}

			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return value | (i << 8);
			}

			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				return value | (i << 8);
			}

		}
		
	}
	
	/** Write 16-bit unsigned value. */
	interface Write16U {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}
			
			static void write(ByteBuffer buf, int value) {
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}
			
			static void write(OutputStream io, int value) throws IOException {
				io.write((value >> 8) & 0xFF);
				io.write(value & 0xFF);
			}

		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset] = (byte)((value >> 8) & 0xFF);
			}
			
			static void write(ByteBuffer buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
			}

			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
			}

			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
			}

			static void write(OutputStream io, int value) throws IOException {
				io.write(value & 0xFF);
				io.write((value >> 8) & 0xFF);
			}

		}
		
	}
	
	/** Read 24-bit unsigned value. */
	interface Read24U {
		
		interface BE {
			
			static int read(byte[] buffer, int offset) {
				int value;
				value = (buffer[offset + 2] & 0xFF);
				value |= (buffer[offset + 1] & 0xFF) << 8;
				value |= (buffer[offset] & 0xFF) << 16;
				return value;
			}
			
			static int read(ByteBuffer buf) {
				int value;
				value = (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static int read(Bytes.Readable buf) {
				int value;
				value = (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}
			
			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 16;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}

			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 16;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}

		}
		
		interface LE {
			
			static int read(byte[] buffer, int offset) {
				int value;
				value = (buffer[offset] & 0xFF);
				value |= (buffer[offset + 1] & 0xFF) << 8;
				value |= (buffer[offset + 2] & 0xFF) << 16;
				return value;
			}

			static int read(ByteBuffer buf) {
				int value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				return value;
			}

			static int read(Bytes.Readable buf) {
				int value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				return value;
			}

			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				return value;
			}
			
			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				return value;
			}

		}
		
	}
	
	/** Write 24-bit unsigned value. */
	interface Write24U {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}

			static void write(ByteBuffer buf, int value) {
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}
			
			static void write(OutputStream io, int value) throws IOException {
				io.write((value >> 16) & 0xFF);
				io.write((value >> 8) & 0xFF);
				io.write(value & 0xFF);
			}
			
		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)((value >> 16) & 0xFF);
			}

			static void write(ByteBuffer buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
			}

			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
			}
			
			static void write(OutputStream io, int value) throws IOException {
				io.write(value & 0xFF);
				io.write((value >> 8) & 0xFF);
				io.write((value >> 16) & 0xFF);
			}
			
		}
		
	}
	
	/** Read 32-bit value. */
	interface Read32 {
		
		interface BE {
			
			static int read(byte[] buffer, int offset) {
				int value;
				value = (buffer[offset] & 0xFF) << 24;
				value |= (buffer[offset + 1] & 0xFF) << 16;
				value |= (buffer[offset + 2] & 0xFF) << 8;
				value |= (buffer[offset + 3] & 0xFF);
				return value;
			}

			static int read(ByteBuffer buf) {
				int value;
				value = (buf.get() & 0xFF) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static int read(Bytes.Readable buf) {
				int value;
				value = (buf.get() & 0xFF) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}
			
			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 24;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}

			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 24;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}

		}
		
		interface LE {
			
			static int read(byte[] buffer, int offset) {
				int value;
				value = (buffer[offset] & 0xFF);
				value |= (buffer[offset + 1] & 0xFF) << 8;
				value |= (buffer[offset + 2] & 0xFF) << 16;
				value |= (buffer[offset + 3] & 0xFF) << 24;
				return value;
			}
			
			static int read(ByteBuffer buf) {
				int value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 24;
				return value;
			}
			
			static int read(Bytes.Readable buf) {
				int value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 24;
				return value;
			}
			
			static int read(IO.ReadableByteStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 24;
				return value;
			}
			
			static int read(InputStream io) throws IOException {
				int value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 24;
				return value;
			}
			
		}
		
	}
	
	/** Write 32-bit value. */
	interface Write32 {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)((value >> 24) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}
			
			static void write(ByteBuffer buf, int value) {
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)((value >> 24) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}
			
			static void write(OutputStream io, int value) throws IOException {
				io.write((value >> 24) & 0xFF);
				io.write((value >> 16) & 0xFF);
				io.write((value >> 8) & 0xFF);
				io.write(value & 0xFF);
			}

		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, int value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset] = (byte)((value >> 24) & 0xFF);
			}

			static void write(ByteBuffer buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
			}

			static void write(Bytes.Writable buf, int value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, int value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 24) & 0xFF));
			}
			
			static void write(OutputStream io, int value) throws IOException {
				io.write(value & 0xFF);
				io.write((value >> 8) & 0xFF);
				io.write((value >> 16) & 0xFF);
				io.write((value >> 24) & 0xFF);
			}

		}
		
	}
	
	/** Read 32-bit unsigned value. */
	interface Read32U {
		
		interface BE {
			
			static long read(byte[] buffer, int offset) {
				long value;
				value = (buffer[offset + 3] & 0xFF);
				value |= (buffer[offset + 2] & 0xFF) << 8;
				value |= (buffer[offset + 1] & 0xFF) << 16;
				value |= ((long)(buffer[offset] & 0xFF)) << 24;
				return value;
			}
			
			static long read(ByteBuffer buf) {
				long value;
				value = ((long)(buf.get() & 0xFF)) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static long read(Bytes.Readable buf) {
				long value;
				value = ((long)(buf.get() & 0xFF)) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static long read(IO.ReadableByteStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 24;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}
			
			static long read(InputStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 24;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}

		}
		
		interface LE {
			
			static long read(byte[] buffer, int offset) {
				long value;
				value = (buffer[offset] & 0xFF);
				value |= (buffer[offset + 1] & 0xFF) << 8;
				value |= (buffer[offset + 2] & 0xFF) << 16;
				value |= ((long)(buffer[offset + 3] & 0xFF)) << 24;
				return value;
			}
			
			static long read(ByteBuffer buf) {
				long value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				return value;
			}
			
			static long read(Bytes.Readable buf) {
				long value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				return value;
			}

			static long read(IO.ReadableByteStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				return value;
			}
			
			static long read(InputStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				return value;
			}

		}
		
	}
	
	/** Write 32-bit unsigned value. */
	interface Write32U {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, long value) {
				buffer[offset++] = (byte)((value >> 24) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}

			static void write(ByteBuffer buf, long value) {
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(Bytes.Writable buf, long value) {
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, long value) throws IOException {
				io.write((byte)((value >> 24) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}

			static void write(OutputStream io, long value) throws IOException {
				io.write((int)((value >> 24) & 0xFF));
				io.write((int)((value >> 16) & 0xFF));
				io.write((int)((value >> 8) & 0xFF));
				io.write((int)(value & 0xFF));
			}

		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, long value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset] = (byte)((value >> 24) & 0xFF);
			}

			static void write(ByteBuffer buf, long value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
			}

			static void write(Bytes.Writable buf, long value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, long value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 24) & 0xFF));
			}

			static void write(OutputStream io, long value) throws IOException {
				io.write((int)(value & 0xFF));
				io.write((int)((value >> 8) & 0xFF));
				io.write((int)((value >> 16) & 0xFF));
				io.write((int)((value >> 24) & 0xFF));
			}

		}
		
	}
	
	/** Read 64-bit value. */
	interface Read64 {
		
		interface BE {
			
			static long read(byte[] buffer, int offset) {
				long value;
				value = ((long)(buffer[offset] & 0xFF)) << 56;
				value |= ((long)(buffer[offset + 1] & 0xFF)) << 48;
				value |= ((long)(buffer[offset + 2] & 0xFF)) << 40;
				value |= ((long)(buffer[offset + 3] & 0xFF)) << 32;
				value |= ((long)(buffer[offset + 4] & 0xFF)) << 24;
				value |= (buffer[offset + 5] & 0xFF) << 16;
				value |= (buffer[offset + 6] & 0xFF) << 8;
				value |= (buffer[offset + 7] & 0xFF);
				return value;
			}

			static long read(ByteBuffer buf) {
				long value;
				value = ((long)(buf.get() & 0xFF)) << 56;
				value |= ((long)(buf.get() & 0xFF)) << 48;
				value |= ((long)(buf.get() & 0xFF)) << 40;
				value |= ((long)(buf.get() & 0xFF)) << 32;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static long read(Bytes.Readable buf) {
				long value;
				value = ((long)(buf.get() & 0xFF)) << 56;
				value |= ((long)(buf.get() & 0xFF)) << 48;
				value |= ((long)(buf.get() & 0xFF)) << 40;
				value |= ((long)(buf.get() & 0xFF)) << 32;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				value |= (buf.get() & 0xFF) << 16;
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF);
				return value;
			}

			static long read(IO.ReadableByteStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 56;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 48;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 40;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 32;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}
			
			static long read(InputStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				value <<= 56;
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 48;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 40;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 32;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i;
				return value;
			}	

		}
		
		interface LE {
			
			static long read(byte[] buffer, int offset) {
				long value;
				value = (buffer[offset] & 0xFF);
				value |= (buffer[offset + 1] & 0xFF) << 8;
				value |= (buffer[offset + 2] & 0xFF) << 16;
				value |= ((long)(buffer[offset + 3] & 0xFF)) << 24;
				value |= ((long)(buffer[offset + 4] & 0xFF)) << 32;
				value |= ((long)(buffer[offset + 5] & 0xFF)) << 40;
				value |= ((long)(buffer[offset + 6] & 0xFF)) << 48;
				value |= ((long)(buffer[offset + 7] & 0xFF)) << 56;
				return value;
			}

			static long read(ByteBuffer buf) {
				long value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				value |= ((long)(buf.get() & 0xFF)) << 32;
				value |= ((long)(buf.get() & 0xFF)) << 40;
				value |= ((long)(buf.get() & 0xFF)) << 48;
				value |= ((long)(buf.get() & 0xFF)) << 56;
				return value;
			}

			static long read(Bytes.Readable buf) {
				long value;
				value = (buf.get() & 0xFF);
				value |= (buf.get() & 0xFF) << 8;
				value |= (buf.get() & 0xFF) << 16;
				value |= ((long)(buf.get() & 0xFF)) << 24;
				value |= ((long)(buf.get() & 0xFF)) << 32;
				value |= ((long)(buf.get() & 0xFF)) << 40;
				value |= ((long)(buf.get() & 0xFF)) << 48;
				value |= ((long)(buf.get() & 0xFF)) << 56;
				return value;
			}
			
			static long read(IO.ReadableByteStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 32;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 40;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 48;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 56;
				return value;
			}
			
			static long read(InputStream io) throws IOException {
				long value = io.read();
				if (value < 0) throw new EOFException();
				int i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 8;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= i << 16;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 24;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 32;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 40;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 48;
				i = io.read();
				if (i < 0) throw new EOFException();
				value |= ((long)i) << 56;
				return value;
			}
			
		}
		
	}
	
	/** Write 64-bit value. */
	interface Write64 {
		
		interface BE {
			
			static void write(byte[] buffer, int offset, long value) {
				buffer[offset++] = (byte)((value >> 56) & 0xFF);
				buffer[offset++] = (byte)((value >> 48) & 0xFF);
				buffer[offset++] = (byte)((value >> 40) & 0xFF);
				buffer[offset++] = (byte)((value >> 32) & 0xFF);
				buffer[offset++] = (byte)((value >> 24) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset] = (byte)(value & 0xFF);
			}

			static void write(ByteBuffer buf, long value) {
				buf.put((byte)((value >> 56) & 0xFF));
				buf.put((byte)((value >> 48) & 0xFF));
				buf.put((byte)((value >> 40) & 0xFF));
				buf.put((byte)((value >> 32) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}

			static void write(Bytes.Writable buf, long value) {
				buf.put((byte)((value >> 56) & 0xFF));
				buf.put((byte)((value >> 48) & 0xFF));
				buf.put((byte)((value >> 40) & 0xFF));
				buf.put((byte)((value >> 32) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)(value & 0xFF));
			}
			
			static void write(IO.WritableByteStream io, long value) throws IOException {
				io.write((byte)((value >> 56) & 0xFF));
				io.write((byte)((value >> 48) & 0xFF));
				io.write((byte)((value >> 40) & 0xFF));
				io.write((byte)((value >> 32) & 0xFF));
				io.write((byte)((value >> 24) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)(value & 0xFF));
			}

			static void write(OutputStream io, long value) throws IOException {
				io.write((int)((value >> 56) & 0xFF));
				io.write((int)((value >> 48) & 0xFF));
				io.write((int)((value >> 40) & 0xFF));
				io.write((int)((value >> 32) & 0xFF));
				io.write((int)((value >> 24) & 0xFF));
				io.write((int)((value >> 16) & 0xFF));
				io.write((int)((value >> 8) & 0xFF));
				io.write((int)(value & 0xFF));
			}

		}
		
		interface LE {
			
			static void write(byte[] buffer, int offset, long value) {
				buffer[offset++] = (byte)(value & 0xFF);
				buffer[offset++] = (byte)((value >> 8) & 0xFF);
				buffer[offset++] = (byte)((value >> 16) & 0xFF);
				buffer[offset++] = (byte)((value >> 24) & 0xFF);
				buffer[offset++] = (byte)((value >> 32) & 0xFF);
				buffer[offset++] = (byte)((value >> 40) & 0xFF);
				buffer[offset++] = (byte)((value >> 48) & 0xFF);
				buffer[offset] = (byte)((value >> 56) & 0xFF);
			}

			static void write(ByteBuffer buf, long value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 32) & 0xFF));
				buf.put((byte)((value >> 40) & 0xFF));
				buf.put((byte)((value >> 48) & 0xFF));
				buf.put((byte)((value >> 56) & 0xFF));
			}
			
			static void write(Bytes.Writable buf, long value) {
				buf.put((byte)(value & 0xFF));
				buf.put((byte)((value >> 8) & 0xFF));
				buf.put((byte)((value >> 16) & 0xFF));
				buf.put((byte)((value >> 24) & 0xFF));
				buf.put((byte)((value >> 32) & 0xFF));
				buf.put((byte)((value >> 40) & 0xFF));
				buf.put((byte)((value >> 48) & 0xFF));
				buf.put((byte)((value >> 56) & 0xFF));
			}

			static void write(IO.WritableByteStream io, long value) throws IOException {
				io.write((byte)(value & 0xFF));
				io.write((byte)((value >> 8) & 0xFF));
				io.write((byte)((value >> 16) & 0xFF));
				io.write((byte)((value >> 24) & 0xFF));
				io.write((byte)((value >> 32) & 0xFF));
				io.write((byte)((value >> 40) & 0xFF));
				io.write((byte)((value >> 48) & 0xFF));
				io.write((byte)((value >> 56) & 0xFF));
			}
			
			static void write(OutputStream io, long value) throws IOException {
				io.write((int)(value & 0xFF));
				io.write((int)((value >> 8) & 0xFF));
				io.write((int)((value >> 16) & 0xFF));
				io.write((int)((value >> 24) & 0xFF));
				io.write((int)((value >> 32) & 0xFF));
				io.write((int)((value >> 40) & 0xFF));
				io.write((int)((value >> 48) & 0xFF));
				io.write((int)((value >> 56) & 0xFF));
			}
			
		}
		
	}
	
}

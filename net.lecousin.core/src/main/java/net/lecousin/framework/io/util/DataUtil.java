package net.lecousin.framework.io.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.lecousin.framework.io.IO;

// skip checkstyle: OverloadMethodsDeclarationOrder
/** Utility method to encode or decode numbers in little or big endian. */
public final class DataUtil {
	
	private DataUtil() { /* no instance. */ }

	/* *** Read operations *** */
	
	/* - Using byte[] */
	
	/** big-endian or motorola format. */
	public static long readLongBigEndian(byte[] buffer, int offset) {
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
	
	/** little-endian or intel format. */
	public static long readLongLittleEndian(byte[] buffer, int offset) {
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
	
	/** big-endian or motorola format. */
	public static int readIntegerBigEndian(byte[] buffer, int offset) {
		int value;
		value = (buffer[offset] & 0xFF) << 24;
		value |= (buffer[offset + 1] & 0xFF) << 16;
		value |= (buffer[offset + 2] & 0xFF) << 8;
		value |= (buffer[offset + 3] & 0xFF);
		return value;
	}
	
	/** little-endian or intel format. */
	public static int readIntegerLittleEndian(byte[] buffer, int offset) {
		int value;
		value = (buffer[offset] & 0xFF);
		value |= (buffer[offset + 1] & 0xFF) << 8;
		value |= (buffer[offset + 2] & 0xFF) << 16;
		value |= (buffer[offset + 3] & 0xFF) << 24;
		return value;
	}
	
	/** little-endian or intel format. */
	public static long readUnsignedIntegerLittleEndian(byte[] buffer, int offset) {
		long value;
		value = (buffer[offset] & 0xFF);
		value |= (buffer[offset + 1] & 0xFF) << 8;
		value |= (buffer[offset + 2] & 0xFF) << 16;
		value |= ((long)(buffer[offset + 3] & 0xFF)) << 24;
		return value;
	}
	
	/** big-endian or motorola format. */
	public static long readUnsignedIntegerBigEndian(byte[] buffer, int offset) {
		long value;
		value = (buffer[offset + 3] & 0xFF);
		value |= (buffer[offset + 2] & 0xFF) << 8;
		value |= (buffer[offset + 1] & 0xFF) << 16;
		value |= ((long)(buffer[offset] & 0xFF)) << 24;
		return value;
	}
	
	/** little-endian or intel format. */
	public static int readUnsignedInteger24BitsLittleEndian(byte[] buffer, int offset) {
		int value;
		value = (buffer[offset] & 0xFF);
		value |= (buffer[offset + 1] & 0xFF) << 8;
		value |= (buffer[offset + 2] & 0xFF) << 16;
		return value;
	}
	
	/** big-endian or motorola format. */
	public static int readUnsignedInteger24BitsBigEndian(byte[] buffer, int offset) {
		int value;
		value = (buffer[offset + 2] & 0xFF);
		value |= (buffer[offset + 1] & 0xFF) << 8;
		value |= (buffer[offset] & 0xFF) << 16;
		return value;
	}
	
	/** little-endian or intel format. */
	public static short readShortLittleEndian(byte[] buffer, int offset) {
		return (short)((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
	}
	
	/** little-endian or intel format. */
	public static int readUnsignedShortLittleEndian(byte[] buffer, int offset) {
		return (buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8);
	}
	
	/** big-endian or motorola format. */
	public static short readShortBigEndian(byte[] buffer, int offset) {
		return (short)(((buffer[offset] & 0xFF) << 8) | (buffer[offset + 1] & 0xFF));
	}
	
	/** big-endian or motorola format. */
	public static int readUnsignedShortBigEndian(byte[] buffer, int offset) {
		return ((buffer[offset] & 0xFF) << 8) | (buffer[offset + 1] & 0xFF);
	}

	/* - Using ByteBuffer */
	
	/** big-endian or motorola format. */
	public static long readUnsignedIntegerBigEndian(ByteBuffer buf) {
		long value;
		value = ((long)(buf.get() & 0xFF)) << 24;
		value |= (buf.get() & 0xFF) << 16;
		value |= (buf.get() & 0xFF) << 8;
		value |= (buf.get() & 0xFF);
		return value;
	}
	
	/** big-endian or motorola format. */
	public static int readUnsignedInteger24BitsBigEndian(ByteBuffer buf) {
		int value;
		value = (buf.get() & 0xFF) << 16;
		value |= (buf.get() & 0xFF) << 8;
		value |= (buf.get() & 0xFF);
		return value;
	}

	/** big-endian or motorola format. */
	public static short readShortBigEndian(ByteBuffer buf) {
		short value;
		value = (short)((buf.get() & 0xFF) << 8);
		value |= (buf.get() & 0xFF);
		return value;
	}

	/** big-endian or motorola format. */
	public static int readUnsignedShortBigEndian(ByteBuffer buf) {
		int value;
		value = (buf.get() & 0xFF) << 8;
		value |= (buf.get() & 0xFF);
		return value;
	}
	
	/* - Using buffered IO */
	
	/** little-endian or intel format. */
	public static short readShortLittleEndian(IO.ReadableByteStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return (short)(value | (i << 8));
	}

	/** big-endian or motorola format. */
	public static short readShortBigEndian(IO.ReadableByteStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return (short)(i | (value << 8));
	}

	/** little-endian or intel format. */
	public static int readUnsignedShortLittleEndian(IO.ReadableByteStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return value | (i << 8);
	}

	/** big-endian or motorola format. */
	public static int readUnsignedShortBigEndian(IO.ReadableByteStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		value <<= 8;
		int i = io.read();
		if (i < 0) throw new EOFException();
		return value | i;
	}

	/** little-endian or intel format. */
	public static int readIntegerLittleEndian(IO.ReadableByteStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static int readIntegerBigEndian(IO.ReadableByteStream io) throws IOException {
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

	/** little-endian or intel format. */
	public static long readUnsignedIntegerLittleEndian(IO.ReadableByteStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static long readUnsignedIntegerBigEndian(IO.ReadableByteStream io) throws IOException {
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

	/** little-endian or intel format. */
	public static int readUnsignedInteger24BitsLittleEndian(IO.ReadableByteStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static int readUnsignedInteger24BitsBigEndian(IO.ReadableByteStream io) throws IOException {
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

	/** little-endian or intel format. */
	public static long readLongLittleEndian(IO.ReadableByteStream io) throws IOException {
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
	
	/** big-endian or motorola format. */
	public static long readLongBigEndian(IO.ReadableByteStream io) throws IOException {
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

	/* - Using InputStream */
	
	/** little-endian or intel format. */
	public static short readShortLittleEndian(InputStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return (short)(value | (i << 8));
	}
	
	/** big-endian or motorola format. */
	public static short readShortBigEndian(InputStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return (short)(i | (value << 8));
	}

	/** little-endian or intel format. */
	public static int readUnsignedShortLittleEndian(InputStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		int i = io.read();
		if (i < 0) throw new EOFException();
		return value | (i << 8);
	}

	/** big-endian or motorola format. */
	public static int readUnsignedShortBigEndian(InputStream io) throws IOException {
		int value = io.read();
		if (value < 0) throw new EOFException();
		value <<= 8;
		int i = io.read();
		if (i < 0) throw new EOFException();
		return value | i;
	}

	/** little-endian or intel format. */
	public static int readIntegerLittleEndian(InputStream io) throws IOException {
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
	
	/** big-endian or motorola format. */
	public static int readIntegerBigEndian(InputStream io) throws IOException {
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

	/** little-endian or intel format. */
	public static long readUnsignedIntegerLittleEndian(InputStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static long readUnsignedIntegerBigEndian(InputStream io) throws IOException {
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

	/** little-endian or intel format. */
	public static int readUnsignedInteger24BitsLittleEndian(InputStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static int readUnsignedInteger24BitsBigEndian(InputStream io) throws IOException {
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
	
	/** little-endian or intel format. */
	public static long readLongLittleEndian(InputStream io) throws IOException {
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

	/** big-endian or motorola format. */
	public static long readLongBigEndian(InputStream io) throws IOException {
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
	
	/* *** Write Operations *** */
	
	/* - Using byte[] */
	
	/** big-endian or motorola format. */
	public static void writeShortBigEndian(byte[] buffer, int offset, short value) {
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}
	
	/** little-endian or intel format. */
	public static void writeShortLittleEndian(byte[] buffer, int offset, short value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset] = (byte)((value >> 8) & 0xFF);
	}

	/** little-endian or intel format. */
	public static void writeUnsignedShortLittleEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset] = (byte)((value >> 8) & 0xFF);
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedShortBigEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}

	/** big-endian or motorola format. */
	public static void writeIntegerBigEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)((value >> 24) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}

	/** little-endian or intel format. */
	public static void writeIntegerLittleEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset] = (byte)((value >> 24) & 0xFF);
	}

	/** little-endian or intel format. */
	public static void writeUnsignedIntegerLittleEndian(byte[] buffer, int offset, long value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset] = (byte)((value >> 24) & 0xFF);
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedIntegerBigEndian(byte[] buffer, int offset, long value) {
		buffer[offset++] = (byte)((value >> 24) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}

	/** little-endian or intel format. */
	public static void writeUnsignedInteger24BitsLittleEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)((value >> 16) & 0xFF);
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedInteger24BitsBigEndian(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}

	/** big-endian or motorola format. */
	public static void writeLongBigEndian(byte[] buffer, int offset, long value) {
		buffer[offset++] = (byte)((value >> 56) & 0xFF);
		buffer[offset++] = (byte)((value >> 48) & 0xFF);
		buffer[offset++] = (byte)((value >> 40) & 0xFF);
		buffer[offset++] = (byte)((value >> 32) & 0xFF);
		buffer[offset++] = (byte)((value >> 24) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset] = (byte)(value & 0xFF);
	}

	/** little-endian or intel format. */
	public static void writeLongLittleEndian(byte[] buffer, int offset, long value) {
		buffer[offset++] = (byte)(value & 0xFF);
		buffer[offset++] = (byte)((value >> 8) & 0xFF);
		buffer[offset++] = (byte)((value >> 16) & 0xFF);
		buffer[offset++] = (byte)((value >> 24) & 0xFF);
		buffer[offset++] = (byte)((value >> 32) & 0xFF);
		buffer[offset++] = (byte)((value >> 40) & 0xFF);
		buffer[offset++] = (byte)((value >> 48) & 0xFF);
		buffer[offset] = (byte)((value >> 56) & 0xFF);
	}

	/* - Using buffered IO */
	
	/** big-endian or motorola format. */
	public static void writeShortBigEndian(IO.WritableByteStream io, short value) throws IOException {
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeShortLittleEndian(IO.WritableByteStream io, short value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedShortLittleEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedShortBigEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeIntegerBigEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)((value >> 24) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeIntegerLittleEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 24) & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedIntegerLittleEndian(IO.WritableByteStream io, long value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 24) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedIntegerBigEndian(IO.WritableByteStream io, long value) throws IOException {
		io.write((byte)((value >> 24) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedInteger24BitsLittleEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedInteger24BitsBigEndian(IO.WritableByteStream io, int value) throws IOException {
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeLongBigEndian(IO.WritableByteStream io, long value) throws IOException {
		io.write((byte)((value >> 56) & 0xFF));
		io.write((byte)((value >> 48) & 0xFF));
		io.write((byte)((value >> 40) & 0xFF));
		io.write((byte)((value >> 32) & 0xFF));
		io.write((byte)((value >> 24) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeLongLittleEndian(IO.WritableByteStream io, long value) throws IOException {
		io.write((byte)(value & 0xFF));
		io.write((byte)((value >> 8) & 0xFF));
		io.write((byte)((value >> 16) & 0xFF));
		io.write((byte)((value >> 24) & 0xFF));
		io.write((byte)((value >> 32) & 0xFF));
		io.write((byte)((value >> 40) & 0xFF));
		io.write((byte)((value >> 48) & 0xFF));
		io.write((byte)((value >> 56) & 0xFF));
	}

	/* - Using ByteBuffer */
	
	/** big-endian or motorola format. */
	public static void writeShortBigEndian(ByteBuffer io, short value) {
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeShortLittleEndian(ByteBuffer io, short value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedShortLittleEndian(ByteBuffer io, int value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedShortBigEndian(ByteBuffer io, int value) {
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeIntegerBigEndian(ByteBuffer io, int value) {
		io.put((byte)((value >> 24) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeIntegerLittleEndian(ByteBuffer io, int value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 24) & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedIntegerLittleEndian(ByteBuffer io, long value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 24) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedIntegerBigEndian(ByteBuffer io, long value) {
		io.put((byte)((value >> 24) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeUnsignedInteger24BitsLittleEndian(ByteBuffer io, int value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeUnsignedInteger24BitsBigEndian(ByteBuffer io, int value) {
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** big-endian or motorola format. */
	public static void writeLongBigEndian(ByteBuffer io, long value) {
		io.put((byte)((value >> 56) & 0xFF));
		io.put((byte)((value >> 48) & 0xFF));
		io.put((byte)((value >> 40) & 0xFF));
		io.put((byte)((value >> 32) & 0xFF));
		io.put((byte)((value >> 24) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)(value & 0xFF));
	}

	/** little-endian or intel format. */
	public static void writeLongLittleEndian(ByteBuffer io, long value) {
		io.put((byte)(value & 0xFF));
		io.put((byte)((value >> 8) & 0xFF));
		io.put((byte)((value >> 16) & 0xFF));
		io.put((byte)((value >> 24) & 0xFF));
		io.put((byte)((value >> 32) & 0xFF));
		io.put((byte)((value >> 40) & 0xFF));
		io.put((byte)((value >> 48) & 0xFF));
		io.put((byte)((value >> 56) & 0xFF));
	}
	
	
	/** little-endian or intel format. */
	public static byte[] getBytesLittleEndian(long value) {
		byte[] b = new byte[8];
		b[0] = (byte)(value & 0xFF);
		b[1] = (byte)((value >> 8) & 0xFF);
		b[2] = (byte)((value >> 16) & 0xFF);
		b[3] = (byte)((value >> 24) & 0xFF);
		b[4] = (byte)((value >> 32) & 0xFF);
		b[5] = (byte)((value >> 40) & 0xFF);
		b[6] = (byte)((value >> 48) & 0xFF);
		b[7] = (byte)((value >> 56) & 0xFF);
		return b;
	}
	
}

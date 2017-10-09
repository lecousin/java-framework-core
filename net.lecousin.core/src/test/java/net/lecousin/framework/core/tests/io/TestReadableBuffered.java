package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

public abstract class TestReadableBuffered extends TestIO.UsingGeneratedTestFiles {
	
	protected TestReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract IO.Readable.Buffered createReadableBufferedFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createReadableBufferedFromFile(openFile(), getFileSize());
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		IO.Readable.Buffered bio = (IO.Readable.Buffered)io;
		bio.getMaxBufferedSize();
		bio.getRemainingBufferedSize();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedBufferByBufferFully() throws Exception {
		IO.Readable.Buffered ioBuf = createReadableBufferedFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length];
		int count = 0;
		do {
			int nb = ioBuf.readFully(b);
			if (nb <= 0) break;
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read after "+count);
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes read after "+count);
			count++;
		} while (true);
		if (count != nbBuf)
			throw new Exception(""+count+" buffers read, expected is " + nbBuf);
		ioBuf.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedByteByByte() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize());
		for (int i = 0; i < nbBuf; ++i) {
			if (nbBuf > 1000 && (i % 100) == 99) {
				// make the test faster
				int skipBuf = 50;
				if (i + skipBuf > nbBuf) skipBuf = nbBuf - i;
				io.skip(skipBuf * testBuf.length);
				i += skipBuf - 1;
				continue;
			}
			for (int j = 0; j < testBuf.length; ++j) {
				if ((i + j) % 2 == 0) {
					int b = io.read();
					if (b != (testBuf[j] & 0xFF))
						throw new Exception("Byte " + b + " read instead of " + (testBuf[j] & 0xFF) + " at offset " + (testBuf.length*i+j));
				} else {
					byte b = io.readByte();
					if (b != testBuf[j])
						throw new Exception("Byte " + b + " read instead of " + testBuf[j] + " at offset " + (testBuf.length*i+j));
				}
			}
		}
		if (io.read() != -1)
			throw new Exception("Remaining byte(s) at the end of the file");
		io.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedBufferByBuffer() throws Exception {
		long size = getFileSize();
		IO.Readable.Buffered ioBuf = createReadableBufferedFromFile(openFile(), size);
		byte[] b = new byte[8192];
		long pos = 0;
		do {
			int nb = ioBuf.read(b, 0, 8192);
			if (nb <= 0) {
				if (pos != size)
					throw new Exception("Unexpected end of file at offset " + pos);
				break;
			}
			if (pos + nb > size)
				throw new Exception("" + nb + " byte(s) read at offset " + pos + ", but file size is " + size);
			
			int i = 0;
			while (i < nb) {
				int start = (int)((pos+i) % testBuf.length);
				int len = nb - i;
				if (len > testBuf.length - start) len = testBuf.length - start;
				for (int j = 0; j < len; ++j)
					if (b[i+j] != testBuf[start+j])
						throw new Exception("Invalid byte at offset " + (pos + i + start + j));
				i += len;
			}
			pos += nb;
		} while (true);
		ioBuf.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testReadableBufferedNextBufferAsync() throws Exception {
		IO.Readable.Buffered io = createReadableBufferedFromFile(openFile(), getFileSize());
		SynchronizationPoint<Exception> done = new SynchronizationPoint<>();
		MutableInteger pos = new MutableInteger(0);
		Mutable<AsyncWork<ByteBuffer,IOException>> read = new Mutable<>(null);
		MutableBoolean onDoneBefore = new MutableBoolean(false);
		RunnableWithParameter<Pair<ByteBuffer,IOException>> ondone = new RunnableWithParameter<Pair<ByteBuffer,IOException>>() {
			@Override
			public void run(Pair<ByteBuffer, IOException> param) {
				onDoneBefore.set(true);
			}
		};
		read.set(io.readNextBufferAsync(ondone));
		read.get().listenInline(new Runnable() {
			@Override
			public void run() {
				if (!onDoneBefore.get()) {
					done.error(new Exception("Method readNextBufferAsync didn't call ondone before listeners"));
					return;
				}
				onDoneBefore.set(false);
				AsyncWork<ByteBuffer,IOException> res = read.get();
				if (res.hasError()) {
					done.error(res.getError());
					return;
				}
				int p = pos.get();
				ByteBuffer buffer = res.getResult();
				if (p == testBuf.length * nbBuf) {
					if (buffer != null) {
						done.error(new Exception("" + buffer.remaining() + " byte(s) read after the end of the file"));
						return;
					}
					done.unblock();
					return;
				}
				if (buffer == null) {
					done.error(new Exception("Method readNextBufferAsync returned a null buffer, but this is not the end of the file: offset " + p));
					return;
				}
				int nb = buffer.remaining();
				int i = 0;
				while (i < nb) {
					int start = (p+i) % testBuf.length;
					int len = nb - i;
					if (len > testBuf.length - start) len = testBuf.length - start;
					for (int j = 0; j < len; ++j) {
						byte b = buffer.get();
						if (b != testBuf[start+j]) {
							done.error(new Exception("Invalid byte " + b + " at offset " + (p + i + start + j) + ", expected is " + testBuf[start+j]));
							return;
						}
					}
					i += len;
				}
				pos.set(p + nb);

				read.set(io.readNextBufferAsync(ondone));
				read.get().listenInline(this);
			}
		});
		done.block(0);
		if (done.hasError()) throw done.getError();
		io.close();
	}
	
	// TODO skip
	
}

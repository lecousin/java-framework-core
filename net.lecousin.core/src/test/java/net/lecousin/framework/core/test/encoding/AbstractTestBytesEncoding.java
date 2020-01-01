package net.lecousin.framework.core.test.encoding;

import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.BytesDecoder;
import net.lecousin.framework.encoding.BytesEncoder;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.io.util.Bytes;
import net.lecousin.framework.io.util.RawByteBuffer;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.util.DebugUtil;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class AbstractTestBytesEncoding extends LCCoreAbstractTest {

	/** Return a list of Pair for each test case, the first element of the pair is the data to encode, the second the expected result. */
	protected abstract List<Pair<byte[], byte[]>> getTestCases();

	/** Return a list of Pair for each test case, the first element of the pair is the data to encode, the second the expected result. */
	protected abstract List<Pair<byte[], byte[]>> getErrorTestCases();
	
	protected abstract BytesEncoder createEncoder();

	protected abstract BytesDecoder createDecoder();

	@Test
	public void testEncodeOneShotExactOutputLength() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				RawByteBuffer input = new RawByteBuffer(test.getValue1());
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue2().length, false));
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue2(), 0, test.getValue2().length));
				cache.free(output.array);
				
				output = new RawByteBuffer(cache.get(test.getValue2().length, false));
				encoder = createEncoder();
				encoder.encode(test.getValue1(), 0, test.getValue1().length, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue2(), 0, test.getValue2().length));
				cache.free(output.array);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}

	@Test
	public void testDecodeOneShotExactOutputLength() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				RawByteBuffer input = new RawByteBuffer(test.getValue2());
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue1().length, false));
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue1(), 0, test.getValue1().length));
				cache.free(output.array);
				
				output = new RawByteBuffer(cache.get(test.getValue1().length, false));
				decoder = createDecoder();
				decoder.decode(test.getValue2(), 0, test.getValue2().length, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue1(), 0, test.getValue1().length));
				cache.free(output.array);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncodeOneShotExactOutputLengthWithBiggerInputBuffer() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				byte[] bi = cache.get(test.getValue1().length + 512, true);
				System.arraycopy(test.getValue1(), 0, bi, 19, test.getValue1().length);
				RawByteBuffer input = new RawByteBuffer(bi, 19, test.getValue1().length);
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue2().length, false));
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue2(), 0, test.getValue2().length));
				cache.free(output.array);
				cache.free(bi);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecodeOneShotExactOutputLengthWithBiggerInputBuffer() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				byte[] bi = cache.get(test.getValue2().length + 512, true);
				System.arraycopy(test.getValue2(), 0, bi, 19, test.getValue2().length);
				RawByteBuffer input = new RawByteBuffer(bi, 19, test.getValue2().length);
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue1().length, false));
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue1(), 0, test.getValue1().length));
				cache.free(output.array);
				cache.free(bi);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncodeOneShotBiggerOutputLength() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				RawByteBuffer input = new RawByteBuffer(test.getValue1());
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue2().length + 512, true));
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue2(), 0, test.getValue2().length));
				cache.free(output.array);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecodeOneShotBiggerOutputLength() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				RawByteBuffer input = new RawByteBuffer(test.getValue2());
				RawByteBuffer output = new RawByteBuffer(cache.get(test.getValue1().length + 512, true));
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.currentOffset - output.arrayOffset);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue1(), 0, test.getValue1().length));
				cache.free(output.array);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncodeByteByByte() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				byte[] in = test.getValue1();
				byte[] out = cache.get(test.getValue2().length, false);
				RawByteBuffer input = new RawByteBuffer(in);
				RawByteBuffer output = new RawByteBuffer(out);
				input.length = 1;
				output.length = 1;
				do {
					int inPos = input.currentOffset;
					int outPos = output.currentOffset;
					encoder.encode(input, output, input.length == in.length);
					if (input.currentOffset == inPos) {
						// no input consumed
						if (output.currentOffset == outPos) {
							// no data written
							if (output.length < test.getValue2().length && output.remaining() < 8)
								output.length++;
							else {
								input.length++;
								output.length = output.currentOffset + 1;
							}
						}
					}
				} while (output.currentOffset < test.getValue2().length);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue2(), 0, test.getValue2().length));
				cache.free(out);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecodeByteByByte() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				byte[] in = test.getValue2();
				byte[] out = cache.get(test.getValue1().length, false);
				RawByteBuffer input = new RawByteBuffer(in);
				RawByteBuffer output = new RawByteBuffer(out);
				input.length = 1;
				output.length = 1;
				do {
					int inPos = input.currentOffset;
					int outPos = output.currentOffset;
					decoder.decode(input, output, input.length == in.length);
					if (input.currentOffset == inPos) {
						// no input consumed
						if (output.currentOffset == outPos) {
							// no data written
							if (output.length < test.getValue1().length && output.remaining() < 8)
								output.length++;
							else {
								input.length++;
								output.length = output.currentOffset + 1;
							}
						}
					}
				} while (output.currentOffset < test.getValue1().length);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.array, output.arrayOffset, test.getValue1(), 0, test.getValue1().length));
				cache.free(out);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncoderConsumerOneShot() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				final int tIndex = testIndex;
				final byte[] expected = test.getValue2();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					private int pos = 0;
					
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						while (data.hasRemaining()) {
							byte b = data.get();
							if (b != expected[pos])
								return new Async<>(new Exception("Invalid byte " + (pos + 1) + " for test " + tIndex + ": expected " + expected[pos] + " but was " + b));
							pos++;
						}
						if (onDataRelease != null)
							onDataRelease.accept(data);
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						if (pos != expected.length)
							return new Async<>(new Exception("End called after " + pos + " bytes received, " + expected.length + " expected"));
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> encoderConsumer = encoder.createEncoderConsumer(consumer, null);
				RawByteBuffer input = new RawByteBuffer(test.getValue1());
				encoderConsumer.consume(input, null).blockThrow(0);
				encoderConsumer.end().blockThrow(0);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data for test " + testIndex + ":\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecoderConsumerOneShot() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				final int tIndex = testIndex;
				final byte[] expected = test.getValue1();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					private int pos = 0;
					
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						if (onDataRelease != null)
							onDataRelease.accept(data);
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						if (pos != expected.length)
							return new Async<>(new Exception("End called after " + pos + " bytes received, " + expected.length + " expected"));
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> decoderConsumer = decoder.createDecoderConsumer(consumer, null);
				RawByteBuffer input = new RawByteBuffer(test.getValue2());
				decoderConsumer.consume(input, null).blockThrow(0);
				decoderConsumer.end().blockThrow(0);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncoderConsumerByteByByte() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder encoder = createEncoder();
				final int tIndex = testIndex;
				final byte[] expected = test.getValue2();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					private int pos = 0;
					
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						if (onDataRelease != null)
							onDataRelease.accept(data);
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						if (pos != expected.length)
							return new Async<>(new Exception("End called after " + pos + " bytes received, " + expected.length + " expected"));
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> encoderConsumer = encoder.createEncoderConsumer(consumer, null);
				for (int i = 0; i < test.getValue1().length; ++i) {
					byte[] buf = new byte[1];
					buf[0] = test.getValue1()[i];
					RawByteBuffer input = new RawByteBuffer(buf);
					encoderConsumer.consume(input, b -> {}).blockThrow(0);
				}
				encoderConsumer.end().blockThrow(0);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error encoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue1(), 0, test.getValue1().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecoderConsumerByteByByte() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				final int tIndex = testIndex;
				final byte[] expected = test.getValue1();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					private int pos = 0;
					
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						if (onDataRelease != null)
							onDataRelease.accept(data);
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						if (pos != expected.length)
							return new Async<>(new Exception("End called after " + pos + " bytes received, " + expected.length + " expected"));
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> decoderConsumer = decoder.createDecoderConsumer(consumer, null);
				for (int i = 0; i < test.getValue2().length; ++i) {
					byte[] buf = new byte[1];
					buf[0] = test.getValue2()[i];
					RawByteBuffer input = new RawByteBuffer(buf);
					decoderConsumer.consume(input, b -> {}).blockThrow(0);
				}
				decoderConsumer.end().blockThrow(0);
			} catch (Exception e) {
				StringBuilder s = new StringBuilder("Error decoding data:\r\n");
				DebugUtil.dumpHex(s, test.getValue2(), 0, test.getValue2().length);
				throw new AssertionError(s.toString(), e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncodeAndDecode() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				RawByteBuffer encoded = new RawByteBuffer(cache.get(test.getValue2().length, true));
				createEncoder().encode(new RawByteBuffer(test.getValue1()), encoded, true);
				RawByteBuffer decoded = new RawByteBuffer(cache.get(test.getValue1().length, true));
				encoded.flip();
				createDecoder().decode(encoded, decoded, true);
				Assert.assertEquals(test.getValue1().length, decoded.currentOffset);
				Assert.assertEquals(0, ArrayUtil.compare(decoded.array, 0, test.getValue1(), 0, test.getValue1().length));
			} catch (Exception e) {
				throw new AssertionError("Error testing encode and decode for test " + testIndex, e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncoderKnownOutputSize() {
		Assume.assumeTrue(createEncoder() instanceof BytesEncoder.KnownOutputSize);
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesEncoder.KnownOutputSize encoder = (BytesEncoder.KnownOutputSize)createEncoder();
				byte[] out = encoder.encode(test.getValue1());
				Assert.assertArrayEquals(test.getValue2(), out);
				
				encoder = (BytesEncoder.KnownOutputSize)createEncoder();
				out = encoder.encode(new RawByteBuffer(test.getValue1()));
				Assert.assertArrayEquals(test.getValue2(), out);
				
				encoder = (BytesEncoder.KnownOutputSize)createEncoder();
				out = encoder.encode(test.getValue1(), 0, test.getValue1().length);
				Assert.assertArrayEquals(test.getValue2(), out);
				
				encoder = (BytesEncoder.KnownOutputSize)createEncoder();
				byte[] in = new byte[test.getValue1().length + 512];
				System.arraycopy(test.getValue1(), 0, in, 19, test.getValue1().length);
				out = encoder.encode(in, 19, test.getValue1().length);
				Assert.assertArrayEquals(test.getValue2(), out);
			} catch (Exception e) {
				throw new AssertionError("Error for test " + testIndex, e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testDecoderKnownOutputSize() {
		Assume.assumeTrue(createDecoder() instanceof BytesDecoder.KnownOutputSize);
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder.KnownOutputSize decoder = (BytesDecoder.KnownOutputSize)createDecoder();
				byte[] out = decoder.decode(test.getValue2());
				Assert.assertArrayEquals(test.getValue1(), out);
				
				decoder = (BytesDecoder.KnownOutputSize)createDecoder();
				out = decoder.decode(new RawByteBuffer(test.getValue2()));
				Assert.assertArrayEquals(test.getValue1(), out);
				
				decoder = (BytesDecoder.KnownOutputSize)createDecoder();
				out = decoder.decode(test.getValue2(), 0, test.getValue2().length);
				Assert.assertArrayEquals(test.getValue1(), out);
				
				decoder = (BytesDecoder.KnownOutputSize)createDecoder();
				byte[] in = new byte[test.getValue2().length + 512];
				System.arraycopy(test.getValue2(), 0, in, 19, test.getValue2().length);
				out = decoder.decode(in, 19, test.getValue2().length);
				Assert.assertArrayEquals(test.getValue1(), out);
			} catch (Exception e) {
				throw new AssertionError("Error for test " + testIndex, e);
			}
			testIndex++;
		}
	}
	
	@Test
	public void testEncoderErrors() {
		byte[] buf = new byte[65536];
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getErrorTestCases()) {
			if (test.getValue1() == null) continue;
			
			BytesEncoder encoder = createEncoder();
			RawByteBuffer output = new RawByteBuffer(buf);
			try {
				encoder.encode(new RawByteBuffer(test.getValue1()), output, true);
				throw new AssertionError("Error expected for error test case " + testIndex);
			} catch (EncodingException e) {
				// ok
			} catch (Exception e) {
				throw new AssertionError("Unexpected error for error test case " + testIndex, e);
			}

			testIndex++;
		}
	}
	
	@Test
	public void testDecoderErrors() {
		byte[] buf = new byte[65536];
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getErrorTestCases()) {
			if (test.getValue2() == null) continue;
			
			BytesDecoder decoder = createDecoder();
			RawByteBuffer output = new RawByteBuffer(buf);
			try {
				decoder.decode(new RawByteBuffer(test.getValue2()), output, true);
				throw new AssertionError("Error expected for error test case " + testIndex);
			} catch (EncodingException e) {
				// ok
			} catch (Exception e) {
				throw new AssertionError("Unexpected error for error test case " + testIndex, e);
			}

			testIndex++;
		}
	}
	
	@Test
	public void testEncoderConsumerErrors() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getErrorTestCases()) {
			if (test.getValue1() == null) continue;
			Mutable<Exception> errorReceived = new Mutable<>(null);
			try {
				BytesEncoder encoder = createEncoder();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
						errorReceived.set(error);
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> encoderConsumer = encoder.createEncoderConsumer(consumer, null);
				RawByteBuffer input = new RawByteBuffer(test.getValue1());
				encoderConsumer.consume(input, null).blockThrow(0);
				encoderConsumer.end().blockThrow(0);
				throw new AssertionError("Error expected for error test case " + testIndex);
			} catch (EncodingException e) {
				// ok
				Assert.assertNotNull(errorReceived.get());
			} catch (Exception e) {
				throw new AssertionError("Unexpected error for error test case " + testIndex, e);
			}
			testIndex++;
		}

		Mutable<Exception> errorReceived = new Mutable<>(null);
		BytesEncoder encoder = createEncoder();
		AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
			@Override
			public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
				return new Async<>(true);
			}

			@Override
			public IAsync<Exception> end() {
				return new Async<>(true);
			}

			@Override
			public void error(Exception error) {
				errorReceived.set(error);
			}
		};
		AsyncConsumer<Bytes.Readable, Exception> encoderConsumer = encoder.createEncoderConsumer(consumer, null);
		encoderConsumer.error(new Exception());
		Assert.assertNotNull(errorReceived.get());
	}
	
	@Test
	public void testDecoderConsumerErrors() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getErrorTestCases()) {
			if (test.getValue2() == null) continue;
			Mutable<Exception> errorReceived = new Mutable<>(null);
			try {
				BytesDecoder decoder = createDecoder();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					@Override
					public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
						return new Async<>(true);
					}

					@Override
					public IAsync<Exception> end() {
						return new Async<>(true);
					}

					@Override
					public void error(Exception error) {
						errorReceived.set(error);
					}
				};
				AsyncConsumer<Bytes.Readable, Exception> decoderConsumer = decoder.createDecoderConsumer(consumer, null);
				RawByteBuffer input = new RawByteBuffer(test.getValue2());
				decoderConsumer.consume(input, null).blockThrow(0);
				decoderConsumer.end().blockThrow(0);
				throw new AssertionError("Error expected for error test case " + testIndex);
			} catch (EncodingException e) {
				// ok
				Assert.assertNotNull(errorReceived.get());
			} catch (Exception e) {
				throw new AssertionError("Unexpected error for error test case " + testIndex, e);
			}
			testIndex++;
		}

		Mutable<Exception> errorReceived = new Mutable<>(null);
		BytesDecoder decoder = createDecoder();
		AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
			@Override
			public IAsync<Exception> consume(Bytes.Readable data, Consumer<Bytes.Readable> onDataRelease) {
				return new Async<>(true);
			}

			@Override
			public IAsync<Exception> end() {
				return new Async<>(true);
			}

			@Override
			public void error(Exception error) {
				errorReceived.set(error);
			}
		};
		AsyncConsumer<Bytes.Readable, Exception> decoderConsumer = decoder.createDecoderConsumer(consumer, null);
		decoderConsumer.error(new Exception());
		Assert.assertNotNull(errorReceived.get());
	}
	
}

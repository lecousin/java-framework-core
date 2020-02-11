package net.lecousin.framework.core.test.encoding;

import java.util.List;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.BytesDecoder;
import net.lecousin.framework.encoding.BytesEncoder;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.io.data.Bytes;
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
				ByteArray input = new ByteArray(test.getValue1());
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue2().length, false), true);
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue2(), 0, test.getValue2().length));
				output.free();
				
				output = new ByteArray.Writable(cache.get(test.getValue2().length, false), true);
				encoder = createEncoder();
				encoder.encode(test.getValue1(), 0, test.getValue1().length, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue2(), 0, test.getValue2().length));
				output.free();
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
				ByteArray input = new ByteArray(test.getValue2());
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue1().length, false), true);
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue1(), 0, test.getValue1().length));
				output.free();
				
				output = new ByteArray.Writable(cache.get(test.getValue1().length, false), true);
				decoder = createDecoder();
				decoder.decode(test.getValue2(), 0, test.getValue2().length, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue1(), 0, test.getValue1().length));
				output.free();
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
				ByteArray input = new ByteArray(bi, 19, test.getValue1().length);
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue2().length, false), true);
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue2(), 0, test.getValue2().length));
				output.free();
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
				ByteArray input = new ByteArray(bi, 19, test.getValue2().length);
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue1().length, false), true);
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue1(), 0, test.getValue1().length));
				output.free();
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
				ByteArray input = new ByteArray(test.getValue1());
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue2().length + 512, false), true);
				encoder.encode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue2().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue2(), 0, test.getValue2().length));
				output.free();
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
				ByteArray input = new ByteArray(test.getValue2());
				ByteArray.Writable output = new ByteArray.Writable(cache.get(test.getValue1().length + 512, false), true);
				decoder.decode(input, output, true);
				Assert.assertEquals("Output size for test " + testIndex, test.getValue1().length, output.position());
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(output.getArray(), 0, test.getValue1(), 0, test.getValue1().length));
				output.free();
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
				int inPos = 0;
				int outPos = 0;
				int inLen = 1;
				int outLen = 1;
				do {
					ByteArray input = new ByteArray(in, inPos, Math.min(inLen, in.length - inPos));
					ByteArray.Writable output = new ByteArray.Writable(out, outPos, Math.min(outLen, out.length - outPos), false);
					encoder.encode(input, output, inPos >= in.length - inLen);
					if (input.position() == 0) {
						// no input consumed
						if (output.position() == 0) {
							// no data written
							if (outPos + outLen < test.getValue2().length && output.remaining() < 8)
								outLen++;
							else {
								outLen = 1;
								inLen++;
							}
						} else {
							outPos += output.position();
							outLen = 1;
						}
					} else {
						inPos += input.position();
						inLen = 1;
						outPos += output.position();
						outLen = 1;
					}
				} while (outPos < test.getValue2().length);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(out, 0, test.getValue2(), 0, test.getValue2().length));
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
				int inPos = 0;
				int outPos = 0;
				int inLen = 1;
				int outLen = 1;
				do {
					ByteArray input = new ByteArray(in, inPos, Math.min(inLen, in.length - inPos));
					ByteArray.Writable output = new ByteArray.Writable(out, outPos, Math.min(outLen, out.length - outPos), false);
					decoder.decode(input, output, inPos >= in.length - inLen);
					if (input.position() == 0) {
						// no input consumed
						if (output.position() == 0) {
							// no data written
							if (outPos + outLen < test.getValue2().length && output.remaining() < 8)
								outLen++;
							else {
								outLen = 1;
								inLen++;
							}
						} else {
							outPos += output.position();
							outLen = 1;
						}
					} else {
						inPos += input.position();
						inLen = 1;
						outPos += output.position();
						outLen = 1;
					}
				} while (outPos < test.getValue1().length);
				Assert.assertEquals("Output comparison for test " + testIndex, 0, ArrayUtil.compare(out, 0, test.getValue1(), 0, test.getValue1().length));
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						while (data.hasRemaining()) {
							byte b = data.get();
							if (b != expected[pos])
								return new Async<>(new Exception("Invalid byte " + (pos + 1) + " for test " + tIndex + ": expected " + expected[pos] + " but was " + b));
							pos++;
						}
						data.free();
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
				ByteArray input = new ByteArray(test.getValue1());
				encoderConsumer.consume(input).blockThrow(0);
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						data.free();
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
				ByteArray input = new ByteArray(test.getValue2());
				decoderConsumer.consume(input).blockThrow(0);
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						data.free();
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
					ByteArray input = new ByteArray(buf);
					encoderConsumer.consume(input).blockThrow(0);
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						data.free();
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
					ByteArray input = new ByteArray(buf);
					decoderConsumer.consume(input).blockThrow(0);
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
	public void testDecoderConsumerByBlocks() {
		int testIndex = 1;
		for (Pair<byte[], byte[]> test : getTestCases()) {
			try {
				BytesDecoder decoder = createDecoder();
				final int tIndex = testIndex;
				final byte[] expected = test.getValue1();
				AsyncConsumer<Bytes.Readable, Exception> consumer = new AsyncConsumer<Bytes.Readable, Exception>() {
					private int pos = 0;
					
					@Override
					public IAsync<Exception> consume(Bytes.Readable data) {
						while (data.hasRemaining()) {
							Assert.assertEquals("Byte " + (pos + 1) + " for test " + tIndex, expected[pos], data.get());
							pos++;
						}
						data.free();
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
				AsyncConsumer<Bytes.Readable, Exception> decoderConsumer = decoder.createDecoderConsumer(consumer, e -> e);
				int pos = 0;
				int len = 1;
				while (pos < test.getValue2().length) {
					int l = Math.min(len, test.getValue2().length - pos);
					byte[] buf = new byte[l];
					System.arraycopy(test.getValue2(), pos, buf, 0, l);
					ByteArray input = new ByteArray(buf);
					decoderConsumer.consume(input).blockThrow(0);
					pos += l;
					len++;
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
				ByteArray.Writable encoded = new ByteArray.Writable(cache.get(test.getValue2().length, true), true);
				createEncoder().encode(new ByteArray(test.getValue1()), encoded, true);
				ByteArray.Writable decoded = new ByteArray.Writable(cache.get(test.getValue1().length, true), true);
				encoded.flip();
				createDecoder().decode(encoded, decoded, true);
				Assert.assertEquals(test.getValue1().length, decoded.position());
				Assert.assertEquals(0, ArrayUtil.compare(decoded.getArray(), 0, test.getValue1(), 0, test.getValue1().length));
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
				out = encoder.encode(new ByteArray(test.getValue1()));
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
				out = decoder.decode(new ByteArray(test.getValue2()));
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
			ByteArray.Writable output = new ByteArray.Writable(buf, false);
			try {
				encoder.encode(new ByteArray(test.getValue1()), output, true);
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
			ByteArray.Writable output = new ByteArray.Writable(buf, false);
			try {
				decoder.decode(new ByteArray(test.getValue2()), output, true);
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						data.free();
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
				ByteArray input = new ByteArray(test.getValue1());
				encoderConsumer.consume(input).blockThrow(0);
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
			public IAsync<Exception> consume(Bytes.Readable data) {
				data.free();
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
					public IAsync<Exception> consume(Bytes.Readable data) {
						data.free();
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
				ByteArray input = new ByteArray(test.getValue2());
				decoderConsumer.consume(input).blockThrow(0);
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
			public IAsync<Exception> consume(Bytes.Readable data) {
				data.free();
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

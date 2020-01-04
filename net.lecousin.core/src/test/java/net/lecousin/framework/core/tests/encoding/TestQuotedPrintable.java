package net.lecousin.framework.core.tests.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.core.test.encoding.AbstractTestBytesEncoding;
import net.lecousin.framework.encoding.BytesDecoder;
import net.lecousin.framework.encoding.BytesEncoder;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.encoding.QuotedPrintable;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TestQuotedPrintable extends AbstractTestBytesEncoding {

	@Override
	protected List<Pair<byte[], byte[]>> getTestCases() {
		return Arrays.asList(
			new Pair<>(
				"Simple test".getBytes(StandardCharsets.UTF_8),
				"Simple test".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				"This is a test é This is a test é This is a test é This is a test é This is a test é This is a test".getBytes(StandardCharsets.UTF_8),
				"This is a test =C3=A9 This is a test =C3=A9 This is a test =C3=A9 This is a=\r\n test =C3=A9 This is a test =C3=A9 This is a test".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				"A test with spaces                                                                                     but not at the end".getBytes(StandardCharsets.UTF_8),
				"A test with spaces                                                         =\r\n                            but not at the end".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				"A test with spaces on several lines                                                                                                                             but not at the end".getBytes(StandardCharsets.UTF_8),
				"A test with spaces on several lines                                        =\r\n                                                                           =\r\n          but not at the end".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				"Test\twith\rspaces\non a line".getBytes(StandardCharsets.UTF_8),
				"Test\twith=0Dspaces=0Aon a line".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				" there is a space at the beginning".getBytes(StandardCharsets.UTF_8),
				" there is a space at the beginning".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				new byte[] { 0, 1, 2, 3, 4 },
				"=00=01=02=03=04".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 },
				"=00=01=02=03=04=05=06=07=08=08=0A=0B=0C=0D=0E=0F=10=11=12=13=14=15=16=17=18=\r\n=19=1A=1B=1C=1D=1E".getBytes(StandardCharsets.UTF_8)
			),
			new Pair<>(
				new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 32, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 },
				"=00=01=02=03=04=05=06=07=08=08 =0A=0B=0C=0D=0E=0F=10=11=12=13=14=15=16=17=\r\n=18=19=1A=1B=1C=1D=1E".getBytes(StandardCharsets.UTF_8)
			)
		);
	}
	
	@Override
	protected List<Pair<byte[], byte[]>> getErrorTestCases() {
		return Arrays.asList(
			new Pair<>(null, "abcde=0A=GFtoto".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, "abcde=0A=0Gtoto".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, new byte[] { 32, 32, 3, 32, 32 })
		);
	}
	
	@Override
	protected BytesEncoder createEncoder() {
		return new QuotedPrintable.Encoder();
	}
	
	@Override
	protected BytesDecoder createDecoder() {
		return new QuotedPrintable.Decoder();
	}
	
	@Test
	public void testDecodingWithTrailingSpaces() throws EncodingException {
		byte[] encoded = "This is a test =C3=A9 This is a test =C3=A9 This is a test =C3=A9 This is a=\r\n test =C3=A9 This is a test =C3=A9 This is a test \t \r".getBytes(StandardCharsets.UTF_8);
		byte[] expected = "This is a test é This is a test é This is a test é This is a test é This is a test é This is a test".getBytes(StandardCharsets.UTF_8);
		RawByteBuffer output = new RawByteBuffer(new byte[2048]);
		createDecoder().decode(new RawByteBuffer(encoded), output, true);
		Assert.assertEquals(expected.length, output.currentOffset);
		Assert.assertEquals(0, ArrayUtil.compare(expected, 0, output.array, 0, expected.length));
	}
	
	@Test
	public void testDecodeWithShortLines() throws EncodingException {
		byte[] encoded = "This is a test =\r\nwith =\r\nshort =\r\nlines".getBytes(StandardCharsets.UTF_8);
		byte[] expected = "This is a test with short lines".getBytes(StandardCharsets.UTF_8);
		RawByteBuffer output = new RawByteBuffer(new byte[2048]);
		createDecoder().decode(new RawByteBuffer(encoded), output, true);
		Assert.assertEquals(expected.length, output.currentOffset);
		Assert.assertEquals(0, ArrayUtil.compare(expected, 0, output.array, 0, expected.length));
	}
	
	@Test
	public void testDecodeWithAdditionalSpaces() throws EncodingException {
		byte[] encoded = "This is a test =\r\n    \r\nwith =\r\nshort =\r\n     \r\nlines".getBytes(StandardCharsets.UTF_8);
		byte[] expected = "This is a test with short lines".getBytes(StandardCharsets.UTF_8);
		RawByteBuffer output = new RawByteBuffer(new byte[2048]);
		createDecoder().decode(new RawByteBuffer(encoded), output, true);
		Assert.assertEquals(expected.length, output.currentOffset);
		Assert.assertEquals(0, ArrayUtil.compare(expected, 0, output.array, 0, expected.length));
	}
	
}

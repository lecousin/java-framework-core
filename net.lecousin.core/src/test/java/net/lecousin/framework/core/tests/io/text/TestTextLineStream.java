package net.lecousin.framework.core.tests.io.text;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.TextLineStream;

import org.junit.Assert;
import org.junit.Test;

public class TestTextLineStream extends LCCoreAbstractTest {

	private static final String line1 = "This is a text file";
	private static final String line2 = "With different lines";
	private static final String line3 = "\tand a tab";
	private static final String data = line1 + "\r\n" + line2 + "\n" + line3;
	
	@SuppressWarnings("resource")
	@Test
	public void test() throws Exception {
		BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(new ByteArrayIO(data.getBytes(StandardCharsets.US_ASCII), "test"), StandardCharsets.US_ASCII, 20, 2);
		TextLineStream lines = new TextLineStream(stream);
		Assert.assertEquals(line1, lines.nextLine().asString());
		Assert.assertEquals(line2, lines.nextLine().asString());
		Assert.assertEquals(line3, lines.nextLine().asString());
		Assert.assertEquals(null, lines.nextLine());
		Assert.assertEquals(Task.Priority.NORMAL, lines.getPriority());
		lines.close();
	}
	
}

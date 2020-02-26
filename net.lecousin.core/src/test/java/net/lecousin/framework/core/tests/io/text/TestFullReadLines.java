package net.lecousin.framework.core.tests.io.text;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.FullReadLines;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Test;

public class TestFullReadLines extends LCCoreAbstractTest {

	public static class MyReader extends FullReadLines<String> {

		public MyReader(ICharacterStream.Readable.Buffered stream) {
			super("test my reader", stream, Priority.NORMAL, IO.OperationType.SYNCHRONOUS);
		}

		@Override
		protected void processLine(CharArrayStringBuffer line) throws Exception {
			throw new Exception("Test error");
		}

		@Override
		protected String generateResult() throws Exception {
			throw new Exception("Test result error");
		}
		
	}
	
	@Test
	public void testIOError() {
		try (BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(new TestIOError.ReadableAlwaysError(), StandardCharsets.UTF_8, 1024, 2)) {
			new MyReader(cs).start().blockResult(0);
			throw new AssertionError();
		} catch (Exception e) {
			// ok
		}
	}
	
	@Test
	public void testProcessorError() {
		try (BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(new ByteArrayIO("Test\nError\nIn\nProcessor".getBytes(StandardCharsets.UTF_8), "test"), StandardCharsets.UTF_8, 1024, 2)) {
			new MyReader(cs).start().blockResult(0);
			throw new AssertionError();
		} catch (Exception e) {
			Assert.assertEquals("Test error", e.getMessage());
		}
		try (BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(new ByteArrayIO("SingleLine".getBytes(StandardCharsets.UTF_8), "test"), StandardCharsets.UTF_8, 1024, 2)) {
			new MyReader(cs).start().blockResult(0);
			throw new AssertionError();
		} catch (Exception e) {
			Assert.assertEquals("Test error", e.getMessage());
		}
		try (BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(new ByteArrayIO("".getBytes(StandardCharsets.UTF_8), "test"), StandardCharsets.UTF_8, 1024, 2)) {
			new MyReader(cs).start().blockResult(0);
			throw new AssertionError();
		} catch (Exception e) {
			Assert.assertEquals("Test result error", e.getMessage());
		}
	}
	
}

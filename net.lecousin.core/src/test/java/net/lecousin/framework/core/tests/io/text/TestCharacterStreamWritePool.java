package net.lecousin.framework.core.tests.io.text;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.CharacterStreamWritePool;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.io.text.WritableCharacterStream;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestCharacterStreamWritePool extends LCCoreAbstractTest {

	@Parameters(name = "buffered = {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(
			new Object[] { Boolean.FALSE },
			new Object[] { Boolean.TRUE }
		);
	}
	
	public TestCharacterStreamWritePool(boolean buffered) {
		this.buffered = buffered;
	}
	
	private boolean buffered;
	private File file;
	private ICharacterStream.Writable stream;
	private CharacterStreamWritePool pool;
	
	@Before
	public void initPool() throws Exception {
		file = TemporaryFiles.get().createFileSync("test", "charpool");
		FileIO.WriteOnly fio = new FileIO.WriteOnly(file, Priority.NORMAL);
		if (buffered)
			stream = new BufferedWritableCharacterStream(fio, StandardCharsets.UTF_8, 16);
		else
			stream = new WritableCharacterStream(fio, StandardCharsets.UTF_8);
		pool = new CharacterStreamWritePool(stream);
	}
	
	@After
	public void closeResources() {
		try {
			stream.close();
		} catch (Exception e) {
			// ignore
		}
		file.delete();
	}
	
	private void checkFile(String text, int nb) throws Exception {
		try (FileIO.ReadOnly fio = new FileIO.ReadOnly(file, Priority.NORMAL); BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(fio, StandardCharsets.UTF_8, 256, 3)) {
			char[] chars = new char[text.length()];
			for (int i = 0; i < nb; ++i) {
				Assert.assertEquals("Read " + i, chars.length, cs.readFullySync(chars, 0, chars.length));
				Assert.assertArrayEquals(text.toCharArray(), chars);
			}
			Assert.assertEquals(0, cs.readFullySync(chars, 0, chars.length));
		}
	}
	
	@Test
	public void testCharByChar() throws Exception {
		char[] chars = "Hello World!".toCharArray();
		
		for (int i = 0; i < 10; ++i)
			for (int j = 0; j < chars.length; ++j)
				pool.write(chars[j]);
		pool.flush().blockThrow(0);
		stream.close();
		checkFile("Hello World!", 10);
	}
	
	@Test
	public void testChars() throws Exception {
		String text = "Hello World !";
		pool.write(text.toCharArray());
		pool.write(new char[0]);
		pool.write(new CharArrayStringBuffer().append(text).asCharBuffers());
		pool.write(text);
		pool.write(new CharArrayStringBuffer().append(text));
		pool.write((CharSequence)text);
		pool.flush().blockThrow(0);
		stream.close();
		checkFile(text, 5);
	}
	
}

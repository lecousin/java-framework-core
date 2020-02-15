package net.lecousin.framework.core.tests.application.libraries.classpath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.libraries.classpath.LoadLibraryExtensionPointsFile;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;

import org.junit.Test;

public class TestLoadLibraryExtensionPointsFile extends LCCoreAbstractTest {

	@Test
	public void testEmpty() throws Exception {
		LoadLibraryExtensionPointsFile loader = createLoader("");
		loader.start().blockThrow(0);
	}

	@Test
	public void testEmptyLine() throws Exception {
		LoadLibraryExtensionPointsFile loader = createLoader("\r\n\r\n");
		loader.start().blockThrow(0);
	}

	@Test
	public void testUnknownClass() throws Exception {
		LoadLibraryExtensionPointsFile loader = createLoader("\r\ntoto\r\n");
		try {
			loader.start().blockThrow(0);
			throw new AssertionError("Must throw an error");
		} catch (IOException e) {
			// ok
		}
	}

	@Test
	public void testInvalidClass() throws Exception {
		LoadLibraryExtensionPointsFile loader = createLoader("\r\n" + getClass().getName() + "\r\n");
		try {
			loader.start().blockThrow(0);
			throw new AssertionError("Must throw an error");
		} catch (IOException e) {
			// ok
		}
	}
	
	private static LoadLibraryExtensionPointsFile createLoader(String content) {
		ByteArrayIO input = new ByteArrayIO(content.getBytes(StandardCharsets.UTF_8), "test");
		BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(input, StandardCharsets.UTF_8, 1024, 2);
		return new LoadLibraryExtensionPointsFile(stream, (ClassLoader & ApplicationClassLoader)LCCore.getApplication().getClassLoader());
	}
	
}

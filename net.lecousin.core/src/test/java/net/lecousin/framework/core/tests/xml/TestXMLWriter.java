package net.lecousin.framework.core.tests.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.xml.XMLWriter;

import org.junit.Test;

public class TestXMLWriter extends LCCoreAbstractTest {

	@Test
	public void testWriteAfterClose() throws Exception {
		MemoryIO io = new MemoryIO(4096, "test");
		XMLWriter writer = new XMLWriter(io, StandardCharsets.UTF_8, false, false);
		writer.start(null, "root", null);
		writer.end().blockThrow(0);
		try {
			writer.addAttribute("a", "b").blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		try {
			writer.endOfAttributes().blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		try {
			writer.openElement(null, "elem", null).blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		try {
			writer.closeElement().blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		try {
			writer.addText("text").blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		try {
			writer.addCData("cdata").blockThrow(0);
			throw new AssertionError("Error expected");
		} catch (IOException e) {
			// ok
		}
		io.close();
	}
	
}

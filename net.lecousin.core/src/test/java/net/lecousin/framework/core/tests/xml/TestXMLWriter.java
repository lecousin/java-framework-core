package net.lecousin.framework.core.tests.xml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.xml.XMLWriter;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

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
	
	@Test
	public void testBasic() throws Exception {
		MemoryIO io = new MemoryIO(4096, "test");
		XMLWriter writer = new XMLWriter(io, null, true, true);
		Map<String, String> namespaces = new LinkedHashMap<>();
		namespaces.put("http://test1", "");
		namespaces.put("http://test2", "t2");
		namespaces.put("http://test3", "t3");
		writer.start("http://test2", "root", namespaces);
		Map<String, String> namespaces2 = new LinkedHashMap<>();
		namespaces2.put("http://test4", "t4");
		writer.openElement(null, "element1", namespaces2);
		writer.addAttribute("a1", "v1");
		writer.openElement("http://test2", "sub1", null);
		writer.addText("my text");
		writer.closeElement();
		writer.openElement("http://test3", "sub2", null);
		writer.addComment(" my comment ");
		writer.closeElement();
		writer.openElement("http://test4", "sub3", null);
		writer.addCData("my cdata");
		writer.closeElement();
		writer.closeElement();
		writer.end().blockThrow(0);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		String xml = IOUtil.readFullyAsString(io, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL).blockResult(0).asString();
		io.close();
		Assert.assertEquals("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
			"<t2:root xmlns=\"http://test1\" xmlns:t2=\"http://test2\" xmlns:t3=\"http://test3\">\n" +
			"\t<element1 xmlns:t4=\"http://test4\" a1=\"v1\">\n" +
			"\t\t<t2:sub1>\n" +
			"\t\t\tmy text\n" +
			"\t\t</t2:sub1>\n" +
			"\t\t<t3:sub2>\n" +
			"\t\t\t<!-- my comment -->\n" +
			"\t\t</t3:sub2>\n" +
			"\t\t<t4:sub3>\n" +
			"\t\t\t<![CDATA[my cdata]]>\n" +
			"\t\t</t4:sub3>\n" +
			"\t</element1>\n" +
			"</t2:root>\n",
			xml);
	}
	
	@Test
	public void testFromDOM() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		String xmlSource = "<root><element a1=\"v1\"><sub1><!-- comment --></sub1><sub2>text</sub2></element></root>";
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlSource)));
		
		// with normal buffer
		MemoryIO io = new MemoryIO(4096, "test");
		XMLWriter writer = new XMLWriter(io, null, false, false);
		Element root = doc.getDocumentElement();
		Map<String, String> namespaces = new HashMap<>();
		writer.start(null, root.getLocalName(), namespaces);
		writer.write((Element)root.getChildNodes().item(0)).blockThrow(0);
		writer.end().blockThrow(0);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		String xml = IOUtil.readFullyAsString(io, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL).blockResult(0).asString();
		io.close();
		Assert.assertEquals(xmlSource, xml);
		
		// with small buffer
		io = new MemoryIO(2, "test");
		writer = new XMLWriter(new BufferedWritableCharacterStream(io, StandardCharsets.UTF_8, 2), false, false);
		root = doc.getDocumentElement();
		namespaces = new HashMap<>();
		writer.start(null, root.getLocalName(), namespaces);
		writer.write((Element)root.getChildNodes().item(0)).blockThrow(0);
		writer.end().blockThrow(0);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		xml = IOUtil.readFullyAsString(io, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL).blockResult(0).asString();
		io.close();
		Assert.assertEquals(xmlSource, xml);
	}
	
}

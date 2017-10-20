package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.xml.XMLStreamCursor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestXMLStreamCursorWithXMLStreamReader extends LCCoreAbstractTest {

	private static final String[] files = {
		"xml-test-suite/xmltest/valid/sa/001.xml",
		"xml-test-suite/xmltest/valid/sa/002.xml",
		"xml-test-suite/xmltest/valid/sa/003.xml",
		"xml-test-suite/xmltest/valid/sa/004.xml",
		"xml-test-suite/xmltest/valid/sa/005.xml",
		"xml-test-suite/xmltest/valid/sa/006.xml",
		"xml-test-suite/xmltest/valid/sa/007.xml",
		"xml-test-suite/xmltest/valid/sa/008.xml",
		"xml-test-suite/xmltest/valid/sa/009.xml",
		"xml-test-suite/xmltest/valid/sa/010.xml",
		"xml-test-suite/xmltest/valid/sa/011.xml",
		"xml-test-suite/xmltest/valid/sa/012.xml",
		"xml-test-suite/xmltest/valid/sa/013.xml",
		"xml-test-suite/xmltest/valid/sa/014.xml",
		"xml-test-suite/xmltest/valid/sa/015.xml",
		"xml-test-suite/xmltest/valid/sa/016.xml",
		"xml-test-suite/xmltest/valid/sa/017.xml",
		"xml-test-suite/xmltest/valid/sa/018.xml",
		"xml-test-suite/xmltest/valid/sa/019.xml",
		"xml-test-suite/xmltest/valid/sa/020.xml",
		"xml-test-suite/xmltest/valid/sa/021.xml",
		"xml-test-suite/xmltest/valid/sa/022.xml",
		"xml-test-suite/xmltest/valid/sa/023.xml",
		"xml-test-suite/xmltest/valid/sa/024.xml",
		"xml-test-suite/xmltest/valid/sa/025.xml",
		"xml-test-suite/xmltest/valid/sa/026.xml",
		"xml-test-suite/xmltest/valid/sa/027.xml",
		"xml-test-suite/xmltest/valid/sa/028.xml",
		"xml-test-suite/xmltest/valid/sa/029.xml",
		"xml-test-suite/xmltest/valid/sa/030.xml",
		"xml-test-suite/xmltest/valid/sa/031.xml",
		"xml-test-suite/xmltest/valid/sa/032.xml",
		"xml-test-suite/xmltest/valid/sa/033.xml",
		"xml-test-suite/xmltest/valid/sa/034.xml",
		"xml-test-suite/xmltest/valid/sa/035.xml",
		"xml-test-suite/xmltest/valid/sa/036.xml",
		"xml-test-suite/xmltest/valid/sa/037.xml",
		"xml-test-suite/xmltest/valid/sa/038.xml",
		"xml-test-suite/xmltest/valid/sa/039.xml",
		"xml-test-suite/xmltest/valid/sa/040.xml",
		"xml-test-suite/xmltest/valid/sa/041.xml",
		"xml-test-suite/xmltest/valid/sa/042.xml",
		"xml-test-suite/xmltest/valid/sa/043.xml",
		"xml-test-suite/xmltest/valid/sa/044.xml",
		"xml-test-suite/xmltest/valid/sa/045.xml",
		"xml-test-suite/xmltest/valid/sa/046.xml",
		"xml-test-suite/xmltest/valid/sa/047.xml",
		"xml-test-suite/xmltest/valid/sa/048.xml",
		"xml-test-suite/xmltest/valid/sa/049.xml",
		"xml-test-suite/xmltest/valid/sa/050.xml",
		"xml-test-suite/xmltest/valid/sa/051.xml",
		"xml-test-suite/xmltest/valid/sa/052.xml",
		"xml-test-suite/xmltest/valid/sa/053.xml",
		"xml-test-suite/xmltest/valid/sa/054.xml",
		"xml-test-suite/xmltest/valid/sa/055.xml",
		"xml-test-suite/xmltest/valid/sa/056.xml",
		"xml-test-suite/xmltest/valid/sa/057.xml",
		"xml-test-suite/xmltest/valid/sa/058.xml",
		"xml-test-suite/xmltest/valid/sa/059.xml",
		"xml-test-suite/xmltest/valid/sa/060.xml",
		"xml-test-suite/xmltest/valid/sa/061.xml",
		"xml-test-suite/xmltest/valid/sa/062.xml",
		"xml-test-suite/xmltest/valid/sa/063.xml",
		"xml-test-suite/xmltest/valid/sa/064.xml",
		"xml-test-suite/xmltest/valid/sa/065.xml",
		// XMLStreamReader fails: "xml-test-suite/xmltest/valid/sa/066.xml",
		"xml-test-suite/xmltest/valid/sa/067.xml",
		"xml-test-suite/xmltest/valid/sa/068.xml",
		"xml-test-suite/xmltest/valid/sa/069.xml",
		"xml-test-suite/xmltest/valid/sa/070.xml",
		"xml-test-suite/xmltest/valid/sa/071.xml",
		"xml-test-suite/xmltest/valid/sa/072.xml",
		"xml-test-suite/xmltest/valid/sa/073.xml",
		"xml-test-suite/xmltest/valid/sa/074.xml",
		"xml-test-suite/xmltest/valid/sa/075.xml",
		"xml-test-suite/xmltest/valid/sa/076.xml",
		"xml-test-suite/xmltest/valid/sa/077.xml",
		"xml-test-suite/xmltest/valid/sa/078.xml",
		"xml-test-suite/xmltest/valid/sa/079.xml",
		"xml-test-suite/xmltest/valid/sa/080.xml",
		"xml-test-suite/xmltest/valid/sa/081.xml",
		"xml-test-suite/xmltest/valid/sa/082.xml",
		"xml-test-suite/xmltest/valid/sa/083.xml",
		"xml-test-suite/xmltest/valid/sa/084.xml",
		"xml-test-suite/xmltest/valid/sa/085.xml",
		"xml-test-suite/xmltest/valid/sa/086.xml",
		"xml-test-suite/xmltest/valid/sa/087.xml",
		"xml-test-suite/xmltest/valid/sa/088.xml",
		"xml-test-suite/xmltest/valid/sa/089.xml",
		"xml-test-suite/xmltest/valid/sa/090.xml",
		"xml-test-suite/xmltest/valid/sa/091.xml",
		"xml-test-suite/xmltest/valid/sa/092.xml",
		"xml-test-suite/xmltest/valid/sa/093.xml",
		"xml-test-suite/xmltest/valid/sa/094.xml",
		"xml-test-suite/xmltest/valid/sa/095.xml",
		"xml-test-suite/xmltest/valid/sa/096.xml",
		"xml-test-suite/xmltest/valid/sa/097.xml",
		"xml-test-suite/xmltest/valid/sa/098.xml",
		"xml-test-suite/xmltest/valid/sa/099.xml",
		"xml-test-suite/xmltest/valid/sa/100.xml",
		"xml-test-suite/xmltest/valid/sa/101.xml",
		"xml-test-suite/xmltest/valid/sa/102.xml",
		"xml-test-suite/xmltest/valid/sa/103.xml",
		"xml-test-suite/xmltest/valid/sa/104.xml",
		"xml-test-suite/xmltest/valid/sa/105.xml",
		"xml-test-suite/xmltest/valid/sa/106.xml",
		"xml-test-suite/xmltest/valid/sa/107.xml",
		// TODO entity ref "xml-test-suite/xmltest/valid/sa/108.xml",
		"xml-test-suite/xmltest/valid/sa/109.xml",
		// TODO entity ref "xml-test-suite/xmltest/valid/sa/110.xml",
		"xml-test-suite/xmltest/valid/sa/111.xml",
		"xml-test-suite/xmltest/valid/sa/112.xml",
		"xml-test-suite/xmltest/valid/sa/113.xml",
		// XMLStreamReader fails: "xml-test-suite/xmltest/valid/sa/114.xml",
		"xml-test-suite/xmltest/valid/sa/115.xml",
		//"xml-test-suite/xmltest/valid/sa/116.xml",
		// XMLStreamReader fails: "xml-test-suite/xmltest/valid/sa/117.xml",
		// XMLStreamReader fails: "xml-test-suite/xmltest/valid/sa/118.xml",
		"xml-test-suite/xmltest/valid/sa/119.xml",
		"xml-test-suite/japanese/pr-xml-euc-jp.xml",
		"xml-test-suite/japanese/pr-xml-iso-2022-jp.xml",
		"xml-test-suite/japanese/pr-xml-little-endian.xml",
		"xml-test-suite/japanese/pr-xml-shift_jis.xml",
		"xml-test-suite/japanese/pr-xml-utf-16.xml",
		"xml-test-suite/japanese/pr-xml-utf-8.xml",
	};
	
	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>(files.length);
		for (int i = 0; i < files.length; ++i)
			list.add(new Object[] { files[i] });
		return list;
	}
	
	public TestXMLStreamCursorWithXMLStreamReader(String filepath) {
		this.filepath = filepath;
	}
	
	private String filepath;
	
	@SuppressWarnings("resource")
	@Test
	public void testParse() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		XMLStreamReader reader = factory.createXMLStreamReader(in);
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		XMLStreamCursor xml = new XMLStreamCursor(new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL));
		reader.next();
		xml.start();
		check(reader, xml, new LinkedList<>());
		in2.close();
		in.close();
	}

	private static void check(XMLStreamReader reader, XMLStreamCursor xml, LinkedList<String> openElements) throws Exception {
		/*
		System.out.println("XMLStreamReader:");
		do {
			System.out.print(reader.getEventType());
			System.out.print(",");
			if (!reader.hasNext()) break;
			reader.next();
		} while (true);
		System.out.println();
		System.out.println("XMLStreamCursor:");
		do {
			System.out.print(xml.type.name());
			System.out.print(",");
			xml.next();
		} while (true);*/
		do {
			boolean skipNextReader = false;
			switch (xml.type) {
			case DOCTYPE:
				Assert.assertEquals("DOCTYPE ", XMLStreamConstants.DTD, reader.getEventType());
				break;
			case TEXT:
				if (reader.getEventType() != XMLStreamConstants.CHARACTERS &&
					reader.getEventType() != XMLStreamConstants.ENTITY_REFERENCE) {
					if (xml.text.asString().trim().length() == 0) {
						skipNextReader = true;
						break;
					}
					throw new AssertionError("XMLStreamCursor has text \"" + xml.text.asString() + "\" but XMLStreamReader has event " + reader.getEventType());
				}
				String ourText = xml.text.asString();
				String theirText = "";
				do {
					int nextEvent = reader.getEventType();
					if (nextEvent == XMLStreamConstants.CHARACTERS) {
						theirText += reader.getText();
					} else if (nextEvent == XMLStreamConstants.ENTITY_REFERENCE) {
						theirText += "&" + reader.getLocalName() + ";";
					} else
						throw new AssertionError("XMLStreamCursor has text \"" + ourText + "\" but XMLStreamReader has text \"" + theirText + "\"");
					if (ourText.equals(theirText)) break;
					if (!ourText.startsWith(theirText))
						throw new AssertionError("XMLStreamCursor has text \"" + ourText + "\" but XMLStreamReader has text \"" + theirText + "\"");
					reader.next();
				} while (true);
				break;
			case START_ELEMENT:
				Assert.assertEquals("START_ELEMENT ", XMLStreamConstants.START_ELEMENT, reader.getEventType());
				assertEquals("START_ELEMENT: ", reader.getLocalName(), xml.text);
				if (!xml.isClosed) {
					openElements.add(xml.text.asString());
				} else {
					Assert.assertEquals("START_ELEMENT closed ", XMLStreamConstants.END_ELEMENT, reader.next());
					assertEquals("START_ELEMENT closed: ", reader.getLocalName(), xml.text);
				}
				// TODO check attributes
				break;
			case END_ELEMENT:
				Assert.assertEquals("END_ELEMENT ", XMLStreamConstants.END_ELEMENT, reader.getEventType());
				assertEquals("END_ELEMENT: ", reader.getLocalName(), xml.text);
				Assert.assertEquals("END_ELEMENT ", openElements.removeLast(), xml.text.asString());
				break;
			case COMMENT: {
				Assert.assertEquals("COMMENT ", XMLStreamConstants.COMMENT, reader.getEventType());
				String t = reader.getText();
				if (!xml.text.equals(t)) {
					String s = xml.text.asString();
					s = s.replace("\r\n", "\n");
					if (s.equals(t)) break;
				}
				assertEquals("COMMENT: ", t, xml.text);
				break;
			}
			case CDATA:
				if (reader.getEventType() == XMLStreamConstants.CDATA) {
					String t = reader.getText();
					if (!xml.text.equals(t)) {
						String s = xml.text.asString();
						s = s.replace("\r\n", "\n");
						if (s.equals(t)) break;
					}
					assertEquals("CDATA: ", t, xml.text);
					break;
				}
				if (reader.getEventType() == XMLStreamConstants.CHARACTERS) {
					String t = reader.getText();
					if (!xml.text.equals(t)) {
						String s = xml.text.asString();
						s = s.replace("\r\n", "\n");
						if (s.equals(t)) break;
					}
					assertEquals("CDATA: ", t, xml.text);
					break;
				}
				Assert.assertEquals("CDATA ", XMLStreamConstants.CDATA, reader.getEventType());
				break;
			case PROCESSING_INSTRUCTION:
				Assert.assertEquals("PROCESSING_INSTRUCTION ", XMLStreamConstants.PROCESSING_INSTRUCTION, reader.getEventType());
				assertEquals("PROCESSING_INSTRUCTION target: ", reader.getPITarget(), xml.text);
				// TODO check PIData
				break;
			}
			if (!skipNextReader)
				reader.next();
			try { xml.next(); }
			catch (EOFException e) {
				Assert.assertEquals("End of file ", XMLStreamConstants.END_DOCUMENT, reader.getEventType());
				return;
			}
		} while (true);
	}
	
	/*
	private static void checkNodeContent(XMLStreamReader reader, XMLStreamCursor xml, LinkedList<String> openElements) throws Exception {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node n = list.item(i);
			checkNode(n, xml, openElements);
		}
	}
	
	private static void checkNode(Node node, XMLStreamCursor xml, LinkedList<String> openElements) throws Exception {
		switch (xml.type) {
		case COMMENT:
			Assert.assertEquals(Node.COMMENT_NODE, node.getNodeType());
			checkComment((Comment)node, xml);
			break;
		case CDATA:
			Assert.assertEquals(Node.CDATA_SECTION_NODE, node.getNodeType());
			checkCData((CDATASection)node, xml);
			break;
		case DOCTYPE:
			Assert.assertEquals(Node.DOCUMENT_TYPE_NODE, node.getNodeType());
			checkDocType((DocumentType)node, xml);
			break;
		case PROCESSING_INSTRUCTION:
			Assert.assertEquals(Node.PROCESSING_INSTRUCTION_NODE, node.getNodeType());
			checkProcessingInstruction((ProcessingInstruction)node, xml);
			break;
		case START_ELEMENT:
			Assert.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
			checkElement((Element)node, xml, openElements);
			break;
		case END_ELEMENT:
			Assert.assertFalse(openElements.isEmpty());
			Assert.assertEquals(openElements.removeLast(), xml.text.asString());
			// TODO check no more elements in DOM
			break;
		case TEXT:
			if (xml.text.asString().trim().isEmpty() && node.getNodeType() != Node.TEXT_NODE) {
				xml.next();
				checkNode(node, xml, openElements);
				return;
			}
			if (node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
				node = node.getOwnerDocument().createTextNode("&" + ((EntityReference)node).getNodeName() + ";");
				// TODO we should have entity reference in XMLStreamCursor as well
			}
			Assert.assertEquals("Expected text <" + xml.text.asString() + ">, but node " + node.getNodeType() + " found", Node.TEXT_NODE, node.getNodeType());
			checkText((Text)node, xml);
			break;
		}
	}
	
	private static void checkComment(Comment node, XMLStreamCursor xml) throws Exception {
		assertEquals("Comment", node.getData(), xml.text);
		xml.next();
	}
	
	private static void checkCData(CDATASection node, XMLStreamCursor xml) throws Exception {
		assertEquals("CDATA content", node.getData(), xml.text);
		xml.next();
	}
	
	private static void checkText(Text node, XMLStreamCursor xml) throws Exception {
		assertEquals("text", node.getData(), xml.text);
		xml.next();
	}
	
	private static void checkDocType(DocumentType node, XMLStreamCursor xml) throws Exception {
		assertEquals("DOCTYPE name", node.getName(), xml.text);
		assertEquals("system id", node.getSystemId(), xml.system);
		assertEquals("public id", node.getPublicId(), xml.publicId);
		// TODO
		xml.next();
	}
	
	private static void checkProcessingInstruction(ProcessingInstruction node, XMLStreamCursor xml) throws Exception {
		assertEquals("processing instruction target", node.getTarget(), xml.text);
		// TODO check inside ? node.getData()
		xml.next();
	}
	
	private static void checkElement(Element node, XMLStreamCursor xml, LinkedList<String> openElements) throws Exception {
		assertEquals("element name", node.getTagName(), xml.text);
		NamedNodeMap attrs = node.getAttributes();
		for (int i = 0; i < attrs.getLength(); ++i) {
			Node attr = attrs.item(i);
			String name = attr.getNodeName();
			String value = attr.getNodeValue();
			assertEquals("attribute " + name + " on element " + node.getNodeName(), value, xml.removeAttribute(name));
		}
		Assert.assertTrue(xml.attributes.isEmpty());
		if (xml.isClosed) {
			Assert.assertTrue(node.getFirstChild() == null);
			xml.next();
			return;
		}
		openElements.add(xml.text.asString());
		xml.next();
		checkNodeContent(node, xml, openElements);
	}
*/	
	
	private static void assertEquals(String message, String expected, IString found) {
		if (found == null) {
			if (expected == null || expected.isEmpty()) return;
			throw new AssertionError(message + ": expected <" + expected + ">, found is null");
		}
		Assert.assertEquals(expected, found.asString());
	}
}

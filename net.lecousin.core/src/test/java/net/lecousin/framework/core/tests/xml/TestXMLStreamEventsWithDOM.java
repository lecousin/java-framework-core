package net.lecousin.framework.core.tests.xml;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.text.IString;
import net.lecousin.framework.xml.XMLStreamEvents;
import net.lecousin.framework.xml.XMLStreamEvents.Attribute;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

public abstract class TestXMLStreamEventsWithDOM<EVENTS extends XMLStreamEvents> extends LCCoreAbstractTest {

	private static final String[] files = {
		"xml-test-suite/mine/001.xml",
		"xml-test-suite/mine/002.xml",
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
		// name with a semi-colon is not allowed by latest version of XML "xml-test-suite/xmltest/valid/sa/012.xml",
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
		//"xml-test-suite/xmltest/valid/sa/044.xml",
		//"xml-test-suite/xmltest/valid/sa/045.xml",
		//"xml-test-suite/xmltest/valid/sa/046.xml",
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
		//"xml-test-suite/xmltest/valid/sa/058.xml",
		"xml-test-suite/xmltest/valid/sa/059.xml",
		"xml-test-suite/xmltest/valid/sa/060.xml",
		"xml-test-suite/xmltest/valid/sa/061.xml",
		"xml-test-suite/xmltest/valid/sa/062.xml",
		"xml-test-suite/xmltest/valid/sa/063.xml",
		"xml-test-suite/xmltest/valid/sa/064.xml",
		"xml-test-suite/xmltest/valid/sa/065.xml",
		//"xml-test-suite/xmltest/valid/sa/066.xml",
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
		//"xml-test-suite/xmltest/valid/sa/080.xml",
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
		//"xml-test-suite/xmltest/valid/sa/091.xml",
		"xml-test-suite/xmltest/valid/sa/092.xml",
		"xml-test-suite/xmltest/valid/sa/093.xml",
		//"xml-test-suite/xmltest/valid/sa/094.xml",
		"xml-test-suite/xmltest/valid/sa/095.xml",
		//"xml-test-suite/xmltest/valid/sa/096.xml",
		//"xml-test-suite/xmltest/valid/sa/097.xml",
		"xml-test-suite/xmltest/valid/sa/098.xml",
		"xml-test-suite/xmltest/valid/sa/099.xml",
		"xml-test-suite/xmltest/valid/sa/100.xml",
		"xml-test-suite/xmltest/valid/sa/101.xml",
		"xml-test-suite/xmltest/valid/sa/102.xml",
		"xml-test-suite/xmltest/valid/sa/103.xml",
		//"xml-test-suite/xmltest/valid/sa/104.xml",
		"xml-test-suite/xmltest/valid/sa/105.xml",
		"xml-test-suite/xmltest/valid/sa/106.xml",
		"xml-test-suite/xmltest/valid/sa/107.xml",
		// entity ref "xml-test-suite/xmltest/valid/sa/108.xml",
		"xml-test-suite/xmltest/valid/sa/109.xml",
		// entity ref "xml-test-suite/xmltest/valid/sa/110.xml",
		//"xml-test-suite/xmltest/valid/sa/111.xml",
		"xml-test-suite/xmltest/valid/sa/112.xml",
		"xml-test-suite/xmltest/valid/sa/113.xml",
		"xml-test-suite/xmltest/valid/sa/114.xml",
		"xml-test-suite/xmltest/valid/sa/115.xml",
		//"xml-test-suite/xmltest/valid/sa/116.xml",
		"xml-test-suite/xmltest/valid/sa/117.xml",
		"xml-test-suite/xmltest/valid/sa/118.xml",
		"xml-test-suite/xmltest/valid/sa/119.xml",
	};
	
	public static Collection<Object[]> getFiles() {
		ArrayList<Object[]> list = new ArrayList<>(files.length);
		for (int i = 0; i < files.length; ++i)
			list.add(new Object[] { files[i] });
		return list;
	}
	
	public TestXMLStreamEventsWithDOM(String filepath) {
		this.filepath = filepath;
	}
	
	protected String filepath;
	
	protected abstract EVENTS start(IO.Readable input) throws Exception;
	
	protected abstract void next(EVENTS xml) throws Exception;
	
	@SuppressWarnings("resource")
	@Test
	public void testParse() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(in));
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		IO.Readable io = new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL);
		EVENTS xml = start(io);
		checkNodeContent(doc, xml, new LinkedList<>());
		in.close();
		io.close();
	}
	
	private void checkNodeContent(Node node, EVENTS xml, LinkedList<String> openElements) throws Exception {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node n = list.item(i);
			checkNode(n, xml, openElements);
		}
	}
	
	private void checkNode(Node node, EVENTS xml, LinkedList<String> openElements) throws Exception {
		switch (xml.event.type) {
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
			Assert.assertEquals(openElements.removeLast(), xml.event.text.asString());
			next(xml);
			break;
		case TEXT:
			if (xml.event.text.asString().trim().isEmpty() && node.getNodeType() != Node.TEXT_NODE) {
				next(xml);
				checkNode(node, xml, openElements);
				return;
			}
			if (node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
				node = node.getOwnerDocument().createTextNode("&" + ((EntityReference)node).getNodeName() + ";");
			}
			Assert.assertEquals("Expected text <" + xml.event.text.asString() + ">, but node " + node.getNodeType() + " found", Node.TEXT_NODE, node.getNodeType());
			checkText((Text)node, xml);
			break;
		}
	}
	
	private void checkComment(Comment node, EVENTS xml) throws Exception {
		assertEquals("Comment", node.getData(), xml.event.text);
		next(xml);
	}
	
	private void checkCData(CDATASection node, EVENTS xml) throws Exception {
		assertEquals("CDATA content", node.getData(), xml.event.text);
		next(xml);
	}
	
	private void checkText(Text node, EVENTS xml) throws Exception {
		String message = "text";
		if (!xml.event.context.isEmpty())
			message += " in " + xml.event.context.getFirst().text.asString();
		assertEquals(message, node.getData(), xml.event.text);
		next(xml);
	}
	
	private void checkDocType(DocumentType node, EVENTS xml) throws Exception {
		assertEquals("DOCTYPE name", node.getName(), xml.event.text);
		assertEquals("system id", node.getSystemId(), xml.event.system);
		assertEquals("public id", node.getPublicId(), xml.event.publicId);
		next(xml);
	}
	
	private void checkProcessingInstruction(ProcessingInstruction node, EVENTS xml) throws Exception {
		assertEquals("processing instruction target", node.getTarget(), xml.event.text);
		next(xml);
	}
	
	private void checkElement(Element node, EVENTS xml, LinkedList<String> openElements) throws Exception {
		assertEquals("element name", node.getTagName(), xml.event.text);
		assertEquals("element local name", node.getLocalName(), xml.event.localName);
		assertEquals("element prefix", node.getPrefix(), xml.event.namespacePrefix);
		assertEquals("element namespace URI", node.getNamespaceURI(), xml.event.namespaceURI);
		NamedNodeMap attrs = node.getAttributes();
		for (int i = 0; i < attrs.getLength(); ++i) {
			Node attr = attrs.item(i);
			String aname = attr.getNodeName();
			if (aname.equals("xmlns") || aname.startsWith("xmlns:")) continue;
			String prefix = attr.getPrefix();
			if (prefix == null) prefix = "";
			String name = attr.getLocalName();
			String value = attr.getNodeValue();
			Attribute a = xml.getAttributeWithPrefix(prefix, name);
			if (a == null)
				throw new AssertionError("Missing attribute '" + name + "' and prefix '" + prefix + "' on element " + node.getNodeName());
			Assert.assertTrue(a == xml.getAttributeByFullName(prefix.length() > 0 ? prefix + ':' + name : name));
			Assert.assertTrue(a == xml.getAttributeWithNamespaceURI(xml.getNamespaceURI(prefix), name));
			xml.removeAttributeWithPrefix(prefix, name);
			assertEquals("value of attribute '" + name + "' on element " + node.getNodeName(), value, a.value);
		}
		Assert.assertTrue(xml.event.attributes.isEmpty());
		if (xml.event.isClosed) {
			Assert.assertTrue(node.getFirstChild() == null);
			next(xml);
			return;
		}
		openElements.add(xml.event.text.asString());
		next(xml);
		checkNodeContent(node, xml, openElements);
	}
	
	private static void assertEquals(String message, String expected, IString found) {
		if (found == null) {
			if (expected == null || expected.isEmpty()) return;
			throw new AssertionError(message + ": expected <" + expected + ">, found is null");
		}
		if (expected == null)
			if (found.length() == 0) return;
		Assert.assertEquals(message, expected.trim(), found.asString().trim());
	}
	
}

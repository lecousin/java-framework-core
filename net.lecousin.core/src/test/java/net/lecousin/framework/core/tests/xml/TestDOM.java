package net.lecousin.framework.core.tests.xml;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.xml.XMLStreamReader;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.dom.XMLDocument;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

@RunWith(Parameterized.class)
public class TestDOM extends LCCoreAbstractTest {

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
		//"xml-test-suite/xmltest/valid/sa/023.xml",
		//"xml-test-suite/xmltest/valid/sa/024.xml",
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
		//"xml-test-suite/xmltest/valid/sa/053.xml",
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
		//"xml-test-suite/xmltest/valid/sa/068.xml",
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
		//"xml-test-suite/xmltest/valid/sa/085.xml",
		//"xml-test-suite/xmltest/valid/sa/086.xml",
		//"xml-test-suite/xmltest/valid/sa/087.xml",
		//"xml-test-suite/xmltest/valid/sa/088.xml",
		//"xml-test-suite/xmltest/valid/sa/089.xml",
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
		//"xml-test-suite/xmltest/valid/sa/114.xml",
		//"xml-test-suite/xmltest/valid/sa/115.xml",
		//"xml-test-suite/xmltest/valid/sa/116.xml",
		//"xml-test-suite/xmltest/valid/sa/117.xml",
		//"xml-test-suite/xmltest/valid/sa/118.xml",
		"xml-test-suite/xmltest/valid/sa/119.xml",
	};
		
	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>(files.length);
		for (int i = 0; i < files.length; ++i)
			list.add(new Object[] { files[i] });
		return list;
	}
	
	public TestDOM(String filepath) {
		this.filepath = filepath;
	}
	
	private String filepath;
	
	@Test
	public void testReaderSync() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(in));
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		IO.Readable io = new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL);
		XMLStreamReader xml = new XMLStreamReader(io, 1024);
		xml.start();
		XMLDocument doc2 = XMLDocument.create(xml);
		checkDocument(doc, doc2);
		in2.close();
		in.close();
	}
	
	@Test
	public void testReaderAsync() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(in));
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		IO.Readable io = new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL);
		XMLStreamReaderAsync xml = new XMLStreamReaderAsync(io, 1024);
		xml.start().blockException(0);
		XMLDocument doc2 = XMLDocument.create(xml).blockResult(0);
		checkDocument(doc, doc2);
		in2.close();
		in.close();
	}
	
	private static void checkDocument(Document expected, Document found) {
		checkDocType(expected.getDoctype(), found.getDoctype());
		checkElement(expected.getDocumentElement(), found.getDocumentElement());
	}
	
	private static void checkDocType(DocumentType expected, DocumentType found) {
		if (expected == null) {
			if (found == null) return;
			throw new AssertionError("No DocumentType expected, one found");
		}
		if (found == null)
			throw new AssertionError("DocumentType expected, no found");
		Assert.assertEquals(expected.getPublicId(), found.getPublicId());
		Assert.assertEquals(expected.getSystemId(), found.getSystemId());
	}
	
	private static void checkElement(Element expected, Element found) {
		Assert.assertEquals(expected.getNodeName(), found.getNodeName());
		Assert.assertEquals(expected.getLocalName(), found.getLocalName());
		Assert.assertEquals(expected.getNamespaceURI(), found.getNamespaceURI());
		Assert.assertEquals(expected.getPrefix(), found.getPrefix());
		Assert.assertEquals(expected.getTagName(), found.getTagName());
		Assert.assertEquals(expected.getNodeValue(), found.getNodeValue());
		//Assert.assertEquals(expected.getTextContent(), found.getTextContent());
		Assert.assertTrue(expected.hasAttributes() == found.hasAttributes());
		checkMap(expected.getAttributes(), found.getAttributes());
		checkList(expected.getChildNodes(), found.getChildNodes());
	}
	
	private static void checkMap(NamedNodeMap expected, NamedNodeMap found) {
		Assert.assertTrue(expected.getLength() == found.getLength());
		for (int i = 0; i < expected.getLength(); ++i)
			checkNode(expected.item(i), found.item(i));
	}
	
	private static void checkList(NodeList expected, NodeList found) {
		List<Node> list = new ArrayList<>(expected.getLength());
		for (int i = 0; i < expected.getLength(); ++i) {
			Node node = expected.item(i);
			if (node instanceof ProcessingInstruction) continue;
			list.add(node);
		}
		Assert.assertTrue(list.size() == found.getLength());
		for (int i = 0; i < list.size(); ++i)
			checkNode(list.get(i), found.item(i));
	}
	
	private static void checkNode(Node expected, Node found) {
		if (expected instanceof Element) {
			Assert.assertTrue(found instanceof Element);
			checkElement((Element)expected, (Element)found);
			return;
		}
		if (expected instanceof CDATASection) {
			Assert.assertTrue(found instanceof CDATASection);
			checkCData((CDATASection)expected, (CDATASection)found);
			return;
		}
		if (expected instanceof Comment) {
			Assert.assertTrue(found instanceof Comment);
			checkComment((Comment)expected, (Comment)found);
			return;
		}
		if (expected instanceof Text) {
			Assert.assertTrue(found instanceof Text);
			checkText((Text)expected, (Text)found);
			return;
		}
		if (expected instanceof Attr) {
			Assert.assertTrue(found instanceof Attr);
			checkAttr((Attr)expected, (Attr)found);
			return;
		}
		throw new AssertionError("Unexpected node: " + expected.getClass().getName());
	}
	
	private static void checkCData(CDATASection expected, CDATASection found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	private static void checkComment(Comment expected, Comment found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	private static void checkText(Text expected, Text found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	private static void checkAttr(Attr expected, Attr found) {
		Assert.assertEquals(expected.getNodeName(), found.getNodeName());
		Assert.assertEquals(expected.getNodeValue(), found.getNodeValue());
		Assert.assertEquals(expected.getLocalName(), found.getLocalName());
		Assert.assertEquals(expected.getNamespaceURI(), found.getNamespaceURI());
		Assert.assertEquals(expected.getPrefix(), found.getPrefix());
		Assert.assertEquals(expected.getName(), found.getName());
		Assert.assertEquals(expected.getValue(), found.getValue());
		Assert.assertEquals(expected.getTextContent(), found.getTextContent());
	}

}

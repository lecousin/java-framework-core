package net.lecousin.framework.core.tests.xml;

import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
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

public abstract class TestDOM extends LCCoreAbstractTest {

	protected static void checkDocument(Document expected, Document found) {
		checkDocType(expected.getDoctype(), found.getDoctype());
		checkElement(expected.getDocumentElement(), found.getDocumentElement());
	}
	
	protected static void checkDocType(DocumentType expected, DocumentType found) {
		if (expected == null) {
			if (found == null) return;
			throw new AssertionError("No DocumentType expected, one found");
		}
		if (found == null)
			throw new AssertionError("DocumentType expected, no found");
		Assert.assertEquals(expected.getPublicId(), found.getPublicId());
		Assert.assertEquals(expected.getSystemId(), found.getSystemId());
	}
	
	protected static void checkElement(Element expected, Element found) {
		if (expected == null) {
			if (found == null) return;
			throw new AssertionError("null element expected, found is " + found.getNodeName());
		}
		if (found == null)
			throw new AssertionError("null element found, expected is " + expected.getNodeName());
		Assert.assertEquals(expected.getNodeName(), found.getNodeName());
		if (expected.getLocalName() != null)
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
	
	protected static void checkMap(NamedNodeMap expected, NamedNodeMap found) {
		List<Node> list = new ArrayList<>(expected.getLength());
		for (int i = 0; i < expected.getLength(); ++i) {
			Node node = expected.item(i);
			if (node.getNodeName().equals("xmlns")) continue;
			if (node.getNodeName().startsWith("xmlns:")) continue;
			list.add(node);
		}
		Assert.assertTrue(list.size() == found.getLength());
		for (int i = 0; i < list.size(); ++i) {
			Node n1 = list.get(i);
			boolean ok = false;
			for (int j = 0; j < found.getLength(); ++j) {
				Node n2 = found.item(j);
				if (n1.getNodeName().equals(n2.getNodeName())) {
					ok = true;
					checkNode(n1, n2);
					break;
				}
			}
			if (!ok)
				throw new AssertionError("Attribute not found: " + n1.getNodeName());
		}
	}
	
	protected static void checkList(NodeList expected, NodeList found) {
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
	
	protected static void checkNode(Node expected, Node found) {
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
	
	protected static void checkCData(CDATASection expected, CDATASection found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	protected static void checkComment(Comment expected, Comment found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	protected static void checkText(Text expected, Text found) {
		Assert.assertEquals(expected.getData(), found.getData());
	}
	
	protected static void checkAttr(Attr expected, Attr found) {
		Assert.assertEquals(expected.getNodeName(), found.getNodeName());
		Assert.assertEquals(expected.getNodeValue(), found.getNodeValue());
		if (expected.getLocalName() != null)
			Assert.assertEquals(expected.getLocalName(), found.getLocalName());
		//Assert.assertEquals(expected.getNamespaceURI(), found.getNamespaceURI());
		Assert.assertEquals(expected.getPrefix(), found.getPrefix());
		Assert.assertEquals(expected.getName(), found.getName());
		Assert.assertEquals(expected.getValue(), found.getValue());
		Assert.assertEquals(expected.getTextContent(), found.getTextContent());
	}

}

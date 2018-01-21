package net.lecousin.framework.core.tests.xml;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import net.lecousin.framework.xml.dom.XMLCData;
import net.lecousin.framework.xml.dom.XMLComment;
import net.lecousin.framework.xml.dom.XMLDocument;
import net.lecousin.framework.xml.dom.XMLElement;
import net.lecousin.framework.xml.dom.XMLText;

public class TestDOMModifications extends TestDOM {

	@Test(timeout=120000)
	public void test() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		// create document
		Document doc1 = factory.newDocumentBuilder().newDocument();
		XMLDocument doc2 = new XMLDocument();
		Assert.assertEquals(doc1.getNodeName(), doc2.getNodeName());
		Assert.assertEquals(doc1.getNodeType(), doc2.getNodeType());
		Assert.assertFalse(doc1.hasChildNodes());
		Assert.assertFalse(doc2.hasChildNodes());
		Assert.assertNull(doc1.getFirstChild());
		Assert.assertNull(doc2.getFirstChild());
		Assert.assertNull(doc1.getLastChild());
		Assert.assertNull(doc2.getLastChild());
		checkDocument(doc1, doc2);
		// create root
		Element root1 = doc1.createElement("root");
		doc1.appendChild(root1);
		XMLElement root2 = doc2.createElement("root");
		doc2.appendChild(root2);
		Assert.assertTrue(doc1.hasChildNodes());
		Assert.assertTrue(doc2.hasChildNodes());
		Assert.assertEquals(root1, doc1.getFirstChild());
		Assert.assertEquals(root1, doc1.getLastChild());
		Assert.assertEquals(root2, doc2.getFirstChild());
		Assert.assertEquals(root2, doc2.getLastChild());
		checkDocument(doc1, doc2);
		// add attribute
		root1.setAttribute("a1", "v1");
		root2.setAttribute("a1", "v1");
		checkDocument(doc1, doc2);
		// add attribute
		root1.setAttribute("a2", "v2");
		root2.setAttribute("a2", "v2");
		checkDocument(doc1, doc2);
		// change attribute
		root1.setAttribute("a2", "V2");
		root2.setAttribute("a2", "V2");
		checkDocument(doc1, doc2);
		Assert.assertTrue(root2.hasAttribute("a2"));
		Assert.assertEquals("V2", root2.getAttributeNode("a2").getNodeValue());
		// remove attribute
		root1.removeAttribute("a1");
		root2.removeAttribute("a1");
		checkDocument(doc1, doc2);
		Assert.assertFalse(root2.hasAttribute("a1"));
		// add attribute
		root1.setAttributeNS("http://test", "test:a3", "v3");
		root2.setAttributeNS("http://test", "test:a3", "v3");
		checkDocument(doc1, doc2);
		// add attribute
		root1.setAttributeNS("http://test2", "test2:a3", "v4");
		root2.setAttributeNS("http://test2", "test2:a3", "v4");
		checkDocument(doc1, doc2);
		// remove attribute
		root1.removeAttributeNS("http://test2", "a3");
		root2.removeAttributeNS("http://test2", "a3");
		checkDocument(doc1, doc2);
		// clone attribute
		checkAttr((Attr)root1.getAttributeNode("a2").cloneNode(true), (Attr)root2.getAttributeNode("a2").cloneNode(true));
		// add attribute
		root1.setAttributeNS("http://test3", "test3:b", "bb");
		root2.setAttributeNS("http://test3", "test3:b", "bb");
		checkDocument(doc1, doc2);
		// change prefix
		root1.getAttributeNodeNS("http://test3", "b").setPrefix("tutu");
		root2.getAttributeNodeNS("http://test3", "b").setPrefix("tutu");
		checkDocument(doc1, doc2);
		// change value
		root1.getAttributeNodeNS("http://test3", "b").setValue("BB");
		root2.getAttributeNodeNS("http://test3", "b").setValue("BB");
		checkDocument(doc1, doc2);
		// get owner
		Assert.assertTrue(root2.getAttributeNodeNS("http://test3", "b").getOwnerElement() == root2);
		Assert.assertTrue(root2.getAttributeNodeNS("http://test3", "b").getOwnerDocument() == doc2);
		// add text
		Text text1 = doc1.createTextNode("My Text");
		XMLText text2 = doc2.createTextNode("My Text");
		root1.appendChild(text1);
		root2.appendChild(text2);
		checkDocument(doc1, doc2);
		// change text
		text1.setData("My Text 2");
		text2.setData("My Text 2");
		checkDocument(doc1, doc2);
		text1.setNodeValue("My Text 3");
		text2.setNodeValue("My Text 3");
		checkDocument(doc1, doc2);
		text2.getNodeName();
		// clone Text
		checkText((Text)text1.cloneNode(true), text2.cloneNode(true));
		text2.isElementContentWhitespace();
		// split Text
		text1.splitText(5);
		text2.splitText(5);
		checkDocument(doc1, doc2);
		Assert.assertEquals(text1.getWholeText(), text2.getWholeText());
		// replace text
		text1.replaceWholeText("My Third Text");
		text2.replaceWholeText("My Third Text");
		checkDocument(doc1, doc2);
		// insert CDATA
		CDATASection data1 = doc1.createCDATASection("The data");
		XMLCData data2 = doc2.createCDATASection("The data");
		root1.insertBefore(data1, text1);
		root2.insertBefore(data2, text2);
		checkDocument(doc1, doc2);
		data2.getNodeName();
		// clone CDATA
		checkCData((CDATASection)data1.cloneNode(true), data2.cloneNode(true));
		// insert Comment
		Comment comment1 = doc1.createComment("A comment");
		XMLComment comment2 = doc2.createComment("A comment");
		root1.insertBefore(comment1, text1);
		root2.insertBefore(comment2, text2);
		checkDocument(doc1, doc2);
		comment2.getNodeName();
		// clone Comment
		checkComment((Comment)comment1.cloneNode(true), comment2.cloneNode(true));
		// modify comment
		comment1.substringData(2, 4);
		comment2.substringData(2, 4);
		checkComment(comment1, comment2);
		comment1.appendData("toto");
		comment2.appendData("toto");
		checkComment(comment1, comment2);
		comment1.insertData(3, "tata");
		comment2.insertData(3, "tata");
		checkComment(comment1, comment2);
		comment1.deleteData(6, 4);
		comment2.deleteData(6, 4);
		checkComment(comment1, comment2);
		comment1.deleteData(0, 1);
		comment2.deleteData(0, 1);
		checkComment(comment1, comment2);
		comment1.replaceData(2, 4, "titi");
		comment2.replaceData(2, 4, "titi");
		checkComment(comment1, comment2);
		checkDocument(doc1, doc2);
		Assert.assertEquals(comment1.getTextContent(), comment2.getTextContent());
		comment1.setTextContent("a comment");
		comment2.setTextContent("a comment");
		checkComment(comment1, comment2);
		checkDocument(doc1, doc2);
		Assert.assertEquals(comment1.getTextContent(), comment2.getTextContent());
		// remove CDATA
		root1.removeChild(data1);
		root2.removeChild(data2);
		checkDocument(doc1, doc2);
		// move Comment
		root1.appendChild(comment1);
		root2.appendChild(comment2);
		checkDocument(doc1, doc2);
		// replace Comment
		Comment comment1bis = doc1.createComment("Second comment");
		XMLComment comment2bis = doc2.createComment("Second comment");
		root1.replaceChild(comment1bis, comment1);
		root2.replaceChild(comment2bis, comment2);
		checkDocument(doc1, doc2);
		// get child
		checkNode(root1.getFirstChild(), root2.getFirstChild());
		checkNode(root1.getLastChild(), root2.getLastChild());
		// clone root
		Element root1Clone = (Element)root1.cloneNode(true);
		XMLElement root2Clone = root2.cloneNode(true);
		checkElement(root1Clone, root2Clone);
		root1Clone = (Element)root1.cloneNode(false);
		root2Clone = root2.cloneNode(false);
		checkElement(root1Clone, root2Clone);
	}
	
}

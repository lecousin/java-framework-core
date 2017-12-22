package net.lecousin.framework.core.tests.xml;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.xml.dom.XMLCData;
import net.lecousin.framework.xml.dom.XMLComment;
import net.lecousin.framework.xml.dom.XMLDocument;
import net.lecousin.framework.xml.dom.XMLElement;
import net.lecousin.framework.xml.dom.XMLText;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

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
		checkDocument(doc1, doc2);
		// create root
		Element root1 = doc1.createElement("root");
		doc1.appendChild(root1);
		XMLElement root2 = doc2.createElement("root");
		doc2.appendChild(root2);
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
		// insert CDATA
		CDATASection data1 = doc1.createCDATASection("The data");
		XMLCData data2 = doc2.createCDATASection("The data");
		root1.insertBefore(data1, text1);
		root2.insertBefore(data2, text2);
		checkDocument(doc1, doc2);
		// insert Comment
		Comment comment1 = doc1.createComment("A comment");
		XMLComment comment2 = doc2.createComment("A comment");
		root1.insertBefore(comment1, text1);
		root2.insertBefore(comment2, text2);
		checkDocument(doc1, doc2);
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
		root1.replaceChild(comment1, comment1bis);
		root2.replaceChild(comment2, comment2bis);
		checkDocument(doc1, doc2);
		// get child
		checkNode(root1.getFirstChild(), root2.getFirstChild());
		checkNode(root1.getLastChild(), root2.getLastChild());
		// TODO continue
	}
	
}

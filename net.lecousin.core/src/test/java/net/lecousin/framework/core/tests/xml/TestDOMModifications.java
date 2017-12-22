package net.lecousin.framework.core.tests.xml;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.xml.dom.XMLDocument;
import net.lecousin.framework.xml.dom.XMLElement;
import net.lecousin.framework.xml.dom.XMLText;

import org.junit.Test;
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
		// remove attribute
		root1.removeAttribute("a1");
		root2.removeAttribute("a1");
		checkDocument(doc1, doc2);
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
		Text text1 = doc1.createTextNode("My Text");
		XMLText text2 = doc2.createTextNode("My Text");
		root1.appendChild(text1);
		root2.appendChild(text2);
		checkDocument(doc1, doc2);
		text1.setData("My Text 2");
		text2.setData("My Text 2");
		checkDocument(doc1, doc2);
		// TODO continue
	}
	
}

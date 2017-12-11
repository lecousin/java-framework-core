package net.lecousin.framework.xml.dom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Utility methods to manipulate DOM.
 */
public final class DOMUtil {

	/** Return the first child of the given name, or null. */
	public static Element getChild(Element parent, String childName) {
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node node = nodes.item(i);
			if ((node instanceof Element) && node.getNodeName().equals(childName))
				return (Element)node;
		}
		return null;
	}
	
	/** Return the children with the given name. */
	public static List<Element> getChildren(Element parent, String childName) {
		ArrayList<Element> children = new ArrayList<Element>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node node = nodes.item(i);
			if ((node instanceof Element) && node.getNodeName().equals(childName))
				children.add((Element)node);
		}
		return children;
	}
	
	/** Return the inner text of a node. */
	public static String getInnerText(Node node)
	{
		if (node instanceof Element)
			return ((Element)node).getTextContent();
		if (node.getNodeType() == Node.TEXT_NODE)
			return node.getNodeValue();
		if (node instanceof Text)
			return ((Text)node).getData();
		return null;
	}

	/** Return the inner text of the first child with the given name. */
	public static String getChildText(Element parent, String childName) {
		Element child = getChild(parent, childName);
		if (child == null) return null;
		return getInnerText(child);
	}
	
}

package net.lecousin.framework.xml.dom;

import java.util.List;

import net.lecousin.framework.util.ObjectUtil;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XMLNamedNodeMap implements NamedNodeMap {

	public XMLNamedNodeMap(List<? extends XMLNode> nodes) {
		this.nodes = nodes;
	}
	
	protected List<? extends XMLNode> nodes;
	
	@Override
	public XMLNode getNamedItem(String name) {
		if (nodes == null || name == null)
			return null;
		for (XMLNode node : nodes)
			if (name.equals(node.getNodeName()))
				return node;
		return null;
	}

	@Override
	public XMLNode setNamedItem(Node arg) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public Node removeNamedItem(String name) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public XMLNode item(int index) {
		if (nodes == null || index < 0 || index >= nodes.size())
			return null;
		return nodes.get(index);
	}

	@Override
	public int getLength() {
		return nodes != null ? nodes.size() : 0;
	}

	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		if (nodes == null || localName == null)
			return null;
		for (XMLNode node : nodes)
			if (localName.equals(node.getLocalName()) && ObjectUtil.equalsOrNull(namespaceURI, node.getNamespaceURI()))
				return node;
		return null;
	}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}
	
}

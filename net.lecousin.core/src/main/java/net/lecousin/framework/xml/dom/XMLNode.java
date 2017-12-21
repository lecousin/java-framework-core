package net.lecousin.framework.xml.dom;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.util.ObjectUtil;
import net.lecousin.framework.util.Pair;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/** DOM Node. */
public abstract class XMLNode implements Node {

	protected XMLNode(XMLDocument doc) {
		this.doc = doc;
	}
	
	protected XMLDocument doc;
	protected XMLNode parent = null;
	protected Map<String, Pair<Object, UserDataHandler>> userData = null;
	
	@Override
	public XMLDocument getOwnerDocument() {
		return doc;
	}
	
	protected void setParent(XMLNode parent) {
		if (this.parent != null)
			this.parent.removeChild(this);
		this.parent = parent;
	}
	
	@Override
	public Node getParentNode() {
		return parent;
	}
	
	/** Check if the given node is an ancestor of this node. */
	public boolean isAncestor(XMLNode node) {
		return node == parent || (parent != null && parent.isAncestor(node));
	}

	@Override
	public String getNodeValue() {
		return null;
	}
	
	@Override
	public void setNodeValue(String nodeValue) {
	}

	@Override
	public void setPrefix(String prefix) {
	}
	
	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNamespaceURI() {
		return null;
	}
	
	@Override
	public Node getFirstChild() {
		return null;
	}
	
	@Override
	public Node getLastChild() {
		return null;
	}
	
	@Override
	public boolean hasChildNodes() {
		return false;
	}

	@Override
	public NodeList getChildNodes() {
		return new XMLNodeList(null);
	}
	
	@Override
	public XMLNode getPreviousSibling() {
		if (parent == null) return null;
		if (!(parent instanceof XMLElement)) return null;
		XMLElement p = (XMLElement)parent;
		int i = p.children.indexOf(this);
		if (i <= 0) return null;
		return p.children.get(i - 1);
	}

	@Override
	public XMLNode getNextSibling() {
		if (parent == null) return null;
		if (!(parent instanceof XMLElement)) return null;
		XMLElement p = (XMLElement)parent;
		int i = p.children.indexOf(this);
		if (i < 0 || i == p.children.size() - 1) return null;
		return p.children.get(i + 1);
	}
	
	@Override
	public Node appendChild(Node newChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation not allowed");
	}
	
	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation not allowed");
	}
	
	@Override
	public Node removeChild(Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation not allowed");
	}
	
	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation not allowed");
	}

	
	@Override
	public String lookupPrefix(String namespaceURI) {
		if (parent == null)
			return null;
		return parent.lookupPrefix(namespaceURI);
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		if (parent == null)
			return false;
		return parent.isDefaultNamespace(namespaceURI);
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		if (parent == null)
			return null;
		return parent.lookupNamespaceURI(prefix);
	}

	
	@Override
	public boolean hasAttributes() {
		return false;
	}
	
	@Override
	public NamedNodeMap getAttributes() {
		return new XMLNamedNodeMap(null);
	}
	
	@Override
	public String getBaseURI() {
		return null;
	}
	
	@Override
	public boolean isSupported(String feature, String version) {
		return false;
	}
	
	@Override
	public Object getFeature(String feature, String version) {
		return null;
	}

	@Override
	public boolean isSameNode(Node other) {
		return other == this;
	}
	
	@Override
	public boolean isEqualNode(Node arg) {
		if (arg == null) return false;
		if (!(arg instanceof XMLNode)) return false;
		XMLNode n = (XMLNode)arg;
		if (!getClass().equals(n.getClass())) return false;
		if (!ObjectUtil.equalsOrNull(getNodeName(), n.getNodeName())) return false;
		if (!ObjectUtil.equalsOrNull(getNodeValue(), n.getNodeValue())) return false;
		if (!ObjectUtil.equalsOrNull(getLocalName(), n.getLocalName())) return false;
		if (!ObjectUtil.equalsOrNull(getPrefix(), n.getPrefix())) return false;
		if (!ObjectUtil.equalsOrNull(getNamespaceURI(), n.getNamespaceURI())) return false;
		NamedNodeMap map1 = getAttributes();
		NamedNodeMap map2 = n.getAttributes();
		if (map1 == null) return map2 == null;
		if (map2 == null) return false;
		if (map1.getLength() != map2.getLength()) return false;
		for (int i = 0; i < map1.getLength(); ++i) {
			Node item = map1.item(i);
			if (map2.getNamedItemNS(item.getNamespaceURI(), item.getLocalName()) == null)
				return false;
		}
		NodeList list1 = getChildNodes();
		NodeList list2 = n.getChildNodes();
		if (list1 == null) return list2 == null;
		if (list2 == null) return false;
		if (list1.getLength() != list2.getLength()) return false;
		for (int i = 0; i < list1.getLength(); ++i) {
			Node item = list1.item(i);
			boolean found = false;
			for (int j = 0; j < list2.getLength(); ++j)
				if (item.isEqualNode(list2.item(j))) {
					found = true;
					break;
				}
			if (!found)
				return false;
		}
		return true;
	}
	
	@Override
	public Object getUserData(String key) {
		if (userData == null)
			return null;
		Pair<Object, UserDataHandler> p = userData.get(key);
		if (p == null)
			return null;
		return p.getValue1();
	}
	
	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		if (userData == null)
			userData = new HashMap<>();
		Pair<Object, UserDataHandler> p = userData.put(key, new Pair<>(data, handler));
		return p != null ? p.getValue1() : null;
	}
	
	protected void cloned(XMLNode clone) {
		if (userData == null) return;
		for (Map.Entry<String, Pair<Object, UserDataHandler>> p : userData.entrySet()) {
			UserDataHandler h = p.getValue().getValue2();
			if (h == null) continue;
			h.handle(UserDataHandler.NODE_CLONED, p.getKey(), p.getValue().getValue1(), this, clone);
		}
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "compareDocumentPosition not supported");
	}
	
	@Override
	public void normalize() {
	}
	
}

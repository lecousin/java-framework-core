package net.lecousin.framework.xml.dom;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.ObjectUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLException;
import net.lecousin.framework.xml.XMLStreamEvents;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamEventsSync;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

public class XMLElement extends XMLNode implements Element {

	// TODO use a XMLDocument as owner of all nodes...
	
	public XMLElement(XMLDocument doc, String prefix, String localName) {
		super(doc);
		this.prefix = prefix;
		this.localName = localName;
	}
	
	public XMLElement(XMLDocument doc, XMLStreamEvents.ElementContext context) {
		this(doc, context.namespacePrefix.asString(), context.localName.asString());
		for (Pair<UnprotectedStringBuffer, UnprotectedStringBuffer> ns : context.namespaces)
			declareNamespace(ns.getValue2().asString(), ns.getValue1().asString());
	}
	
	protected String prefix;
	protected String localName;
	protected Map<String, String> prefixToURI = null;
	protected LinkedList<XMLAttribute> attributes = new LinkedList<>();
	protected LinkedList<XMLNode> children = new LinkedList<>();

	@Override
	public XMLElement cloneNode(boolean deep) {
		XMLElement clone = new XMLElement(doc, prefix, localName);
		if (prefixToURI != null) clone.prefixToURI = new HashMap<>(prefixToURI);
		for (XMLAttribute a : attributes)
			clone.addAttribute(a.cloneNode(false));
		if (deep)
			for (XMLNode child : children)
				clone.appendChild(child.cloneNode(true));
		cloned(clone);
		return clone;
	}
	
	@Override
	public short getNodeType() {
		return Node.ELEMENT_NODE;
	}
	
	@Override
	public String getNodeName() {
		if (prefix != null && prefix.length() > 0)
			return prefix + ':' + localName;
		return localName;
	}
	
	@Override
	public String getTagName() {
		if (prefix != null && prefix.length() > 0)
			return prefix + ':' + localName;
		return localName;
	}
	
	@Override
	public String getLocalName() {
		return localName;
	}
	
	@Override
	public String getPrefix() {
		return prefix == null || prefix.length() == 0 ? null : prefix;
	}
	
	@Override
	public void setPrefix(String prefix) throws DOMException {
		this.prefix = prefix;
	}
	
	@Override
	public String getNamespaceURI() {
		return lookupNamespaceURI(prefix);
		//return prefixToURI == null ? null : prefixToURI.get(prefix);
	}
	
	@Override
	public XMLNode appendChild(Node newChild) throws DOMException {
		if (!(newChild instanceof XMLNode))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild must implement XMLNode");
		if (newChild == this)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert a node inside itself");
		XMLNode child = (XMLNode)newChild;
		if (isAncestor(child))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert an ancestor into a descendent");
		child.setParent(this);
		children.add(child);
		return child;
	}
	
	@Override
	public XMLNode insertBefore(Node newChild, Node refChild) throws DOMException {
		if (!(newChild instanceof XMLNode))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild must implement XMLNode");
		if (newChild == this)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert a node inside itself");
		XMLNode child = (XMLNode)newChild;
		if (isAncestor(child))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert an ancestor into a descendent");
		if (refChild == null) {
			child.setParent(this);
			children.add(child);
			return child;
		}
		int i = children.indexOf(refChild);
		if (i < 0)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "refChild is not a child of this element");
		child.setParent(this);
		children.add(i, child);
		return child;
	}
	
	@Override
	public XMLNode removeChild(Node oldChild) throws DOMException {
		if (!(oldChild instanceof XMLNode))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "oldChild must implement XMLNode");
		XMLNode child = (XMLNode)oldChild;
		if (child.parent != this)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "The given child is not a child of this Element");
		child.parent = null;
		children.remove(child);
		return child;
	}
	
	@Override
	public XMLNode replaceChild(Node newChild, Node oldChild) throws DOMException {
		if (!(newChild instanceof XMLNode))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild must implement XMLNode");
		if (newChild == this)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert a node inside itself");
		if (!(oldChild instanceof XMLNode))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "oldChild must implement XMLNode");
		XMLNode neChild = (XMLNode)newChild;
		XMLNode olChild = (XMLNode)oldChild;
		if (olChild.parent != this)
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "The given oldChild is not a child of this Element");
		if (isAncestor(neChild))
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert an ancestor into a descendent");
		int i = children.indexOf(olChild);
		olChild.parent = null;
		neChild.setParent(this);
		children.set(i, neChild);
		return olChild;
	}
	
	@Override
	public Node getFirstChild() {
		return children.peekFirst();
	}
	
	@Override
	public Node getLastChild() {
		return children.peekLast();
	}
	
	@Override
	public boolean hasChildNodes() {
		return !children.isEmpty();
	}

	@Override
	public NodeList getChildNodes() {
		return new XMLNodeList(children);
	}
	
	
	@Override
	public boolean hasAttributes() {
		return !attributes.isEmpty();
	}
	
	@Override
	public boolean hasAttribute(String name) {
		for (XMLAttribute a : attributes)
			if (a.getNodeName().equals(name))
				return true;
		return false;
	}
	
	@Override
	public NamedNodeMap getAttributes() {
		return new XMLNamedNodeMap(attributes);
	}
	
	@Override
	public String getAttribute(String name) {
		for (XMLAttribute a : attributes)
			if (a.getNodeName().equals(name))
				return a.getNodeValue();
		return null;
	}

	@Override
	public void setAttribute(String name, String value) throws DOMException {
		for (XMLAttribute a : attributes)
			if (a.getNodeName().equals(name)) {
				a.setNodeValue(value);
				return;
			}
		addAttribute(new XMLAttribute(doc, "", name, value));
	}

	@Override
	public void removeAttribute(String name) throws DOMException {
		for (Iterator<XMLAttribute> it = attributes.iterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.getNodeName().equals(name)) {
				a.parent = null;
				it.remove();
				return;
			}
		}
	}

	@Override
	public XMLAttribute getAttributeNode(String name) {
		for (XMLAttribute a : attributes)
			if (a.getNodeName().equals(name))
				return a;
		return null;
	}

	@Override
	public XMLAttribute setAttributeNode(Attr newAttr) throws DOMException {
		if (!(newAttr instanceof XMLAttribute))
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "newAttr must be a XMLAttribute");
		XMLAttribute na = (XMLAttribute)newAttr;
		for (ListIterator<XMLAttribute> it = attributes.listIterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.isEqualNode(na)) {
				it.set(na);
				a.parent = null;
				return na;
			}
		}
		addAttribute(na);
		return na;
	}

	@Override
	public XMLAttribute removeAttributeNode(Attr oldAttr) throws DOMException {
		if (!(oldAttr instanceof XMLAttribute))
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "oldAttr must be a XMLAttribute");
		XMLAttribute na = (XMLAttribute)oldAttr;
		for (Iterator<XMLAttribute> it = attributes.iterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.isEqualNode(na)) {
				it.remove();
				a.parent = null;
				return a;
			}
		}
		throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute not found in this element");
	}
	
	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		String prefix = getPrefixForNamespaceURI(namespaceURI);
		if (prefix == null)
			return false;
		for (XMLAttribute a : attributes)
			if (prefix.equals(a.prefix) && localName.equals(a.localName))
				return true;
		return false;
	}
	
	@Override
	public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
		String prefix = getPrefixForNamespaceURI(namespaceURI);
		if (prefix == null)
			return "";
		for (XMLAttribute a : attributes)
			if (prefix.equals(a.prefix) && localName.equals(a.localName))
				return a.value == null ? "" : a.value;
		return "";
	}

	@Override
	public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
		int i = qualifiedName.indexOf(':');
		String prefix;
		String localName;
		if (i < 0) {
			prefix = "";
			localName = qualifiedName;
		} else {
			prefix = qualifiedName.substring(0, i);
			localName = qualifiedName.substring(i + 1);
		}
		if (prefixToURI != null) {
			String uri = prefixToURI.get(prefix);
			if (uri != null && !uri.equals(namespaceURI))
				throw new DOMException(DOMException.NAMESPACE_ERR, "Prefix " + prefix + " is already used for namespace " + uri);
			if (uri == null)
				prefixToURI.put(prefix, namespaceURI);
		} else if (namespaceURI != null || !prefix.isEmpty()) {
			declareNamespace(namespaceURI, prefix);
		}
		XMLAttribute a = new XMLAttribute(doc, prefix, localName, value);
		setAttributeNode(a);
	}

	@Override
	public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
		String prefix = getPrefixForNamespaceURI(namespaceURI);
		for (Iterator<XMLAttribute> it = attributes.iterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.localName.equals(localName) && a.prefix.equals(prefix)) {
				it.remove();
				a.parent = null;
				return;
			}
		}
	}

	@Override
	public XMLAttribute getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
		String prefix = getPrefixForNamespaceURI(namespaceURI);
		for (Iterator<XMLAttribute> it = attributes.iterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.localName.equals(localName) && a.prefix.equals(prefix))
				return a;
		}
		return null;
	}

	@Override
	public XMLAttribute setAttributeNodeNS(Attr newAttr) throws DOMException {
		if (!(newAttr instanceof XMLAttribute))
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "newAttr must be a XMLAttribute");
		XMLAttribute na = (XMLAttribute)newAttr;
		for (ListIterator<XMLAttribute> it = attributes.listIterator(); it.hasNext(); ) {
			XMLAttribute a = it.next();
			if (a.isEqualNode(na)) {
				it.set(na);
				return na;
			}
		}
		addAttribute(na);
		return na;
	}
	
	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {
		XMLAttribute a = getAttributeNode(name);
		if (a == null)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute " + name + " does not exist on this element");
		a.isId = isId;
	}

	@Override
	public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
		XMLAttribute a = getAttributeNodeNS(namespaceURI, localName);
		if (a == null)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute " + localName + " does not exist on this element");
		a.isId = isId;
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		if (!attributes.contains(idAttr))
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute " + localName + " does not exist on this element");
		XMLAttribute a = (XMLAttribute)idAttr;
		a.isId = isId;
	}
	
	public void addAttribute(XMLAttribute a) {
		a.setParent(this);
		attributes.add(a);
	}

	
	public String getPrefixForNamespaceURI(String namespaceURI) {
		if (namespaceURI == null)
			return "";
		if (prefixToURI != null) {
			for (Map.Entry<String, String> e : prefixToURI.entrySet())
				if (e.getValue().equals(namespaceURI))
					return e.getKey();
		}
		if (parent == null)
			return null;
		if (parent instanceof XMLElement)
			return ((XMLElement)parent).getPrefixForNamespaceURI(namespaceURI);
		return parent.lookupPrefix(namespaceURI);
	}
	
	@Override
	public String lookupPrefix(String namespaceURI) {
		if (prefixToURI != null)
			for (Map.Entry<String, String> e : prefixToURI.entrySet())
				if (e.getValue().equals(namespaceURI)) {
					if (e.getKey().length() > 0)
						return e.getKey();
				}
		if (parent == null)
			return null;
		return parent.lookupPrefix(namespaceURI);
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		if (prefixToURI != null)
			for (Map.Entry<String, String> e : prefixToURI.entrySet())
				if (e.getKey().length() == 0)
					return e.getValue().equals(namespaceURI);
		if (parent == null)
			return false;
		return parent.isDefaultNamespace(namespaceURI);
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		if (prefixToURI != null) {
			String uri = prefixToURI.get(prefix);
			if (uri != null) return uri;
		}
		if (parent == null)
			return null;
		return parent.lookupNamespaceURI(prefix);
	}

	public void declareNamespace(String uri, String prefix) {
		if (prefixToURI == null)
			prefixToURI = new HashMap<>(5);
		prefixToURI.put(prefix, uri);
	}
	
	@Override
	public NodeList getElementsByTagName(String name) {
		LinkedList<XMLNode> elements = new LinkedList<>();
		getElementsByTagName(name, elements);
		return new XMLNodeList(elements);
	}
	
	protected void getElementsByTagName(String name, List<XMLNode> result) {
		for (XMLNode child : children) {
			if (!(child instanceof XMLElement)) continue;
			XMLElement e = (XMLElement)child;
			if (e.getNodeName().equals(name))
				result.add(e);
			else
				e.getElementsByTagName(name, result);
		}
	}
	
	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
		LinkedList<XMLNode> elements = new LinkedList<>();
		getElementsByTagName(namespaceURI, localName, elements);
		return new XMLNodeList(elements);
	}

	protected void getElementsByTagName(String namespaceURI, String localName, List<XMLNode> result) {
		for (XMLNode child : children) {
			if (!(child instanceof XMLElement)) continue;
			XMLElement e = (XMLElement)child;
			if (e.getLocalName().equals(localName) && ObjectUtil.equalsOrNull(namespaceURI, e.getNamespaceURI()))
				result.add(e);
			else
				e.getElementsByTagName(namespaceURI, localName, result);
		}
	}
	
	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}
	
	@Override
	public String getTextContent() {
		if (children.isEmpty()) return "";
		StringBuilder s = new StringBuilder();
		for (XMLNode child : children) {
			if (child instanceof XMLComment) continue;
			if (child instanceof XMLText) {
				if (((XMLText)child).isElementContentWhitespace()) continue;
			}
			s.append(child.getTextContent());
		}
		return s.toString();
	}
	
	@Override
	public void setTextContent(String textContent) {
		while (!children.isEmpty())
			removeChild(children.getFirst());
		appendChild(new XMLText(doc, textContent));
	}

	@Override
	public void normalize() {
		for (int pos = 1; pos < children.size(); ++pos) {
			XMLNode child = children.get(pos);
			if (!(child instanceof XMLText)) continue;
			XMLNode prev = children.get(pos - 1);
			if (!(prev instanceof XMLText)) continue;
			prev.setTextContent(prev.getTextContent() + child.getTextContent());
			removeChild(child);
			pos--;
		}
	}

	
	public static XMLElement create(XMLDocument doc, XMLStreamEventsSync stream) throws XMLException, IOException, IllegalStateException {
		if (!Event.Type.START_ELEMENT.equals(stream.event.type))
			throw new IllegalStateException("Method XMLElement.create must be called with a stream being on a START_ELEMENT event");
		XMLElement element = new XMLElement(doc, stream.event.context.getFirst());
		for (XMLStreamEvents.Attribute a : stream.event.attributes)
			element.addAttribute(new XMLAttribute(doc, a.namespacePrefix.asString(), a.localName.asString(), a.value != null ? a.value.asString() : null));
		element.parseContent(stream);
		return element;
	}
	
	public static AsyncWork<XMLElement, Exception> create(XMLDocument doc, XMLStreamEventsAsync stream) throws IllegalStateException {
		if (!Event.Type.START_ELEMENT.equals(stream.event.type))
			throw new IllegalStateException("Method XMLElement.create must be called with a stream being on a START_ELEMENT event");
		XMLElement element = new XMLElement(doc, stream.event.context.getFirst());
		for (XMLStreamEvents.Attribute a : stream.event.attributes)
			element.addAttribute(new XMLAttribute(doc, a.namespacePrefix.asString(), a.localName.asString(), a.value != null ? a.value.asString() : null));
		ISynchronizationPoint<Exception> parse = element.parseContent(stream);
		AsyncWork<XMLElement, Exception> result = new AsyncWork<>();
		if (parse.isUnblocked()) {
			if (parse.hasError()) result.error(parse.getError());
			else result.unblockSuccess(element);
			return result;
		}
		parse.listenInline(() -> { result.unblockSuccess(element); }, result);
		return result;
	}
	
	public void parseContent(XMLStreamEventsSync stream) throws XMLException, IOException {
		if (stream.event.isClosed) return;
		do {
			stream.next();
			switch (stream.event.type) {
			case START_ELEMENT:
				appendChild(create(doc, stream));
				break;
			case END_ELEMENT:
				return;
			case TEXT:
				appendChild(new XMLText(doc, stream.event.text.asString()));
				break;
			case CDATA:
				appendChild(new XMLCData(doc, stream.event.text.asString()));
				break;
			case COMMENT:
				appendChild(new XMLComment(doc, stream.event.text.asString()));
				break;
			case PROCESSING_INSTRUCTION:
				// TODO
				break;
			default:
				// TODO XMLException
				throw new IOException("Unexpected XML event " + stream.event.type + " in an element");
			}
		} while (true);
	}
	
	public ISynchronizationPoint<Exception> parseContent(XMLStreamEventsAsync stream) {
		if (stream.event.isClosed) return new SynchronizationPoint<>(true);
		return parseContent(stream, null);
	}
	
	private ISynchronizationPoint<Exception> parseContent(XMLStreamEventsAsync stream, ISynchronizationPoint<Exception> s) {
		do {
			ISynchronizationPoint<Exception> next = s != null ? s : stream.next();
			if (next.isUnblocked()) {
				if (next.hasError()) return next;
				switch (stream.event.type) {
				case START_ELEMENT: {
					AsyncWork<XMLElement, Exception> child = create(doc, stream);
					if (child.isUnblocked()) {
						if (child.hasError()) return child;
						appendChild(child.getResult());
						break;
					}
					SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
					child.listenAsync(new Task.Cpu<Void, NoException>("Parsing XML to DOM", stream.getPriority()) {
						@Override
						public Void run() {
							appendChild(child.getResult());
							parseContent(stream, null).listenInlineSP(sp);
							return null;
						}
					}, sp);
					return sp;
				}
				case END_ELEMENT:
					return next;
				case TEXT:
					appendChild(new XMLText(doc, stream.event.text.asString()));
					break;
				case CDATA:
					appendChild(new XMLCData(doc, stream.event.text.asString()));
					break;
				case COMMENT:
					appendChild(new XMLComment(doc, stream.event.text.asString()));
					break;
				case PROCESSING_INSTRUCTION:
					// TODO
					break;
				default:
					// TODO XMLException
					return new SynchronizationPoint<>(new IOException("Unexpected XML event " + stream.event.type + " in an element"));
				}
				s = null;
				continue;
			}
			// blocked
			SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
			next.listenAsync(new Task.Cpu<Void, NoException>("Parsing XML to DOM", stream.getPriority()) {
				@Override
				public Void run() {
					parseContent(stream, next).listenInlineSP(sp);
					return null;
				}
			}, sp);
			return sp;
		} while (true);
	}

}

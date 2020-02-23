package net.lecousin.framework.xml.dom;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.xml.XMLException;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamEventsSync;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/** DOM Document. */
public class XMLDocument extends XMLNode implements Document {

	/** Constructor. */
	public XMLDocument() {
		super(null);
		this.doc = this;
	}
	
	protected XMLDocumentType docType = null;
	protected XMLElement root = null;
	
	/** Set the document type. */
	public void setDocumentType(XMLDocumentType docType) {
		docType.doc = this;
		this.docType = docType;
	}
	
	/** Set the root element. */
	public void setDocumentElement(XMLElement root) {
		root.doc = this;
		this.root = root;
	}
	
	@Override
	public Node appendChild(Node newChild) {
		if (newChild instanceof XMLElement) {
			if (root == null) {
				root = (XMLElement)newChild;
				return root;
			}
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Root element already present on document");
		}
		if (newChild instanceof XMLDocumentType) {
			if (docType == null) {
				docType = (XMLDocumentType)newChild;
				return docType;
			}
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Document type already present on document");
		}
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
			"Cannot append a node " + newChild.getClass().getName() + " on XMLDocument");
	}
	
	@Override
	public XMLNode getFirstChild() {
		return docType != null ? docType : root;
	}
	
	@Override
	public XMLNode getLastChild() {
		return root != null ? root : docType;
	}
	
	@Override
	public boolean hasChildNodes() {
		return docType != null || root != null;
	}
	
	@Override
	public NodeList getChildNodes() {
		ArrayList<XMLNode> list = new ArrayList<>(2);
		if (docType != null) list.add(docType);
		if (root != null) list.add(root);
		return new XMLNodeList(list);
	}
	
	/** Create a document from a XMLStreamEvents. */
	public static XMLDocument create(XMLStreamEventsSync stream) throws XMLException, IOException {
		XMLDocument doc = new XMLDocument();
		do {
			switch (stream.event.type) {
			case DOCTYPE:
				if (doc.docType != null)
					throw new XMLException(stream.getCharacterStream(), stream.event.context,
						XMLException.LOCALIZED_MESSAGE_UNEXPECTED_ELEMENT, "DOCTYPE");
				doc.docType = new XMLDocumentType(doc, stream.event.text.asString(),
					stream.event.publicId != null ? stream.event.publicId.asString() : null,
					stream.event.system != null ? stream.event.system.asString() : null);
				break;
			case START_ELEMENT:
				if (doc.root != null)
					throw new XMLException(stream.getCharacterStream(), stream.event.context,
						XMLException.LOCALIZED_MESSAGE_UNEXPECTED_ELEMENT, stream.event.text.asString());
				doc.root = XMLElement.create(doc, stream);
				break;
			default: break;
			}
			try { stream.next(); }
			catch (EOFException e) {
				break;
			}
		} while (true);
		return doc;
	}
	
	/** Create a document from a XMLStreamEvents. */
	public static AsyncSupplier<XMLDocument, Exception> create(XMLStreamEventsAsync stream) {
		XMLDocument doc = new XMLDocument();
		AsyncSupplier<XMLDocument, Exception> result = new AsyncSupplier<>();
		create(doc, stream, result, new Async<>(true));
		return result;
	}
	
	@SuppressWarnings("java:S3776") // complexity
	private static void create(
		XMLDocument doc, XMLStreamEventsAsync stream, AsyncSupplier<XMLDocument, Exception> result, IAsync<Exception> n
	) {
		do {
			IAsync<Exception> next = n != null ? n : stream.next();
			n = null;
			if (next.isDone()) {
				if (next.hasError()) {
					if (next.getError() instanceof EOFException) {
						result.unblockSuccess(doc);
						return;
					}
					result.error(next.getError());
					return;
				}
				switch (stream.event.type) {
				case DOCTYPE:
					if (doc.docType != null) {
						// TODO XMLException
						result.error(new IOException("Unexpected element DOCTYPE"));
						return;
					}
					doc.docType = new XMLDocumentType(doc, stream.event.text.asString(),
						stream.event.publicId != null ? stream.event.publicId.asString() : null,
						stream.event.system != null ? stream.event.system.asString() : null);
					break;
				case START_ELEMENT:
					if (doc.root != null) {
						// TODO XMLException
						result.error(new IOException(XMLException.LOCALIZED_MESSAGE_UNEXPECTED_ELEMENT + ' '
							+ stream.event.text.asString()));
						return;
					}
					AsyncSupplier<XMLElement, Exception> root = XMLElement.create(doc, stream);
					if (root.isDone()) {
						if (root.hasError()) {
							result.error(root.getError());
							return;
						}
						doc.root = root.getResult();
						break;
					}
					root.thenStart("Parsing XML root element", stream.getPriority(), () -> {
						doc.root = root.getResult();
						create(doc, stream, result, null);
						return null;
					}, result);
					return;
				default: break;
				}
				continue;
			}
			next.thenStart("Parsing XML", stream.getPriority(), () -> {
				create(doc, stream, result, next);
				return null;
			}, true);
			return;
		} while (true);
	}

	@Override
	public String getNodeName() {
		return "#document";
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	public XMLDocument cloneNode(boolean deep) {
		return null;
	}

	@Override
	public DocumentType getDoctype() {
		return docType;
	}

	@Override
	public XMLDomImplementation getImplementation() {
		return XMLDomImplementation.getInstance();
	}

	@Override
	public XMLElement getDocumentElement() {
		return root;
	}

	@Override
	public XMLElement createElement(String tagName) {
		String prefix;
		String localName;
		int i = tagName.indexOf(':');
		if (i < 0) {
			prefix = "";
			localName = tagName;
		} else {
			prefix = tagName.substring(0, i);
			localName = tagName.substring(i + 1);
		}
		return new XMLElement(this, prefix, localName);
	}

	@Override
	public XMLElement createElementNS(String namespaceURI, String qualifiedName) {
		String prefix;
		String localName;
		int i = qualifiedName.indexOf(':');
		if (i < 0) {
			prefix = "";
			localName = qualifiedName;
		} else {
			prefix = qualifiedName.substring(0, i);
			localName = qualifiedName.substring(i + 1);
		}
		XMLElement e = new XMLElement(this, prefix, localName);
		if (namespaceURI != null)
			e.declareNamespace(namespaceURI, prefix);
		return e;
	}
	
	@Override
	public DocumentFragment createDocumentFragment() {
		throw DOMErrors.operationNotSupported();
	}

	@Override
	public XMLText createTextNode(String data) {
		return new XMLText(this, data);
	}

	@Override
	public XMLComment createComment(String data) {
		return new XMLComment(this, data);
	}

	@Override
	public XMLCData createCDATASection(String data) {
		return new XMLCData(this, data);
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String target, String data) {
		throw DOMErrors.operationNotSupported();
	}
	
	@Override
	public String getTextContent() {
		return null;
	}
	
	@Override
	public void setTextContent(String textContent) {
		// nothing
	}
	
	@Override
	public void normalize() {
		if (root != null)
			root.normalize();
	}

	@Override
	public XMLAttribute createAttribute(String name) {
		return new XMLAttribute(this, "", name, null);
	}

	@Override
	public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
		throw DOMErrors.operationNotSupported();
	}

	@Override
	public EntityReference createEntityReference(String name) {
		throw DOMErrors.operationNotSupported();
	}

	@Override
	public NodeList getElementsByTagName(String tagname) {
		if (root == null) return new XMLNodeList(null);
		return root.getElementsByTagName(tagname);
	}

	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
		if (root == null) return new XMLNodeList(null);
		return root.getElementsByTagNameNS(namespaceURI, localName);
	}

	@Override
	public XMLElement getElementById(String elementId) {
		if (root == null)
			return null;
		return getElementById(elementId, root);
	}
	
	protected XMLElement getElementById(String id, XMLElement e) {
		for (XMLAttribute a : e.attributes)
			if (a.isId && id.equals(a.getNodeValue()))
				return e;
		for (XMLNode child : e.children)
			if (child instanceof XMLElement) {
				XMLElement el = getElementById(id, (XMLElement)child);
				if (el != null)
					return el;
			}
		return null;
	}

	@Override
	public String getInputEncoding() {
		return "UTF-8"; // TODO
	}

	@Override
	public String getXmlEncoding() {
		return "UTF-8"; // TODO
	}

	@Override
	public boolean getXmlStandalone() {
		return true; // TODO
	}

	@Override
	public void setXmlStandalone(boolean xmlStandalone) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getXmlVersion() {
		return "1.0"; // TODO
	}

	@Override
	public void setXmlVersion(String xmlVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getStrictErrorChecking() {
		return false;
	}

	@Override
	public void setStrictErrorChecking(boolean strictErrorChecking) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDocumentURI() {
		return null; // TODO
	}

	@Override
	public void setDocumentURI(String documentURI) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node importNode(Node importedNode, boolean deep) {
		// TODO UserDataHandler
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public Node adoptNode(Node source) {
		// TODO UserDataHandler
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public DOMConfiguration getDomConfig() {
		return null; // TODO
	}

	@Override
	public void normalizeDocument() {
		if (root != null)
			root.normalize();
	}

	@Override
	public Node renameNode(Node n, String namespaceURI, String qualifiedName) {
		// TODO UserDataHandler
		throw DOMErrors.operationNotSupported();
	}
	
}

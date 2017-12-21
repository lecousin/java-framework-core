package net.lecousin.framework.xml.dom;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
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
					throw new XMLException(stream.getPosition(), "Unexpected element ", "DOCTYPE");
				doc.docType = new XMLDocumentType(doc, stream.event.text.asString(),
					stream.event.publicId != null ? stream.event.publicId.asString() : null,
					stream.event.system != null ? stream.event.system.asString() : null);
				break;
			case START_ELEMENT:
				if (doc.root != null)
					throw new XMLException(stream.getPosition(), "Unexpected element ", stream.event.text.asString());
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
	public static AsyncWork<XMLDocument, Exception> create(XMLStreamEventsAsync stream) {
		XMLDocument doc = new XMLDocument();
		AsyncWork<XMLDocument, Exception> result = new AsyncWork<>();
		create(doc, stream, result, new SynchronizationPoint<>(true));
		return result;
	}
	
	private static void create(
		XMLDocument doc, XMLStreamEventsAsync stream, AsyncWork<XMLDocument, Exception> result, ISynchronizationPoint<Exception> n
	) {
		do {
			ISynchronizationPoint<Exception> next = n != null ? n : stream.next();
			n = null;
			if (next.isUnblocked()) {
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
						result.error(new IOException("Unexpected element " + stream.event.text.asString()));
						return;
					}
					AsyncWork<XMLElement, Exception> root = XMLElement.create(doc, stream);
					if (root.isUnblocked()) {
						if (root.hasError()) {
							result.error(root.getError());
							return;
						}
						doc.root = root.getResult();
						break;
					}
					root.listenAsync(new Task.Cpu<Void, NoException>("Parsing XML root element", stream.getPriority()) {
						@Override
						public Void run() {
							doc.root = root.getResult();
							create(doc, stream, result, null);
							return null;
						}
					}, result);
					return;
				default: break;
				}
				continue;
			}
			next.listenAsync(new Task.Cpu<Void, NoException>("Parsing XML", stream.getPriority()) {
				@Override
				public Void run() {
					create(doc, stream, result, next);
					return null;
				}
			}, result);
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
	public XMLElement createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
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
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
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
	public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}
	
	@Override
	public String getTextContent() {
		return null;
	}
	
	@Override
	public void setTextContent(String textContent) {
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
	public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public EntityReference createEntityReference(String name) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public NodeList getElementsByTagName(String tagname) {
		// TODO
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
		// TODO
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
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
	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getXmlVersion() {
		return "1.0"; // TODO
	}

	@Override
	public void setXmlVersion(String xmlVersion) throws DOMException {
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
	public Node importNode(Node importedNode, boolean deep) throws DOMException {
		// TODO UserDataHandler
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}

	@Override
	public Node adoptNode(Node source) throws DOMException {
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
	public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
		// TODO UserDataHandler
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}
	
}

package net.lecousin.framework.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;

/** DOM implementation. */
public class XMLDomImplementation implements DOMImplementation {

	private XMLDomImplementation() {}
	
	private static XMLDomImplementation instance = new XMLDomImplementation();
	
	public static XMLDomImplementation getInstance() { return instance; }

	@Override
	public XMLDocumentType createDocumentType(String qualifiedName, String publicId, String systemId) throws DOMException {
		return new XMLDocumentType(null, qualifiedName, publicId, systemId);
	}

	@Override
	public XMLDocument createDocument(String namespaceURI, String qualifiedName, DocumentType doctype) throws DOMException {
		XMLDocument doc = new XMLDocument();
		if (doctype != null) {
			if (!(doctype instanceof XMLDocumentType))
				throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "doctype must be a XMLDocumentType");
			doc.setDocumentType((XMLDocumentType)doctype);
		}
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
		XMLElement root = new XMLElement(doc, prefix, localName);
		if (namespaceURI != null)
			root.declareNamespace(namespaceURI, prefix);
		doc.setDocumentElement(root);
		return doc;
	}

	@Override
	public boolean hasFeature(String feature, String version) {
		return false;
	}

	@Override
	public Object getFeature(String feature, String version) {
		return null;
	}
	
}

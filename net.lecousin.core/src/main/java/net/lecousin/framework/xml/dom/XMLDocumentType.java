package net.lecousin.framework.xml.dom;

import net.lecousin.framework.util.ObjectUtil;

import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** DOM DocumentType. */
public class XMLDocumentType extends XMLNode implements DocumentType {

	/** Constructor. */
	public XMLDocumentType(XMLDocument doc, String name, String publicId, String systemId) {
		super(doc);
		this.name = name;
		this.publicId = publicId;
		this.systemId = systemId;
	}
	
	protected String name;
	protected String publicId;
	protected String systemId;
	
	@Override
	public String getPublicId() {
		return publicId;
	}
	
	@Override
	public String getSystemId() {
		return systemId;
	}
	
	@Override
	public boolean isEqualNode(Node arg) {
		if (!super.isEqualNode(arg)) return false;
		XMLDocumentType o = (XMLDocumentType)arg;
		if (!name.equals(o.name)) return false;
		return ObjectUtil.equalsOrNull(publicId, o.publicId) && ObjectUtil.equalsOrNull(systemId, o.systemId);
	}

	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_TYPE_NODE;
	}

	@Override
	public XMLDocumentType cloneNode(boolean deep) {
		XMLDocumentType clone = new XMLDocumentType(doc, name, publicId, systemId);
		cloned(clone);
		return clone;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public NamedNodeMap getEntities() {
		return new XMLNamedNodeMap(null);
	}

	@Override
	public NamedNodeMap getNotations() {
		return new XMLNamedNodeMap(null);
	}

	@Override
	public String getInternalSubset() {
		return null;
	}
	
	@Override
	public String getTextContent() {
		return null;
	}
	
	@Override
	public void setTextContent(String textContent) {
		// nothing
	}
	
}

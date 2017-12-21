package net.lecousin.framework.xml.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;

/** DOM attribute. */
public class XMLAttribute extends XMLNode implements Attr {

	/** Constructor. */
	public XMLAttribute(XMLDocument doc, String prefix, String localName, String value) {
		super(doc);
		this.prefix = prefix;
		this.localName = localName;
		this.value = value;
	}
	
	protected String prefix;
	protected String localName;
	protected String value;
	protected boolean isId = false;
	
	@Override
	public XMLAttribute cloneNode(boolean deep) {
		XMLAttribute clone = new XMLAttribute(doc, prefix, localName, value);
		clone.isId = isId;
		cloned(clone);
		return clone;
	}
	
	@Override
	public short getNodeType() {
		return Node.ATTRIBUTE_NODE;
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
	public String getLocalName() {
		return localName;
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String getNodeName() {
		if (prefix != null && prefix.length() > 0)
			return prefix + ':' + localName;
		return localName;
	}
	
	@Override
	public String getNodeValue() {
		return value;
	}
	
	@Override
	public void setNodeValue(String nodeValue) {
		this.value = nodeValue;
	}
	
	@Override
	public String getName() {
		return prefix.length() > 0 ? prefix + ':' + localName : localName;
	}
	
	@Override
	public XMLElement getOwnerElement() {
		return parent == null ? null : (XMLElement)parent;
	}
	
	@Override
	public boolean getSpecified() {
		return value != null;
	}
	
	@Override
	public boolean isId() {
		return isId;
	}

	@Override
	public String getNamespaceURI() {
		if (parent == null) return null;
		if (((XMLElement)parent).prefixToURI == null) return null;
		return ((XMLElement)parent).prefixToURI.get(prefix);
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}
	
	@Override
	public String getTextContent() {
		return value != null ? value : "";
	}
	
	@Override
	public void setTextContent(String textContent) {
	}
	
}

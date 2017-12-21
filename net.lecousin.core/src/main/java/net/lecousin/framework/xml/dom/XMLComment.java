package net.lecousin.framework.xml.dom;

import javax.xml.soap.Node;

import org.w3c.dom.Comment;

/** DOM Comment. */
public class XMLComment extends XMLCharacterData implements Comment {

	/** Constructor. */
	public XMLComment(XMLDocument doc, String text) {
		super(doc, text);
	}
	
	@Override
	public short getNodeType() {
		return Node.COMMENT_NODE;
	}
	
	@Override
	public String getNodeName() {
		return "#comment";
	}
	
	@Override
	public XMLComment cloneNode(boolean deep) {
		XMLComment clone = new XMLComment(doc, text);
		cloned(clone);
		return clone;
	}
	
}

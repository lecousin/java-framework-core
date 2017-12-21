package net.lecousin.framework.xml.dom;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Node;

// skip checkstyle: AbbreviationAsWordInName
/** DOM CDATASection. */
public class XMLCData extends XMLText implements CDATASection {

	/** Constructor. */
	public XMLCData(XMLDocument doc, String text) {
		super(doc, text);
	}
	
	@Override
	public short getNodeType() {
		return Node.CDATA_SECTION_NODE;
	}
	
	@Override
	public String getNodeName() {
		return "#cdata-section";
	}
	
	@Override
	public XMLCData cloneNode(boolean deep) {
		XMLCData clone = new XMLCData(doc, text);
		cloned(clone);
		return clone;
	}
	
}

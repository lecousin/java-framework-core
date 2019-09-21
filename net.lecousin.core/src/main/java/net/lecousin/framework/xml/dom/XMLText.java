package net.lecousin.framework.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/** DOM Text. */
public class XMLText extends XMLCharacterData implements Text {

	/** Constructor. */
	public XMLText(XMLDocument doc, String text) {
		super(doc, text);
	}
	
	@Override
	public short getNodeType() {
		return Node.TEXT_NODE;
	}
	
	@Override
	public String getNodeName() {
		return "#text";
	}
	
	@Override
	public XMLText cloneNode(boolean deep) {
		XMLText clone = new XMLText(doc, text);
		cloned(clone);
		return clone;
	}

	@Override
	public XMLText splitText(int offset) {
		if (parent == null)
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Cannot split a text node which has no parent");
		String s;
		if (offset >= text.length())
			s = "";
		else {
			s = text.substring(offset);
			text = text.substring(0, offset);
		}
		XMLNode next = getNextSibling();
		XMLText t = new XMLText(doc, s);
		if (next == null)
			parent.appendChild(new XMLText(doc, s));
		else
			parent.insertBefore(t, next);
		return t;
	}

	@Override
	public boolean isElementContentWhitespace() {
		for (int i = 0; i < text.length(); ++i)
			if (!isXMLSpace(text.charAt(i)))
				return false;
		return true;
	}
	
	/** Return true is the given character is considered as a white space. */
	public static boolean isXMLSpace(char c) {
    	return (c <= 0x0020) &&
    			(((((1L << 0x0009)
    			| (1L << 0x000A)
    			| (1L << 0x000D)
    			| (1L << 0x0020)) >> c) & 1L) != 0);
	}

	@Override
	public String getWholeText() {
		if (parent == null) return text;
		XMLText first = this;
		while (first.getPreviousSibling() instanceof XMLText)
			first = (XMLText)first.getPreviousSibling();
		StringBuilder s = new StringBuilder();
		do {
			s.append(first.text);
			if (!(first.getNextSibling() instanceof XMLText))
				break;
			first = (XMLText)first.getNextSibling();
		} while (true);
		return s.toString();
	}

	@Override
	public XMLText replaceWholeText(String content) {
		setData(content);
		if (parent == null) return this;
		while (getPreviousSibling() instanceof XMLText)
			parent.removeChild(getPreviousSibling());
		while (getNextSibling() instanceof XMLText)
			parent.removeChild(getNextSibling());
		return this;
	}

}

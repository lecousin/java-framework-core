package net.lecousin.framework.xml.dom;

import javax.xml.soap.Node;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

public class XMLText extends XMLCharacterData implements Text {

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
	public XMLText splitText(int offset) throws DOMException {
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
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
				return true;
		}
		return false;
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
	public XMLText replaceWholeText(String content) throws DOMException {
		setData(content);
		if (parent == null) return this;
		while (getPreviousSibling() instanceof XMLText)
			parent.removeChild(getPreviousSibling());
		while (getNextSibling() instanceof XMLText)
			parent.removeChild(getNextSibling());
		return this;
	}

}
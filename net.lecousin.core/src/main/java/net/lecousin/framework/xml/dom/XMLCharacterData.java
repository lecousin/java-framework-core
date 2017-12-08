package net.lecousin.framework.xml.dom;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;

public abstract class XMLCharacterData extends XMLNode implements CharacterData {

	public XMLCharacterData(XMLDocument doc, String text) {
		super(doc);
		this.text = text;
	}
	
	protected String text;
	
	@Override
	public String getNodeValue() {
		return text;
	}
	
	@Override
	public void setNodeValue(String nodeValue) {
		text = nodeValue;
	}

	@Override
	public String getData() {
		return text;
	}

	@Override
	public void setData(String data) {
		text = data;
	}

	@Override
	public int getLength() {
		return text.length();
	}

	@Override
	public String substringData(int offset, int count) {
		return text.substring(offset, offset + count - 1);
	}

	@Override
	public void appendData(String arg) {
		text = text + arg;
	}

	@Override
	public void insertData(int offset, String arg) throws DOMException {
		StringBuilder s = new StringBuilder(text.length() + arg.length());
		s.append(text.substring(0, offset));
		s.append(arg);
		s.append(text.substring(offset));
		text = s.toString();
	}

	@Override
	public void deleteData(int offset, int count) throws DOMException {
		StringBuilder s = new StringBuilder(text.length() - count);
		if (offset > 0) s.append(text.substring(0, offset));
		if (text.length() > offset + count)
			s.append(text.substring(offset + count));
		text = s.toString();
	}

	@Override
	public void replaceData(int offset, int count, String arg) {
		deleteData(offset, count);
		insertData(offset, arg);
	}
	
	@Override
	public String getTextContent() {
		return getNodeValue();
	}
	
	@Override
	public void setTextContent(String textContent) {
		setData(textContent);
	}
	
}

package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;

public abstract class XMLStreamEventsSync extends XMLStreamEvents {

	/** Move forward to the next event. */
	public abstract void next() throws XMLException, IOException;
	
	/** Return the current line number and character number on the line. */
	public abstract Pair<Integer, Integer> getPosition();

	/** Shortcut to move forward to the next START_ELEMENT, return false if no START_ELEMENT event was found. */
	public boolean nextStartElement() throws XMLException, IOException {
		try {
			do {
				next();
			} while (!Type.START_ELEMENT.equals(event.type));
			return true;
		} catch (EOFException e) {
			return false;
		}
	}
	
	/** Go to the next inner element. Return true if one is found, false if the closing tag of the parent is found. */
	public boolean nextInnerElement(String parentName) throws XMLException, IOException {
		try {
			if (Type.START_ELEMENT.equals(event.type) && event.text.equals(parentName) && event.isClosed)
				return false; // we are one the opening tag of the parent, and it is closed, so it has no content
			do {
				next();
				if (Type.END_ELEMENT.equals(event.type)) {
					if (!event.text.equals(parentName))
						throw new XMLException(getPosition(), "Unexpected end element", event.text.asString());
					return false;
				}
				if (Type.START_ELEMENT.equals(event.type))
					return true;
			} while (true);
		} catch (EOFException e) {
			return false;
		}
	}
	
	/** Go to the next inner element having the given name. Return true if one is found, false if the closing tag of the parent is found. */
	public boolean nextInnerElement(String parentName, String childName) throws XMLException, IOException {
		try {
			do {
				next();
				if (Type.END_ELEMENT.equals(event.type)) {
					if (!event.text.equals(parentName))
						throw new XMLException(getPosition(), "Unexpected end element", event.text.asString());
					return false;
				}
				if (Type.START_ELEMENT.equals(event.type)) {
					if (!event.text.equals(childName)) {
						closeElement();
						continue;
					}
					return true;
				}
			} while (true);
		} catch (EOFException e) {
			return false;
		}
	}
	
	/** Read inner text and close element. */
	public UnprotectedStringBuffer readInnerText() throws XMLException, IOException {
		if (!Type.START_ELEMENT.equals(event.type))
			throw new IOException("Invalid call of readInnerText: it must be called on a start element");
		if (event.isClosed) return new UnprotectedStringBuffer();
		UnprotectedStringBuffer elementName = event.text;
		UnprotectedStringBuffer innerText = new UnprotectedStringBuffer();
		do {
			try { next(); }
			catch (EOFException e) {
				throw new XMLException(getPosition(), "Unexpected end", "readInnerText(" + elementName.toString() + ")");
			}
			if (Type.COMMENT.equals(event.type)) continue;
			if (Type.TEXT.equals(event.type)) {
				innerText.append(event.text);
				continue;
			}
			if (Type.START_ELEMENT.equals(event.type)) {
				closeElement();
				continue;
			}
			if (Type.END_ELEMENT.equals(event.type)) {
				if (!event.text.equals(elementName))
					throw new XMLException(getPosition(), "Unexpected end element", event.text.asString());
				return innerText;
			}
		} while (true);
	}
	
	/** Go the the END_ELEMENT event corresponding to the current START_ELEMENT (must be called with a current event to START_ELEMENT). */
	public void closeElement() throws XMLException, IOException {
		if (!Type.START_ELEMENT.equals(event.type))
			throw new IOException("Invalid call of closeElement: it must be called on a start element");
		if (event.isClosed) return;
		UnprotectedStringBuffer elementName = event.text;
		do {
			try { next(); }
			catch (EOFException e) {
				throw new XMLException(getPosition(), "Unexpected end", "closeElement(" + elementName.asString() + ")");
			}
			if (Type.END_ELEMENT.equals(event.type)) {
				if (!event.text.equals(elementName))
					throw new XMLException(getPosition(), "Unexpected end element expected is",
						event.text.asString(), elementName.asString());
				return;
			}
			if (Type.START_ELEMENT.equals(event.type))
				closeElement();
		} while (true);
	}

	/** Move forward until an element with the given name is found (whatever its depth).
	 * @return true if found, false if the end of file is reached.
	 */
	public boolean searchElement(String elementName) throws XMLException, IOException {
		try {
			do {
				if (Type.START_ELEMENT.equals(event.type)) {
					if (event.text.equals(elementName))
						return true;
				}
				next();
			} while (true);
		} catch (EOFException e) {
			return false;
		}
	}
	
	/** Go successively into the given elements. */
	public boolean goInto(String... innerElements) throws IOException, XMLException {
		for (int i = 0; i < innerElements.length; ++i)
			if (!goInto(innerElements[i]))
				return false;
		return true;
	}
	
	/** Go to the given inner element. */
	public boolean goInto(String innerElement) throws IOException, XMLException {
		String parentName = event.text.toString();
		return nextInnerElement(parentName, innerElement);
	}
	
	/** Read all inner elements with their text, and return a mapping with the element's name as key and inner text as value. */
	public Map<String,String> readInnerElementsText(String parentName) throws IOException, XMLException {
		Map<String, String> texts = new HashMap<>();
		while (nextInnerElement(parentName)) {
			String name = event.text.toString();
			texts.put(name, readInnerText().asString());
		}
		return texts;
	}
	
}

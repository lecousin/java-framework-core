package net.lecousin.framework.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Base class to parse XML raising an event each time a new element (comment, text, node... is found).<br/>
 * It is used by synchronous ({@link XMLStreamEventsSync}) and asynchronous (XMLStreamEventsAsync) implementations.<br/>
 * This class defines the different types of event, and the available information for each event.
 */
public abstract class XMLStreamEvents {

	public Event event = new Event();

	/** Parsing event. */
	public static class Event {
		
		/** Type of event. */
		public static enum Type {
			PROCESSING_INSTRUCTION,
			DOCTYPE,
			COMMENT,
			TEXT,
			START_ELEMENT,
			END_ELEMENT,
			CDATA
		}
		
		/** Type of current node. */
		public Type type = null;
		
		/** Text read, depending of the node type. <ul>
		 *    <li>CDATA: content of the CDATA tag</li>
		 *    <li>COMMENT: content of the comment tag</li>
		 *    <li>DOCTYPE: type of document</li>
		 *    <li>PROCESSING_INSTRUCTION: instruction name</li>
		 *    <li>START_ELEMENT: element name</li>
		 *    <li>TEXT: text</li>
		 *  </ul>
		 */
		public UnprotectedStringBuffer text = null;
		
		/** On START_ELEMENT or END_ELEMENT events, it contains the namespace part of the text. */
		public UnprotectedStringBuffer namespacePrefix = null;
		
		/** On START_ELEMENT or END_ELEMENT events, it contains the namespace URI correpsonding to the prefix. */
		public UnprotectedStringBuffer namespaceURI = null;
		
		/** On START_ELEMENT or END_ELEMENT events, it contains the local name part of the text. */
		public UnprotectedStringBuffer localName = null;
		
		/** When type if START_ELEMENT, it specifies if it is an empty-element. */
		public boolean isClosed = false;
		
		/** When type if START_ELEMENT, it contains the list of attributes. */
		public LinkedList<Attribute> attributes = null;
		
		/** System specified in the DOCTYPE tag. */
		public UnprotectedStringBuffer system = null;
		
		/** Public ID specified in the DOCTYPE tag. */
		public UnprotectedStringBuffer publicId = null;

		/** Current context with elements' names of the current hierarchy and namespaces declaration.
		 * A new context is created on each START_ELEMENT, and removed after the corresponding END_ELEMENT.
		 * If an element is closed on a START_ELEMENT, the context will be removed at the next event but
		 * present during the START_ELEMENT event.
		 * The context of an element is not removed on the END_ELEMENT event, but just after.
		 * The contexts are in reverse order so the first context corresponds to the current element,
		 * and the last context to the root element.
		 */
		public LinkedList<ElementContext> context = new LinkedList<>();
		
		/** Create a copy of this event. */
		public Event copy() {
			Event event = new Event();
			event.type = type;
			event.text = text;
			event.namespacePrefix = namespacePrefix;
			event.localName = localName;
			event.namespaceURI = namespaceURI;
			event.isClosed = isClosed;
			if (attributes != null)
				event.attributes = new LinkedList<>(attributes);
			event.system = system;
			event.publicId = publicId;
			event.context.addAll(context);
			return event;
		}
	}
	
	/** Information about an Element node. Those contexts are stacked to know the current hierarchy on an event. */
	public static class ElementContext {
		public UnprotectedStringBuffer text;
		public UnprotectedStringBuffer namespacePrefix;
		public UnprotectedStringBuffer localName;
		/** List of pair prefix/uri. */
		public List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> namespaces = new LinkedList<>();
	}
	
	/** An attribute on an element node. */
	public static class Attribute {
		public UnprotectedStringBuffer text;
		public UnprotectedStringBuffer namespacePrefix;
		public UnprotectedStringBuffer localName;
		public UnprotectedStringBuffer value;
	}
	
	/** Get the namespace URI for a prefix, or empty string if the prefix is not defined. */
	public UnprotectedStringBuffer getNamespaceURI(CharSequence namespacePrefix) {
		for (ElementContext ctx : event.context) {
			for (Pair<UnprotectedStringBuffer, UnprotectedStringBuffer> ns : ctx.namespaces)
				if (ns.getValue1().equals(namespacePrefix))
					return ns.getValue2();
		}
		return new UnprotectedStringBuffer();
	}
	
	/** Shortcut to get the attribute for the given full name. */
	public Attribute getAttributeByFullName(CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.text.equals(name))
				return attr;
		return null;
	}
	
	/** Shortcut to get the attibute for the given local name. */
	public Attribute getAttributeByLocalName(CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name))
				return attr;
		return null;
	}

	/** Shortcut to get the attribute for the given namespace prefix and local name. */
	public Attribute getAttributeWithPrefix(CharSequence prefix, CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name) && attr.namespacePrefix.equals(prefix))
				return attr;
		return null;
	}
	
	/** Shortcut to get the attribute for the given namespace URI and local name. */
	public Attribute getAttributeWithNamespaceURI(CharSequence uri, CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri))
				return attr;
		return null;
	}

	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueByFullName(CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.text.equals(name))
				return attr.value;
		return null;
	}
	
	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueByLocalName(CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name))
				return attr.value;
		return null;
	}

	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueWithPrefix(CharSequence prefix, CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name) && attr.namespacePrefix.equals(prefix))
				return attr.value;
		return null;
	}
	
	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueWithNamespaceURI(CharSequence uri, CharSequence name) {
		for (Attribute attr : event.attributes)
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri))
				return attr.value;
		return null;
	}
	
	/** Remove and return the attribute for the given full name if it exists. */
	public Attribute removeAttributeByFullName(CharSequence name) {
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute attr = it.next();
			if (attr.text.equals(name)) {
				it.remove();
				return attr;
			}
		}
		return null;
	}
	
	/** Remove and return the attibute for the given local name if it exists. */
	public Attribute removeAttributeByLocalName(CharSequence name) {
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute attr = it.next();
			if (attr.localName.equals(name)) {
				it.remove();
				return attr;
			}
		}
		return null;
	}

	/** Remove and return the attribute for the given namespace prefix and local name if it exists. */
	public Attribute removeAttributeWithPrefix(CharSequence prefix, CharSequence name) {
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute attr = it.next();
			if (attr.localName.equals(name) && attr.namespacePrefix.equals(prefix)) {
				it.remove();
				return attr;
			}
		}
		return null;
	}
	
	/** Remove and return the attribute for the given namespace URI and local name if it exists. */
	public Attribute removeAttributeWithNamespaceURI(CharSequence uri, CharSequence name) {
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute attr = it.next();
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri)) {
				it.remove();
				return attr;
			}
		}
		return null;
	}
	
	protected void reset() {
		if (Event.Type.START_ELEMENT.equals(event.type) && event.isClosed)
			event.context.removeFirst();
		else if (Event.Type.END_ELEMENT.equals(event.type))
			event.context.removeFirst();
		event.type = null;
		event.text = null;
		event.namespacePrefix = null;
		event.localName = null;
		event.isClosed = false;
		event.attributes = null;
		event.system = null;
		event.publicId = null;
	}
	
	
	protected void onStartElement() {
		int i = event.text.indexOf(':');
		if (i < 0) {
			event.namespacePrefix = new UnprotectedStringBuffer();
			event.localName = event.text;
		} else {
			event.namespacePrefix = event.text.substring(0, i);
			event.localName = event.text.substring(i + 1);
		}
		ElementContext ctx = new ElementContext();
		ctx.text = event.text;
		ctx.namespacePrefix = event.namespacePrefix;
		ctx.localName = event.localName;
		ctx.namespaces = readNamespaces();
		event.context.addFirst(ctx);
		event.namespaceURI = getNamespaceURI(event.namespacePrefix);
	}
	
	protected List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> readNamespaces() {
		List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> list = new LinkedList<>();
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (a.namespacePrefix.length() == 0) {
				if (a.localName.equals("xmlns")) {
					list.add(new Pair<>(a.namespacePrefix, a.value));
					it.remove();
					continue;
				}
			} else if (a.namespacePrefix.equals("xmlns")) {
				list.add(new Pair<>(a.localName, a.value));
				it.remove();
				continue;
			}
		}
		return list;
	}
	
}

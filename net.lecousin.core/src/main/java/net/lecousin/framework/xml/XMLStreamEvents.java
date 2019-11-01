package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStreamLocation;
import net.lecousin.framework.io.text.Decoder;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.io.text.ProgressiveBufferedReadableCharStream;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Base class to parse XML raising an event each time a new element (comment, text, node... is found).<br/>
 * It is used by synchronous ({@link XMLStreamEventsSync}) and asynchronous (XMLStreamEventsAsync) implementations.<br/>
 * This class defines the different types of event, and the available information for each event.
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public abstract class XMLStreamEvents {

	public Event event = new Event();
	
	protected int maxTextSize = -1;
	protected int maxCDataSize = -1;
	protected ICharacterStream.Readable.Buffered stream;
	
	public final ICharacterStream.Readable.Buffered getCharacterStream() {
		return stream;
	}

	/** Parsing event. */
	@SuppressWarnings("squid:S1319") // we want to use LinkedList, not just List
	public static class Event {
		
		/** Type of event. */
		public enum Type {
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
		public UnprotectedStringBuffer defaultNamespace = null;
		
		@Override
		public String toString() {
			return "XML Element Context: " + (localName != null ? localName.asString() : "null");
		}
	}
	
	/** An attribute on an element node. */
	public static class Attribute {
		public UnprotectedStringBuffer text;
		public UnprotectedStringBuffer namespacePrefix;
		public UnprotectedStringBuffer localName;
		public UnprotectedStringBuffer value;
	}
	
	public int getMaximumTextSize() { return maxTextSize; }
	
	public void setMaximumTextSize(int max) { maxTextSize = max; }
	
	public int getMaximumCDataSize() { return maxCDataSize; }
	
	public void setMaximumCDataSize(int max) { maxCDataSize = max; }
	
	/** Get the namespace URI for a prefix, or empty string if the prefix is not defined. */
	public UnprotectedStringBuffer getNamespaceURI(CharSequence namespacePrefix) {
		if (namespacePrefix.length() == 0) {
			for (ElementContext ctx : event.context)
				if (ctx.defaultNamespace != null)
					return ctx.defaultNamespace;
			return new UnprotectedStringBuffer();
		}
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
		if ((Event.Type.START_ELEMENT.equals(event.type) && event.isClosed) ||
			(Event.Type.END_ELEMENT.equals(event.type)))
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
	
	
	protected void onStartElement(int i) {
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
		readNamespaces(ctx);
		event.context.addFirst(ctx);
		event.namespaceURI = getNamespaceURI(event.namespacePrefix);
	}
	
	protected void readNamespaces(ElementContext ctx) {
		List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> list = new LinkedList<>();
		for (Iterator<Attribute> it = event.attributes.iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (a.namespacePrefix.isEmpty()) {
				if (a.localName.equals("xmlns")) {
					ctx.defaultNamespace = a.value;
					it.remove();
				}
			} else if (a.namespacePrefix.equals("xmlns")) {
				list.add(new Pair<>(a.localName, a.value));
				it.remove();
			}
		}
		ctx.namespaces = list;
	}

	/* Utility methods */
	
	private static byte[] charType = {
		(byte)99,	// 0x00
		(byte)99,	// 0x01
		(byte)99,	// 0x02
		(byte)99,	// 0x03
		(byte)99,	// 0x04
		(byte)99,	// 0x05
		(byte)99,	// 0x06
		(byte)99,	// 0x07
		(byte)99,	// 0x08
		(byte)2,	// 0x09
		(byte)2,	// 0x0a
		(byte)99,	// 0x0b
		(byte)99,	// 0x0c
		(byte)2,	// 0x0d
		(byte)99,	// 0x0e
		(byte)99,	// 0x0f
		(byte)99,	// 0x10
		(byte)99,	// 0x11
		(byte)99,	// 0x12
		(byte)99,	// 0x13
		(byte)99,	// 0x14
		(byte)99,	// 0x15
		(byte)99,	// 0x16
		(byte)99,	// 0x17
		(byte)99,	// 0x18
		(byte)99,	// 0x19
		(byte)99,	// 0x1a
		(byte)99,	// 0x1b
		(byte)99,	// 0x1c
		(byte)99,	// 0x1d
		(byte)99,	// 0x1e
		(byte)99,	// 0x1f
		(byte)2,	// 0x20
		(byte)99,	// 0x21
		(byte)99,	// 0x22
		(byte)99,	// 0x23
		(byte)99,	// 0x24
		(byte)99,	// 0x25
		(byte)99,	// 0x26
		(byte)99,	// 0x27
		(byte)99,	// 0x28
		(byte)99,	// 0x29
		(byte)99,	// 0x2a
		(byte)99,	// 0x2b
		(byte)99,	// 0x2c
		(byte)1,	// 0x2d
		(byte)1,	// 0x2e
		(byte)99,	// 0x2f
		(byte)1,	// 0x30
		(byte)1,	// 0x31
		(byte)1,	// 0x32
		(byte)1,	// 0x33
		(byte)1,	// 0x34
		(byte)1,	// 0x35
		(byte)1,	// 0x36
		(byte)1,	// 0x37
		(byte)1,	// 0x38
		(byte)1,	// 0x39
		(byte)0,	// 0x3a
		(byte)99,	// 0x3b
		(byte)99,	// 0x3c
		(byte)99,	// 0x3d
		(byte)99,	// 0x3e
		(byte)99,	// 0x3f
		(byte)99,	// 0x40
		(byte)0,	// 0x41
		(byte)0,	// 0x42
		(byte)0,	// 0x43
		(byte)0,	// 0x44
		(byte)0,	// 0x45
		(byte)0,	// 0x46
		(byte)0,	// 0x47
		(byte)0,	// 0x48
		(byte)0,	// 0x49
		(byte)0,	// 0x4a
		(byte)0,	// 0x4b
		(byte)0,	// 0x4c
		(byte)0,	// 0x4d
		(byte)0,	// 0x4e
		(byte)0,	// 0x4f
		(byte)0,	// 0x50
		(byte)0,	// 0x51
		(byte)0,	// 0x52
		(byte)0,	// 0x53
		(byte)0,	// 0x54
		(byte)0,	// 0x55
		(byte)0,	// 0x56
		(byte)0,	// 0x57
		(byte)0,	// 0x58
		(byte)0,	// 0x59
		(byte)0,	// 0x5a
		(byte)99,	// 0x5b
		(byte)99,	// 0x5c
		(byte)99,	// 0x5d
		(byte)99,	// 0x5e
		(byte)0,	// 0x5f
		(byte)99,	// 0x60
		(byte)0,	// 0x61
		(byte)0,	// 0x62
		(byte)0,	// 0x63
		(byte)0,	// 0x64
		(byte)0,	// 0x65
		(byte)0,	// 0x66
		(byte)0,	// 0x67
		(byte)0,	// 0x68
		(byte)0,	// 0x69
		(byte)0,	// 0x6a
		(byte)0,	// 0x6b
		(byte)0,	// 0x6c
		(byte)0,	// 0x6d
		(byte)0,	// 0x6e
		(byte)0,	// 0x6f
		(byte)0,	// 0x70
		(byte)0,	// 0x71
		(byte)0,	// 0x72
		(byte)0,	// 0x73
		(byte)0,	// 0x74
		(byte)0,	// 0x75
		(byte)0,	// 0x76
		(byte)0,	// 0x77
		(byte)0,	// 0x78
		(byte)0,	// 0x79
		(byte)0,	// 0x7a
		(byte)99,	// 0x7b
		(byte)99,	// 0x7c
		(byte)99,	// 0x7d
		(byte)99,	// 0x7e
		(byte)99,	// 0x7f
		(byte)99,	// 0x80
		(byte)99,	// 0x81
		(byte)99,	// 0x82
		(byte)99,	// 0x83
		(byte)99,	// 0x84
		(byte)99,	// 0x85
		(byte)99,	// 0x86
		(byte)99,	// 0x87
		(byte)99,	// 0x88
		(byte)99,	// 0x89
		(byte)99,	// 0x8a
		(byte)99,	// 0x8b
		(byte)99,	// 0x8c
		(byte)99,	// 0x8d
		(byte)99,	// 0x8e
		(byte)99,	// 0x8f
		(byte)99,	// 0x90
		(byte)99,	// 0x91
		(byte)99,	// 0x92
		(byte)99,	// 0x93
		(byte)99,	// 0x94
		(byte)99,	// 0x95
		(byte)99,	// 0x96
		(byte)99,	// 0x97
		(byte)99,	// 0x98
		(byte)99,	// 0x99
		(byte)99,	// 0x9a
		(byte)99,	// 0x9b
		(byte)99,	// 0x9c
		(byte)99,	// 0x9d
		(byte)99,	// 0x9e
		(byte)99,	// 0x9f
		(byte)99,	// 0xa0
		(byte)99,	// 0xa1
		(byte)99,	// 0xa2
		(byte)99,	// 0xa3
		(byte)99,	// 0xa4
		(byte)99,	// 0xa5
		(byte)99,	// 0xa6
		(byte)99,	// 0xa7
		(byte)99,	// 0xa8
		(byte)99,	// 0xa9
		(byte)99,	// 0xaa
		(byte)99,	// 0xab
		(byte)99,	// 0xac
		(byte)99,	// 0xad
		(byte)99,	// 0xae
		(byte)99,	// 0xaf
		(byte)99,	// 0xb0
		(byte)99,	// 0xb1
		(byte)99,	// 0xb2
		(byte)99,	// 0xb3
		(byte)99,	// 0xb4
		(byte)99,	// 0xb5
		(byte)99,	// 0xb6
		(byte)1,	// 0xb7
		(byte)99,	// 0xb8
		(byte)99,	// 0xb9
		(byte)99,	// 0xba
		(byte)99,	// 0xbb
		(byte)99,	// 0xbc
		(byte)99,	// 0xbd
		(byte)99,	// 0xbe
		(byte)99,	// 0xbf
		(byte)0,	// 0xc0
		(byte)0,	// 0xc1
		(byte)0,	// 0xc2
		(byte)0,	// 0xc3
		(byte)0,	// 0xc4
		(byte)0,	// 0xc5
		(byte)0,	// 0xc6
		(byte)0,	// 0xc7
		(byte)0,	// 0xc8
		(byte)0,	// 0xc9
		(byte)0,	// 0xca
		(byte)0,	// 0xcb
		(byte)0,	// 0xcc
		(byte)0,	// 0xcd
		(byte)0,	// 0xce
		(byte)0,	// 0xcf
		(byte)0,	// 0xd0
		(byte)0,	// 0xd1
		(byte)0,	// 0xd2
		(byte)0,	// 0xd3
		(byte)0,	// 0xd4
		(byte)0,	// 0xd5
		(byte)0,	// 0xd6
		(byte)99,	// 0xd7
		(byte)0,	// 0xd8
		(byte)0,	// 0xd9
		(byte)0,	// 0xda
		(byte)0,	// 0xdb
		(byte)0,	// 0xdc
		(byte)0,	// 0xdd
		(byte)0,	// 0xde
		(byte)0,	// 0xdf
		(byte)0,	// 0xe0
		(byte)0,	// 0xe1
		(byte)0,	// 0xe2
		(byte)0,	// 0xe3
		(byte)0,	// 0xe4
		(byte)0,	// 0xe5
		(byte)0,	// 0xe6
		(byte)0,	// 0xe7
		(byte)0,	// 0xe8
		(byte)0,	// 0xe9
		(byte)0,	// 0xea
		(byte)0,	// 0xeb
		(byte)0,	// 0xec
		(byte)0,	// 0xed
		(byte)0,	// 0xee
		(byte)0,	// 0xef
		(byte)0,	// 0xf0
		(byte)0,	// 0xf1
		(byte)0,	// 0xf2
		(byte)0,	// 0xf3
		(byte)0,	// 0xf4
		(byte)0,	// 0xf5
		(byte)0,	// 0xf6
		(byte)99,	// 0xf7
		(byte)0,	// 0xf8
		(byte)0,	// 0xf9
		(byte)0,	// 0xfa
		(byte)0,	// 0xfb
		(byte)0,	// 0xfc
		(byte)0,	// 0xfd
		(byte)0,	// 0xfe
		(byte)0	// 0xff
	};
	
	/** Return true if the given character is considered as a white space. */
	public static boolean isSpaceChar(char c) {
		return c < 0x100 && charType[c] == 2;
	}
	
	/** Return true if the given character is a valid starting character for a name. */
	public static boolean isNameStartChar(char c) {
		/*
		if (c < 0xF8) {
			if (c < 0x3A) return false; // :
			if (c < 0xC0) {
				return
					(c >= 'a' && c <= 'z') || // 61-7A
					(c >= 'A' && c <= 'Z') || // 41-5A
					c == ':' || // 3A
					c == '_' // 5F
					;
			}
			return (c != 0xD7 && c != 0xF7);
		}*/
		if (c < 0x100) return charType[c] == 0;
		if (c < 0x2000) {
			if (c >= 0x300 && c <= 0x36F) return false;
			if (c == 0x37E) return false;
			return true;
		}
		if (c < 0xD800) {
			if (c <= 0x3000) {
				if (c < 0x2070)
					return c != 0x200C && c != 0x200D;
				if (c < 0x2190) return true;
				if (c < 0x2C00) return false;
				if (c > 0x2FEF) return false;
			}
			return true;
		}
		if (c >= 0xE000) {
			if (c < 0xF900) return false;
			if (c <= 0xFDCF) return true;
			if (c < 0xFDF0) return false;
			if (c <= 0xFFFD) return true;
			return false;
		}
		if (c >= 0xDC00) return false;
		return true;
	}
	
	/** Return true if the given character is a valid character for a name. */
	public static boolean isNameChar(char c) {
		/*
		if (c < 0xF8) {
			if (c < 0x3A) {
				return c >= 0x2D && c != 0x2F;
			}
			if (c < 0xC0) {
				return
					(c >= 'a' && c <= 'z') || // 61-7A
					(c >= 'A' && c <= 'Z') || // 41-5A
					c == ':' || // 3A
					c == '_' || // 5F
					c == 0xB7;
			}
			return c != 0xD7 && c != 0xF7;
		}*/
		if (c < 0x100) return charType[c] <= 1;
		if (c < 0x2000) {
			return c != 0x37E;
		}
		if (c < 0xD800) {
			if (c <= 0x3000) {
				if (c < 0x2070)
					return c != 0x200C && c != 0x200D && c != 0x203F && c != 0x2040;
				if (c < 0x2190) return true;
				if (c < 0x2C00 || c > 0x2FEF) return false;
			}
			return true;
		}
		if (c >= 0xE000) {
			if (c < 0xF900) return false;
			if (c <= 0xFDCF) return true;
			if (c < 0xFDF0) return false;
			if (c <= 0xFFFD) return true;
			return false;
		}
		return true;
	}
	
	/* Charset handling */

	/** Helper class to start parsing the XML file and detect the encoding, either with a BOM or with the XML declaration tag. */
	protected static class Starter {
		public Starter(IO.Readable.Buffered io, Charset givenEncoding, int bufferSize, int maxBuffers, boolean addPositionInErrors) {
			this.io = io;
			this.givenEncoding = givenEncoding;
			this.bufferSize = bufferSize;
			this.maxBuffers = maxBuffers;
			this.addPositionInErrors = addPositionInErrors;
		}
		
		protected IO.Readable.Buffered io;
		protected int bufferSize;
		protected int maxBuffers;
		protected boolean addPositionInErrors;
		protected Charset givenEncoding;
		protected Charset bomEncoding;
		protected Decoder tmpDecoder;
		protected int line = 1;
		protected int posInLine = 1;
		protected UnprotectedStringBuffer xmlVersion = null;
		protected UnprotectedStringBuffer xmlEncoding = null;
		protected UnprotectedStringBuffer xmlStandalone = null;
		
		private byte[] firstBytes;
		private int firstBytesPos;
		private int firstBytesLength;
		
		public ICharacterStream.Readable.Buffered start() throws IOException, XMLException {
			loadFirstBytes();
			readBOM();
			int posAfterBOM = firstBytesPos;
			try {
				if (givenEncoding != null)
					tmpDecoder = Decoder.get(givenEncoding);
				else if (bomEncoding != null)
					tmpDecoder = Decoder.get(bomEncoding);
				else
					tmpDecoder = Decoder.get(StandardCharsets.UTF_8);
			} catch (Exception e) {
				throw new IOException("Error initializing character decoder", e);
			}
			chars = new char[bufferSize];
			// TODO line and posInLine...
			boolean hasDecl = readXMLDeclaration();
			if (!hasDecl) {
				ICharacterStream.Readable.Buffered stream = initWithoutXMLDeclaration(posAfterBOM);
				if (addPositionInErrors)
					stream = new BufferedReadableCharacterStreamLocation(stream, line, posInLine);
				return stream;
			}
			if (xmlEncoding != null) {
				Charset encoding = Charset.forName(xmlEncoding.asString());
				if (!encoding.equals(tmpDecoder.getEncoding())) {
					ICharacterStream.Readable.Buffered stream = initWithSpecifiedEncoding(posAfterBOM, encoding);
					if (addPositionInErrors)
						stream = new BufferedReadableCharacterStreamLocation(stream, line, posInLine);
					return stream;
				}
			}
			if (firstBytesPos < firstBytesLength)
				tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, firstBytesPos, firstBytesLength - firstBytesPos));
			tmpDecoder.setInput(io);
			ICharacterStream.Readable.Buffered stream = new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers,
				CharBuffer.wrap(chars, charsPos, charsLength - charsPos));
			if (addPositionInErrors)
				stream = new BufferedReadableCharacterStreamLocation(stream, line, posInLine);
			return stream;
		}
		
		private void loadFirstBytes() throws IOException {
			ByteBuffer buf = io.readNextBuffer();
			if (buf != null) {
				firstBytes = buf.array();
				firstBytesPos = buf.arrayOffset() + buf.position();
				firstBytesLength = firstBytesPos + buf.remaining();
				while (firstBytesLength < 4) {
					buf = io.readNextBuffer();
					if (buf == null) break;
					byte[] b = new byte[firstBytesLength + buf.remaining()];
					System.arraycopy(firstBytes, firstBytesPos, b, 0, firstBytesLength);
					System.arraycopy(buf.array(), buf.arrayOffset() + buf.position(), b, firstBytesLength, buf.remaining());
					firstBytes = b;
					firstBytesPos = 0;
					firstBytesLength += buf.remaining();
				}
			}
			//firstBytesLength = io.readFully(firstBytes);
		}
		
		private void readBOM() throws XMLException {
			if (firstBytesLength == 0) throw new XMLException(null, "File is empty");
			switch (firstBytes[0] & 0xFF) {
			case 0x00:
				readBom00();
				break;
			case 0xEF:
				readBomEF();
				break;
			case 0xFE:
				readBomFE();
				break;
			case 0xFF:
				readBomFF();
				break;
			default: break;
			}
		}
		
		private void readBomEF() throws XMLException {
			// it may be a UTF-8 BOM
			if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
			if (firstBytes[1] == (byte)0xBB) {
				if (firstBytesLength == 2) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
				if (firstBytes[2] == (byte)0xBF) {
					// UTF-8 BOM
					bomEncoding = StandardCharsets.UTF_8;
					firstBytesPos = 3;
				}
			}
		}
		
		private void readBomFE() throws XMLException {
			// it may be a UTF-16 big-endian BOM
			if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
			if (firstBytes[1] == (byte)0xFF) {
				// UTF-16 big-endian
				bomEncoding = StandardCharsets.UTF_16BE;
				firstBytesPos = 2;
			}
		}
		
		private void readBomFF() throws XMLException {
			// it may be a BOM for UTF-16 little-endian or UTF-32 little-endian
			readBom16or32((byte)0xFE, StandardCharsets.UTF_16LE, (byte)0x00, (byte)0x00, Charset.forName("UTF-32LE"));
		}
		
		private void readBom00() throws XMLException {
			// it may be a UTF-32 big-endian BOM, but it may also be UTF-16 without BOM
			readBom16or32((byte)0x00, StandardCharsets.UTF_16BE, (byte)0xFE, (byte)0xFF, Charset.forName("UTF-32BE"));
		}
		
		private void readBom16or32(byte second, Charset secondCharset, byte third, byte fourth, Charset fourthCharset) throws XMLException {
			if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
			if (firstBytes[1] == second) {
				if (firstBytesLength == 2) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
				if (firstBytes[2] == third) {
					if (firstBytesLength == 3) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
					if (firstBytes[3] == fourth) {
						bomEncoding = fourthCharset;
						firstBytesPos = 4;
					}
					return;
				}
				bomEncoding = secondCharset;
				firstBytesPos = 2;
			}
		}
		
		private ICharacterStream.Readable.Buffered initWithoutXMLDeclaration(int posAfterBOM) throws IOException {
			// simple
			try { tmpDecoder = Decoder.get(tmpDecoder.getEncoding()); }
			catch (Exception e) {
				throw new IOException("Error initializing character decoder", e);
			}
			tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, posAfterBOM, firstBytesLength - posAfterBOM));
			tmpDecoder.setInput(io);
			return new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers);
		}
		
		private ICharacterStream.Readable.Buffered initWithSpecifiedEncoding(int posAfterBOM, Charset encoding) throws IOException {
			try { tmpDecoder = Decoder.get(encoding); }
			catch (Exception e) {
				throw new IOException("Error initializing character decoder", e);
			}
			tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, posAfterBOM, firstBytesLength - posAfterBOM));
			tmpDecoder.setInput(io);
			ICharacterStream.Readable.Buffered stream = new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers);
			// we reset the decoder, so we need to skip the <?xml ... ?>
			char c;
			boolean end = false;
			do {
				c = stream.read();
				// TODO string
				while (c == '?') {
					c = stream.read();
					if (c == '>') {
						end = true;
						break;
					}
				}
			} while (!end);
			return stream;
		}
		
		private char[] chars;
		private int charsPos = 0;
		private int charsLength = 0;

		private char nextChar() throws IOException {
			if (charsPos < charsLength)
				return chars[charsPos++];
			if (firstBytesPos > 0 && firstBytesLength - firstBytesPos < 10) {
				byte[] b = new byte[firstBytes.length > 256 ? firstBytes.length * 2 : 512];
				System.arraycopy(firstBytes, 0, b, 0, firstBytes.length);
				int nb = io.readFullySync(ByteBuffer.wrap(b, firstBytesLength, b.length - firstBytesLength));
				if (nb <= 0) throw new EOFException();
				firstBytes = b;
				firstBytesLength += nb;
			}
			charsPos = 0;
			ByteBuffer bb = ByteBuffer.wrap(firstBytes, firstBytesPos, firstBytesLength - firstBytesPos);
			charsLength = tmpDecoder.decode(bb, chars, 0, chars.length);
			if (charsLength <= 0)
				throw new EOFException();
			firstBytesPos = bb.position();
			return chars[charsPos++];
		}
		
		@SuppressWarnings("squid:S3776") // complexity
		private boolean readXMLDeclaration() throws IOException {
			char c;
			while (isSpaceChar(c = nextChar()));
			if (c != '<')
				return false;
			if (charsPos + 5 <= charsLength) {
				if ((chars[charsPos++] != '?') ||
					((c = chars[charsPos++]) != 'x' && c != 'X') ||
					((c = chars[charsPos++]) != 'm' && c != 'M') ||
					((c = chars[charsPos++]) != 'l' && c != 'L') ||
					(!isSpaceChar(chars[charsPos++])))
					return false;
			} else {
				if ((nextChar() != '?') ||
					((c = nextChar()) != 'x' && c != 'X') ||
					((c = nextChar()) != 'm' && c != 'M') ||
					((c = nextChar()) != 'l' && c != 'L') ||
					(!isSpaceChar(nextChar())))
					return false;
			}
			
			while (isSpaceChar(c = nextChar()));
			if (c != 'v' && c != 'V') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			xmlVersion = eat(
				new char[] { 'e', 'r', 's', 'i', 'o', 'n' },
				new char[] { 'E', 'R', 'S', 'I', 'O', 'N' });
			if (xmlVersion == null)
				throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			
			while (isSpaceChar(c = nextChar()));
			if (c == 'e' || c == 'E') {
				xmlEncoding = eat(
					new char[] { 'n', 'c', 'o', 'd', 'i', 'n', 'g' },
					new char[] { 'N', 'C', 'O', 'D', 'I', 'N', 'G' });
				if (xmlEncoding == null)
					throw new IOException("Invalid XML Declaration: unknown attribute");
				while (isSpaceChar(c = nextChar()));
			}
			if (c == 's' || c == 'S') {
				xmlStandalone = eat(
					new char[] { 't', 'a', 'n', 'd', 'a', 'l', 'o', 'n', 'e' },
					new char[] { 'T', 'A', 'N', 'D', 'A', 'L', 'O', 'N', 'E' });
				if (xmlStandalone == null)
					throw new IOException("Invalid XML Declaration: unknown attribute");
				while (isSpaceChar(c = nextChar()));
			}
			
			if (c != '?') throw new IOException("Unexpected character '" + c + "' in XML declaration tag");
			if (nextChar() != '>') throw new IOException("Unexpected character '" + c + "' in XML declaration tag");
			return true;
		}
		
		private UnprotectedStringBuffer eat(char[] lowerCase, char[] upperCase) throws IOException {
			int l = lowerCase.length;
			char c;
			if (charsPos + l <= charsLength) {
				for (int i = 0; i < l; ++i)
					if ((c = chars[charsPos++]) != lowerCase[i] && c != upperCase[i])
						return null;
			} else {
				for (int i = 0; i < l; ++i)
					if ((c = nextChar()) != lowerCase[i] && c != upperCase[i])
						return null;
			}
			while (isSpaceChar(c = nextChar()));
			if (c != '=')
				throw new IOException("Invalid XML Declaration: character '=' expected after attribute name");
			while (isSpaceChar(c = nextChar()));
			if (c != '\'' && c != '"')
				throw new IOException("Invalid XML Declaration: character ' or \" expected after '='");
			char c2 = c;
			UnprotectedStringBuffer value = new UnprotectedStringBuffer();
			while ((c = nextChar()) != c2)
				value.append(c);
			return value;
		}
		
	}

}

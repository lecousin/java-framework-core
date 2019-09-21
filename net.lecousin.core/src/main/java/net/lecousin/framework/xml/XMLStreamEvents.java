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

	/** Parsing event. */
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
				}
			} else if (a.namespacePrefix.equals("xmlns")) {
				list.add(new Pair<>(a.localName, a.value));
				it.remove();
			}
		}
		return list;
	}

	/* Utility methods */
	
	private static boolean[] isSpace = new boolean[] { true, true, false, false, true };
	
	/** Return true if the given character is considered as a white space. */
	public static boolean isSpaceChar(char c) {
		if (c == 0x20) return true;
		if (c > 0xD) return false;
		if (c < 0x9) return false;
		return isSpace[c - 9];
	}
	
	/* Charset handling */

	/** Helper class to start parsing the XML file and detect the encoding, either with a BOM or with the XML declaration tag. */
	protected static class Starter {
		public Starter(IO.Readable.Buffered io, Charset givenEncoding, int bufferSize, int maxBuffers) {
			this.io = io;
			this.givenEncoding = givenEncoding;
			this.bufferSize = bufferSize;
			this.maxBuffers = maxBuffers;
		}
		
		protected IO.Readable.Buffered io;
		protected int bufferSize;
		protected int maxBuffers;
		protected Charset givenEncoding;
		protected Charset bomEncoding;
		protected Decoder tmpDecoder;
		protected int line = 1;
		protected int posInLine = 1;
		protected UnprotectedStringBuffer xmlVersion = null;
		protected UnprotectedStringBuffer xmlEncoding = null;
		protected UnprotectedStringBuffer xmlStandalone = null;
		
		private byte[] firstBytes = new byte[1024];
		private int firstBytesPos = 0;
		private int firstBytesLength = 0;
		
		public BufferedReadableCharacterStreamLocation start() throws IOException, XMLException {
			firstBytesLength = io.readFully(firstBytes);
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
			// TODO line and posInLine...
			boolean hasDecl = readXMLDeclaration();
			if (!hasDecl) {
				// simple
				try { tmpDecoder = Decoder.get(tmpDecoder.getEncoding()); }
				catch (Exception e) {
					throw new IOException("Error initializing character decoder", e);
				}
				tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, posAfterBOM, firstBytesLength - posAfterBOM));
				tmpDecoder.setInput(io);
				return new BufferedReadableCharacterStreamLocation(
					new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers), line, posInLine);
			}
			if (xmlEncoding != null) {
				Charset encoding = Charset.forName(xmlEncoding.asString());
				if (!encoding.equals(tmpDecoder.getEncoding())) {
					try { tmpDecoder = Decoder.get(encoding); }
					catch (Exception e) {
						throw new IOException("Error initializing character decoder", e);
					}
					tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, posAfterBOM, firstBytesLength - posAfterBOM));
					tmpDecoder.setInput(io);
					BufferedReadableCharacterStreamLocation stream = new BufferedReadableCharacterStreamLocation(
						new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers), line, posInLine);
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
			}
			tmpDecoder.setFirstBytes(ByteBuffer.wrap(firstBytes, firstBytesPos, firstBytesLength - firstBytesPos));
			tmpDecoder.setInput(io);
			return new BufferedReadableCharacterStreamLocation(
				new ProgressiveBufferedReadableCharStream(tmpDecoder, bufferSize, maxBuffers,
						CharBuffer.wrap(chars, charsPos, charsLength - charsPos)), line, posInLine);
		}
		
		public void readBOM() throws XMLException {
			if (firstBytesLength == 0) throw new XMLException(null, "File is empty");
			switch (firstBytes[0] & 0xFF) {
			case 0xEF: {
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
				break;
			}
			case 0xFE: {
				// it may be a UTF-16 big-endian BOM
				if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
				if (firstBytes[1] == (byte)0xFF) {
					// UTF-16 big-endian
					bomEncoding = StandardCharsets.UTF_16BE;
					firstBytesPos = 2;
				}
				break;
			}
			case 0xFF: {
				// it may be a BOM for UTF-16 little-endian or UTF-32 little-endian
				if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
				if (firstBytes[1] == (byte)0xFE) {
					if (firstBytesLength == 2) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
					if (firstBytes[2] == (byte)0x00) {
						if (firstBytesLength == 3) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
						if (firstBytes[3] == (byte)0x00) {
							// UTF-32 little-endian
							bomEncoding = Charset.forName("UTF-32LE");
							firstBytesPos = 4;
						}
						break;
					}
					// UTF-16 little-endian
					bomEncoding = StandardCharsets.UTF_16LE;
					firstBytesPos = 2;
				}
				break;
			}
			case 0x00: {
				// it may be a UTF-32 big-endian BOM, but it may also be UTF-16 without BOM
				if (firstBytesLength == 1) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
				if (firstBytes[1] == (byte)0x00) {
					if (firstBytesLength == 2) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
					if (firstBytes[2] == (byte)0xFE) {
						if (firstBytesLength == 3) throw new XMLException(null, XMLException.LOCALIZED_MESSAGE_NOT_XML);
						if (firstBytes[3] == (byte)0xFF) {
							// UTF-32 big-endian
							bomEncoding = Charset.forName("UTF-32BE");
							firstBytesPos = 4;
						}
						break;
					}
					// UTF-16 without BOM
					bomEncoding = StandardCharsets.UTF_16BE;
					firstBytesPos = 2;
				}
				break;
			}
			default:
				break;
			}
		}
		
		private char[] chars = new char[64];
		private int charsPos = 0;
		private int charsLength = 0;

		private char nextChar() throws IOException {
			if (charsPos < charsLength)
				return chars[charsPos++];
			if (firstBytesPos > 0 && firstBytesLength - firstBytesPos < 10) {
				// TODO in fact we must enlarge firstBytes...
				if (firstBytesPos < firstBytesLength) {
					int len = firstBytesLength - firstBytesPos;
					System.arraycopy(firstBytes, firstBytesPos, firstBytes, 0, len);
					int nb = io.readFullySync(ByteBuffer.wrap(firstBytes, len, firstBytes.length - len));
					if (nb == -1) nb = 0;
					firstBytesPos = 0;
					firstBytesLength = len + nb;
					// TODO if end reached
				} else {
					firstBytesPos = 0;
					firstBytesLength = io.readFullySync(ByteBuffer.wrap(firstBytes));
					// TODO if end reached
				}
			}
			charsPos = 0;
			ByteBuffer bb = ByteBuffer.wrap(firstBytes, firstBytesPos, firstBytesLength - firstBytesPos);
			charsLength = tmpDecoder.decode(bb, chars, 0, chars.length);
			if (charsLength <= 0)
				throw new EOFException();
			firstBytesPos = bb.position();
			return chars[charsPos++];
		}
		
		protected boolean readXMLDeclaration() throws IOException {
			char c;
			while (isSpaceChar(c = nextChar()));
			if (c != '<')
				return false;
			c = nextChar();
			if (c != '?')
				return false;
			c = nextChar();
			if (c != 'x' && c != 'X')
				return false;
			char c2 = nextChar();
			if (c2 != 'm' && c2 != 'M')
				return false;
			char c3 = nextChar();
			if (c3 != 'l' && c3 != 'L')
				return false;
			char c4 = nextChar();
			if (!isSpaceChar(c4))
				return false;
			while (isSpaceChar(c = nextChar()));
			if (c != 'v' && c != 'V') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 'e' && c != 'E') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 'r' && c != 'R') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 's' && c != 'S') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 'i' && c != 'I') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 'o' && c != 'O') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			c = nextChar();
			if (c != 'n' && c != 'N') throw new IOException("Invalid XML Declaration: first attribute must be 'version'");
			while (isSpaceChar(c = nextChar()));
			if (c != '=') throw new IOException("Invalid XML Declaration: character '=' expected after attribute name 'version'");
			while (isSpaceChar(c = nextChar()));
			if (c != '\'' && c != '"') throw new IOException("Invalid XML Declaration: character ' or \" expected after 'version='");
			c2 = c;
			xmlVersion = new UnprotectedStringBuffer();
			while ((c = nextChar()) != c2)
				xmlVersion.append(c);
			while (isSpaceChar(c = nextChar()));
			if (c == 'e' || c == 'E') {
				c = nextChar();
				if (c != 'n' && c != 'N')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'e" + c + "'");
				c = nextChar();
				if (c != 'c' && c != 'C')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'en" + c + "'");
				c = nextChar();
				if (c != 'o' && c != 'O')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'enc" + c + "'");
				c = nextChar();
				if (c != 'd' && c != 'D')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'enco" + c + "'");
				c = nextChar();
				if (c != 'i' && c != 'I')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'encod" + c + "'");
				c = nextChar();
				if (c != 'n' && c != 'N')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'encodi" + c + "'");
				c = nextChar();
				if (c != 'g' && c != 'G')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'encodin" + c + "'");
				while (isSpaceChar(c = nextChar()));
				if (c != '=')
					throw new IOException("Invalid XML Declaration: character '=' expected after attribute name 'encoding'");
				while (isSpaceChar(c = nextChar()));
				if (c != '\'' && c != '"')
					throw new IOException("Invalid XML Declaration: character ' or \" expected after 'encoding='");
				c2 = c;
				xmlEncoding = new UnprotectedStringBuffer();
				while ((c = nextChar()) != c2)
					xmlEncoding.append(c);
				while (isSpaceChar(c = nextChar()));
			}
			if (c == 's' || c == 'S') {
				c = nextChar();
				if (c != 't' && c != 'T')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 's" + c + "'");
				c = nextChar();
				if (c != 'a' && c != 'A')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'st" + c + "'");
				c = nextChar();
				if (c != 'n' && c != 'N')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'sta" + c + "'");
				c = nextChar();
				if (c != 'd' && c != 'D')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'stan" + c + "'");
				c = nextChar();
				if (c != 'a' && c != 'A')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'stand" + c + "'");
				c = nextChar();
				if (c != 'l' && c != 'L')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'standa" + c + "'");
				c = nextChar();
				if (c != 'o' && c != 'O')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'standal" + c + "'");
				c = nextChar();
				if (c != 'n' && c != 'N')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'standalo" + c + "'");
				c = nextChar();
				if (c != 'e' && c != 'E')
					throw new IOException("Invalid XML Declaration: unknown attribute starting with 'standalon" + c + "'");
				while (isSpaceChar(c = nextChar()));
				if (c != '=')
					throw new IOException("Invalid XML Declaration: character '=' expected after attribute name 'standalone'");
				while (isSpaceChar(c = nextChar()));
				if (c != '\'' && c != '"')
					throw new IOException("Invalid XML Declaration: character ' or \" expected after 'standalone='");
				c2 = c;
				xmlStandalone = new UnprotectedStringBuffer();
				while ((c = nextChar()) != c2)
					xmlStandalone.append(c);
				while (isSpaceChar(c = nextChar()));
			}
			if (c != '?') throw new IOException("Unexpected character '" + c + "' in XML declaration tag");
			if (nextChar() != '>') throw new IOException("Unexpected character '" + c + "' in XML declaration tag");
			return true;
		}
		
	}

}

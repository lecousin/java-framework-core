package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.encoding.DecimalNumber;
import net.lecousin.framework.io.encoding.HexadecimalNumber;
import net.lecousin.framework.io.encoding.INumberEncoding;
import net.lecousin.framework.io.text.BufferedReadableCharacterStreamLocation;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;

/**
 * Read an XML in a similar way as {@link javax.xml.stream.XMLStreamReader}: read-only, forward, event based.
 * The method next() allows to move forward to the next event (such as start element, end element, comment, text...).
 * It uses {@link UnprotectedString} to avoid allocating many character arrays.
 * Charset is automatically detected by reading the beginning of the XML (either with auto-detection or with the specified encoding).
 */
public class XMLStreamReader extends XMLStreamEventsSync {

	/** Initialize with the given IO.
	 * If it is not buffered, a {@link PreBufferedReadable} is created.
	 * If the charset is null, it will be automatically detected.
	 */
	public XMLStreamReader(IO.Readable io, Charset defaultEncoding, int charactersBuffersSize) {
		if (io instanceof IO.Readable.Buffered)
			this.io = (IO.Readable.Buffered)io;
		else
			this.io = new PreBufferedReadable(io, 1024, io.getPriority(), charactersBuffersSize, (byte)(io.getPriority() - 1), 4);
		this.defaultEncoding = defaultEncoding;
		this.charactersBuffersSize = charactersBuffersSize;
	}
	
	/** Constructor, without charset. */
	public XMLStreamReader(IO.Readable io, int charactersBuffersSize) {
		this(io, null, charactersBuffersSize);
	}
	
	private Charset defaultEncoding;
	private int charactersBuffersSize;
	private IO.Readable.Buffered io;
	private BufferedReadableCharacterStreamLocation stream;
	
	/* Public methods */
	
	/** Utility method that initialize a XMLStreamReader, initialize it, and
	 * return an AsyncWork which is unblocked when characters are available to be read.
	 */
	public static AsyncWork<XMLStreamReader, Exception> start(IO.Readable.Buffered io, int charactersBufferSize) {
		AsyncWork<XMLStreamReader, Exception> result = new AsyncWork<>();
		new Task.Cpu.FromRunnable("Start reading XML " + io.getSourceDescription(), io.getPriority(), () -> {
			XMLStreamReader reader = new XMLStreamReader(io, charactersBufferSize);
			try {
				Starter start = new Starter(io, reader.defaultEncoding, reader.charactersBuffersSize);
				reader.stream = start.start();
				reader.stream.canStartReading().listenAsync(
				new Task.Cpu.FromRunnable("Start reading XML " + io.getSourceDescription(), io.getPriority(), () -> {
					try {
						reader.next();
						result.unblockSuccess(reader);
					} catch (Exception e) {
						result.unblockError(e);
					}
				}), true);
			} catch (Exception e) {
				result.unblockError(e);
			}
		}).startOn(io.canStartReading(), true);
		return result;
	}

	@Override
	public void start() throws XMLException, IOException {
		Starter start = new Starter(io, defaultEncoding, charactersBuffersSize);
		stream = start.start();
		next();
	}
	
	/** Move forward to the next event. */
	@Override
	public void next() throws XMLException, IOException {
		reset();
		char c = stream.read();
		if (c == '<')
			readTag();
		else
			readChars(c);
	}
	
	@Override
	public Pair<Integer, Integer> getPosition() {
		return new Pair<>(Integer.valueOf(stream.getLine()), Integer.valueOf(stream.getPositionInLine()));
	}
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public void startRootElement() throws XMLException, IOException {
		start();
		while (!Type.START_ELEMENT.equals(event.type)) next();
	}
	
	
	/* Private methods */
	
	private void readTag() throws XMLException, IOException {
		try {
			char c = stream.read();
			if (c == '!') {
				readTagExclamation();
			} else if (c == '?') {
				readProcessingInstruction();
			} else if (c == '/') {
				readEndTag();
			} else if (isNameStartChar(c)) {
				readStartTag(c);
			} else {
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in XML document"));
		}
	}
	
	private static char[] CDATA = new char[] { 'C','D','A','T','A','[' };
	private static char[] OCTYPE = new char[] { 'O','C','T','Y','P','E' };
	
	private void readTagExclamation() throws XMLException, IOException {
		// can be a comment, a CDATA, or a DOCTYPE
		char c = stream.read();
		if (c == '-') {
			// comment ?
			c = stream.read();
			if (c == '-')
				readComment();
			else
				throw new XMLException(getPosition(), "Invalid XML");
		} else if (c == '[') {
			// CDATA ?
			if (!readExpectedChars(CDATA))
				throw new XMLException(getPosition(), "Invalid XML");
			readCData();
		} else if (c == 'D') {
			// DOCTYPE ?
			if (!readExpectedChars(OCTYPE))
				throw new XMLException(getPosition(), "Invalid XML");
			readDocType();
		} else
			throw new XMLException(getPosition(), "Invalid XML");
	}
	
	private static char[] YSTEM = new char[] { 'Y', 'S', 'T', 'E', 'M' };
	private static char[] UBLIC = new char[] { 'U', 'B', 'L', 'I', 'C' };
	
	private void readDocType() throws XMLException, IOException {
		event.type = Type.DOCTYPE;
		event.text = new UnprotectedStringBuffer();
		readSpace();
		readName(event.text);
		char c;
		while (isSpaceChar(c = stream.read()));
		if (c == 'S') {
			if (!readExpectedChars(YSTEM))
				throw new XMLException(getPosition(), "Invalid XML");
			readSpace();
			event.system = readSystemLiteral();
			c = stream.read();
			while (isSpaceChar(c)) c = stream.read();
		} else if (c == 'P') {
			if (!readExpectedChars(UBLIC))
				throw new XMLException(getPosition(), "Invalid XML");
			readSpace();
			event.publicId = readPublicIDLiteral();
			readSpace();
			event.system = readSystemLiteral();
			c = stream.read();
			while (isSpaceChar(c)) c = stream.read();
		}
		if (c == '[') {
			readIntSubset();
			c = stream.read();
			while (isSpaceChar(c)) c = stream.read();
		}
		if (c != '>')
			throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
	}
	
	private void readComment() throws XMLException, IOException {
		event.type = Type.COMMENT;
		event.text = new UnprotectedStringBuffer();
		do {
			char c;
			try { c = stream.read(); }
			catch (EOFException e) {
				throw new XMLException(getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "inside comment"));
			}
			if (c == '-') {
				try { c = stream.read(); }
				catch (EOFException e) {
					throw new XMLException(getPosition(),
						new LocalizableString("lc.xml.error", "Unexpected end"),
						new LocalizableString("lc.xml.error", "inside comment"));
				}
				if (c != '-') {
					if (maxTextSize <= 0 || event.text.length() < maxTextSize) {
						event.text.append('-');
						event.text.append(c);
					}
					continue;
				}
				do {
					try { c = stream.read(); }
					catch (EOFException e) {
						throw new XMLException(getPosition(),
							new LocalizableString("lc.xml.error", "Unexpected end"),
							new LocalizableString("lc.xml.error", "inside comment"));
					}
					if (c == '>')
						return;
					if (c == '-') {
						if (maxTextSize <= 0 || event.text.length() < maxTextSize)
							event.text.append('-');
						continue;
					}
					if (maxTextSize <= 0 || event.text.length() < maxTextSize) {
						event.text.append('-');
						event.text.append('-');
					}
					break;
				} while (true);
				continue;
			}
			if (maxTextSize <= 0 || event.text.length() < maxTextSize)
				event.text.append(c);
		} while (true);
	}
	
	private boolean readExpectedChars(char[] expected) throws IOException {
		for (int i = 0; i < expected.length; ++i)
			if (expected[i] != stream.read())
				return false;
		return true;
	}
	
	private void readCData() throws XMLException, IOException {
		event.type = Type.CDATA;
		event.text = new UnprotectedStringBuffer();
		do {
			char c;
			try { c = stream.read(); }
			catch (EOFException e) {
				throw new XMLException(getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "inside CDATA"));
			}
			if (c == ']') {
				try { c = stream.read(); }
				catch (EOFException e) {
					throw new XMLException(getPosition(),
						new LocalizableString("lc.xml.error", "Unexpected end"),
						new LocalizableString("lc.xml.error", "inside CDATA"));
				}
				if (c != ']') {
					if (maxCDataSize <= 0 || event.text.length() < maxCDataSize) {
						event.text.append(']');
						event.text.append(c);
					}
					continue;
				}
				do {
					try { c = stream.read(); }
					catch (EOFException e) {
						throw new XMLException(getPosition(),
							new LocalizableString("lc.xml.error", "Unexpected end"),
							new LocalizableString("lc.xml.error", "inside CDATA"));
					}
					if (c == '>')
						return;
					if (c == ']') {
						if (maxCDataSize <= 0 || event.text.length() < maxCDataSize)
							event.text.append(']');
						continue;
					}
					if (maxCDataSize <= 0 || event.text.length() < maxCDataSize) {
						event.text.append(']');
						event.text.append(']');
						event.text.append(c);
					}
					break;
				} while (true);
				continue;
			}
			if (maxCDataSize <= 0 || event.text.length() < maxCDataSize)
				event.text.append(c);
		} while (true);
	}
	
	private void readStartTag(char c) throws XMLException, IOException {
		event.type = Type.START_ELEMENT;
		event.attributes = new LinkedList<>();
		event.text = new UnprotectedStringBuffer();
		event.text.append(c);
		continueReadName(event.text);
		do {
			while (isSpaceChar(c = stream.read()));
			if (c == '>') break;
			if (c == '/') {
				if (stream.read() != '>')
					throw new XMLException(getPosition(), "Invalid XML");
				event.isClosed = true;
				break;
			}
			if (!isNameStartChar(c))
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			Attribute a = new Attribute();
			
			// attribute name
			a.text = new UnprotectedStringBuffer();
			a.text.append(c);
			continueReadName(a.text);
			int i = a.text.indexOf(':');
			if (i < 0) {
				a.namespacePrefix = new UnprotectedStringBuffer();
				a.localName = a.text;
			} else {
				a.namespacePrefix = a.text.substring(0, i);
				a.localName = a.text.substring(i + 1);
			}
			
			// equal
			while (isSpaceChar(c = stream.read()));
			if (c != '=')
				throw new XMLException(getPosition(), "Expected character", Character.valueOf(c), Character.valueOf('='));
			
			// attribute value
			while (isSpaceChar(c = stream.read()));
			a.value = new UnprotectedStringBuffer();
			if (c == '"' || c == '\'') readAttrValue(a.value, c);
			else throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			event.attributes.add(a);
		} while (true);
		onStartElement();
	}
	
	private void readProcessingInstruction() throws XMLException, IOException {
		event.type = Type.PROCESSING_INSTRUCTION;
		event.attributes = new LinkedList<>();
		event.text = new UnprotectedStringBuffer();
		readName(event.text);
		char c;
		do {
			while (isSpaceChar(c = stream.read()));
			if (c == '?') {
				c = stream.read();
				if (c == '>') {
					event.isClosed = true;
					return;
				}
				stream.back(c);
				continue;
			}
			if (!isNameStartChar(c))
				continue;
			Attribute a = new Attribute();
			// attribute name
			a.text = new UnprotectedStringBuffer();
			a.text.append(c);
			continueReadName(a.text);
			int i = a.text.indexOf(':');
			if (i < 0) {
				a.namespacePrefix = new UnprotectedStringBuffer();
				a.localName = a.text;
			} else {
				a.namespacePrefix = a.text.substring(0, i);
				a.localName = a.text.substring(i + 1);
			}

			// equal
			while (isSpaceChar(c = stream.read()));
			if (c != '=') {
				// empty attribute
				event.attributes.add(a);
				stream.back(c);
				continue;
			}
			// attribute value
			while (isSpaceChar(c = stream.read()));
			a.value = new UnprotectedStringBuffer();
			if (c == '"' || c == '\'') readAttrValue(a.value, c);
			else throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			event.attributes.add(a);
		} while (true);
	}
	
	private void readAttrValue(UnprotectedStringBuffer value, char quote) throws XMLException, IOException {
		do {
			char c = stream.read();
			if (c == quote) break;
			if (c == '<') throw new XMLException(getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected character", Character.valueOf(c)),
				new LocalizableString("lc.xml.error", "in attribute value"));
			if (c == '&') value.append(readReference());
			else if (c == '\n') value.append(' ');
			else if (c == '\r') continue;
			else value.append(c);
		} while (true);
	}
	
	private void readChars(char c) throws XMLException, IOException {
		event.type = Type.TEXT;
		event.text = new UnprotectedStringBuffer();
		do {
			if (c == '&') {
				CharSequence ref = readReference();
				if (maxTextSize <= 0 || event.text.length() < maxTextSize)
					event.text.append(ref);
			} else if (c == '<') {
				stream.back(c);
				break;
			} else {
				if (c == '\r') {
					try {
						c = stream.read();
					} catch (EOFException e) {
						if (maxTextSize <= 0 || event.text.length() < maxTextSize)
							event.text.append(c);
						break;
					}
					if (c == '\n') {
						if (maxTextSize <= 0 || event.text.length() < maxTextSize)
							event.text.append(c);
					} else {
						if (maxTextSize <= 0 || event.text.length() < maxTextSize)
							event.text.append('\r');
						continue;
					}
				} else if (maxTextSize <= 0 || event.text.length() < maxTextSize)
					event.text.append(c);
			}
			try {
				c = stream.read();
			} catch (EOFException e) {
				break;
			}
		} while (true);
	}
	
	private CharSequence readReference() throws XMLException, IOException {
		char c = stream.read();
		if (c == '#') return new UnprotectedString(readCharRef());
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		name.append(c);
		continueReadName(name);
		c = stream.read();
		if (c != ';')
			throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		return resolveEntityRef(name);
	}
	
	private char[] readCharRef() throws XMLException, IOException {
		char c = stream.read();
		INumberEncoding n;
		if (c == 'x') {
			n = new HexadecimalNumber();
		} else {
			n = new DecimalNumber();
			if (!n.addChar(c))
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		}
		do {
			c = stream.read();
			if (c == ';') break;
			if (!n.addChar(c)) throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		} while (true);
		return Character.toChars((int)n.getNumber());
	}
	
	private static CharSequence resolveEntityRef(UnprotectedStringBuffer name) {
		int l = name.length();
		if (l < 2) {
			UnprotectedStringBuffer s = new UnprotectedStringBuffer();
			s.append('&');
			s.append(name);
			s.append(';');
			return s;
			//throw new XMLException(getPosition(), "Invalid XML entity", name.toString());
		}
		char c = name.charAt(0);
		if (c == 'a') {
			if (l == 3) {
				if (name.charAt(1) == 'm' && name.charAt(2) == 'p')
					return new UnprotectedString('&');
			} else if (l == 4) {
				if (name.charAt(1) == 'p' && name.charAt(2) == 'o' && name.charAt(3) == 's')
					return new UnprotectedString('\'');
			}
		} else if (c == 'l') {
			if (l == 2 && name.charAt(1) == 't')
				return new UnprotectedString('<');
		} else if (c == 'g') {
			if (l == 2 && name.charAt(1) == 't')
				return new UnprotectedString('>');
		} else if (c == 'q') {
			if (l == 4 && name.charAt(1) == 'u' && name.charAt(2) == 'o' && name.charAt(3) == 't')
				return new UnprotectedString('"');
		}
		UnprotectedStringBuffer s = new UnprotectedStringBuffer();
		s.append('&');
		s.append(name);
		s.append(';');
		return s;
		//throw new XMLException(getPosition(), "Invalid XML entity", name.toString());
	}
	
	private UnprotectedStringBuffer readSystemLiteral() throws XMLException, IOException {
		char c = stream.read();
		if (c != '"' && c != '\'') throw new XMLException(getPosition(), "Expected character", Character.valueOf(c),
			"simple or double quote");
		UnprotectedStringBuffer literal = new UnprotectedStringBuffer();
		char quote = c;
		do {
			c = stream.read();
			if (c == quote) break;
			literal.append(c);
		} while (true);
		return literal;
	}

	private UnprotectedStringBuffer readPublicIDLiteral() throws XMLException, IOException {
		char quote = stream.read();
		if (quote != '"' && quote != '\'') throw new XMLException(getPosition(), "Expected character", Character.valueOf(quote), "\",\'");
		UnprotectedStringBuffer literal = new UnprotectedStringBuffer();
		do {
			char c = stream.read();
			if (c == quote) break;
			literal.append(c);
		} while (true);
		return literal;
	}
	
	private void readEndTag() throws XMLException, IOException {
		event.type = Type.END_ELEMENT;
		event.text = new UnprotectedStringBuffer();
		readName(event.text);
		do {
			char c = stream.read();
			if (c == '>') break;
			if (!isSpaceChar(c))
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		} while (true);
		ElementContext ctx = event.context.peekFirst();
		if (ctx == null)
			throw new XMLException(getPosition(), "Unexpected end element", event.text.asString());
		if (!ctx.text.equals(event.text))
			throw new XMLException(getPosition(), "Unexpected end element expected is", event.text.asString(), ctx.text.asString());
		int i = event.text.indexOf(':');
		if (i < 0) {
			event.namespacePrefix = new UnprotectedStringBuffer();
			event.localName = event.text;
		} else {
			event.namespacePrefix = event.text.substring(0, i);
			event.localName = event.text.substring(i + 1);
		}
		event.namespaceURI = getNamespaceURI(event.namespacePrefix);
	}
	
	private void readIntSubset() throws XMLException, IOException {
		Event save = event.copy();
		
		do {
			char c;
			try { c = stream.read(); }
			catch (EOFException e) {
				throw new XMLException(getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in internal subset declaration"));
			}
			if (isSpaceChar(c)) continue;
			if (c == ']')
				break;
			if (c == '%') {
				readIntSubsetPEReference();
				continue;
			}
			if (c != '<')
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			readIntSubsetTag();
		} while (true);
		
		event = save;
	}
	
	private void readIntSubsetPEReference() throws XMLException, IOException {
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		try {
			do {
				char c = stream.read();
				if (isSpaceChar(c)) continue;
				if (c == ';') break;
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in internal subset declaration"));
		}
	}
	
	private void readIntSubsetTag() throws XMLException, IOException {
		try {
			char c = stream.read();
			if (c == '!') {
				readIntSubsetTagExclamation();
			} else if (c == '?') {
				readProcessingInstruction();
			} else {
				throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in XML document"));
		}
	}
	
	private void readIntSubsetTagExclamation() throws XMLException, IOException {
		// can be a comment, a CDATA, or a DOCTYPE
		char c = stream.read();
		if (c == '-') {
			// comment ?
			c = stream.read();
			if (c == '-') {
				readComment();
				return;
			}
			throw new XMLException(getPosition(), "Invalid XML");
		} else if (c == 'E') {
			c = stream.read();
			if (c == 'L') {
				if (stream.read() == 'E' &&
					stream.read() == 'M' &&
					stream.read() == 'E' &&
					stream.read() == 'N' &&
					stream.read() == 'T' &&
					isSpaceChar(stream.read())) {
					readElementDeclaration();
					return;
				}
			} else if (c == 'N') {
				if (stream.read() == 'T' &&
					stream.read() == 'I' &&
					stream.read() == 'T' &&
					stream.read() == 'Y' &&
					isSpaceChar(stream.read())) {
					readEntityDeclaration();
					return;
				}
			}
		} else if (c == 'A') {
			if (stream.read() == 'T' &&
				stream.read() == 'T' &&
				stream.read() == 'L' &&
				stream.read() == 'I' &&
				stream.read() == 'S' &&
				stream.read() == 'T' &&
				isSpaceChar(stream.read())) {
				readAttListDeclaration();
				return;
			}
		} else if (c == 'N') {
			if (stream.read() == 'O' &&
				stream.read() == 'T' &&
				stream.read() == 'A' &&
				stream.read() == 'T' &&
				stream.read() == 'I' &&
				stream.read() == 'O' &&
				stream.read() == 'N' &&
				isSpaceChar(stream.read())) {
				readNotationDeclaration();
				return;
			}
		}
		throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
	}
	
	private void readElementDeclaration() throws XMLException, IOException {
		char c = stream.read();
		while (isSpaceChar(c)) c = stream.read();
		stream.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = stream.read();
		while (c != '>') c = stream.read();
	}

	private void readEntityDeclaration() throws IOException {
		char c = stream.read();
		while (isSpaceChar(c)) c = stream.read();
		boolean inString = false;
		do {
			c = stream.read();
			if (inString) {
				if (c == '"') inString = false;
				continue;
			}
			if (c == '"') inString = true;
			else if (c == '>') break;
		} while (true);
	}
	
	private void readAttListDeclaration() throws XMLException, IOException {
		char c = stream.read();
		while (isSpaceChar(c)) c = stream.read();
		stream.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = stream.read();
		while (c != '>') c = stream.read();
	}
	
	private void readNotationDeclaration() throws XMLException, IOException {
		char c = stream.read();
		while (isSpaceChar(c)) c = stream.read();
		stream.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = stream.read();
		while (c != '>') c = stream.read();
	}

	
	
	private void readName(UnprotectedStringBuffer name) throws XMLException, IOException {
		char c = stream.read();
		if (!isNameStartChar(c)) throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		name.append(c);
		continueReadName(name);
	}
	
	private void continueReadName(UnprotectedStringBuffer name) throws IOException {
		do {
			char c = stream.read();
			if (!isNameChar(c)) {
				stream.back(c);
				return;
			}
			if (maxTextSize <= 0 || name.length() < maxTextSize)
				name.append(c);
		} while (true);
	}
	
	private void readSpace() throws XMLException, IOException {
		char c = stream.read();
		if (!isSpaceChar(c)) throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		do {
			c = stream.read();
			if (!isSpaceChar(c)) {
				stream.back(c);
				return;
			}
		} while (true);
	}
	

	/** Return true if the given character is a valid starting character for a name. */
	public static boolean isNameStartChar(char c) {
		if (c < 0xF8) {
			if (c < 0x3A) return false; // :
			if (c < 0xC0) {
				if (c == ':' /*3A*/ || c == '_' /*5F*/) return true;
				if (c >= 'a' && c <= 'z') return true; /*61-7A*/
				if (c >= 'A' && c <= 'Z') return true; /*41-5A*/
				return false;
			}
			if (c == 0xD7) return false;
			if (c == 0xF7) return false;
			return true;
		}
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
	
	private static boolean isNameChar(char c) {
		if (c < 0xF8) {
			if (c < 0x3A) {
				if (c < 0x2D) return false;
				if (c == 0x2F) return false;
				return true;
			}
			if (c < 0xC0) {
				if (c == ':' /*3A*/ || c == '_' /*5F*/) return true;
				if (c >= 'a' && c <= 'z') return true; /*61-7A*/
				if (c >= 'A' && c <= 'Z') return true; /*41-5A*/
				return c == 0xB7;
			}
			if (c == 0xD7) return false;
			if (c == 0xF7) return false;
			return true;
		}
		if (c < 0x2000) {
			if (c == 0x37E) return false;
			return true;
		}
		if (c < 0xD800) {
			if (c <= 0x3000) {
				if (c < 0x2070)
					return c != 0x200C && c != 0x200D && c != 0x203F && c != 0x2040;
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
		return true;
	}

}

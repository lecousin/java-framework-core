package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.encoding.DecimalNumber;
import net.lecousin.framework.io.encoding.HexadecimalNumber;
import net.lecousin.framework.io.encoding.INumberEncoding;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.util.UnprotectedStringBufferLimited;
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
	public XMLStreamReader(IO.Readable io, Charset defaultEncoding, int charactersBuffersSize, int maxBuffers) {
		if (io instanceof IO.Readable.Buffered)
			this.io = (IO.Readable.Buffered)io;
		else
			this.io = new PreBufferedReadable(io, 1024, io.getPriority(), charactersBuffersSize,
				(byte)(io.getPriority() - 1), maxBuffers);
		this.defaultEncoding = defaultEncoding;
		this.charactersBuffersSize = charactersBuffersSize;
		this.maxBuffers = maxBuffers;
	}
	
	/** Constructor, without charset. */
	public XMLStreamReader(IO.Readable io, int charactersBuffersSize, int maxBuffers) {
		this(io, null, charactersBuffersSize, maxBuffers);
	}
	
	private Charset defaultEncoding;
	private int charactersBuffersSize;
	private int maxBuffers;
	private IO.Readable.Buffered io;
	
	/* Public methods */
	
	/** Utility method that initialize a XMLStreamReader, initialize it, and
	 * return an AsyncWork which is unblocked when characters are available to be read.
	 */
	public static AsyncSupplier<XMLStreamReader, Exception> start(
		IO.Readable.Buffered io, int charactersBufferSize, int maxBuffers, boolean addPositionInErrors
	) {
		AsyncSupplier<XMLStreamReader, Exception> result = new AsyncSupplier<>();
		new Task.Cpu.FromRunnable("Start reading XML " + io.getSourceDescription(), io.getPriority(), () -> {
			XMLStreamReader reader = new XMLStreamReader(io, charactersBufferSize, maxBuffers);
			try {
				Starter start = new Starter(io, reader.defaultEncoding, reader.charactersBuffersSize,
					reader. maxBuffers, addPositionInErrors);
				reader.stream = start.start();
				reader.stream.canStartReading().thenStart(
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
		Starter start = new Starter(io, defaultEncoding, charactersBuffersSize, maxBuffers, false);
		stream = start.start();
		next();
	}
	
	/** Move forward to the next event. */
	@Override
	public void next() throws XMLException, IOException {
		reset();
		char c;
		if ((c = stream.read()) == '<')
			readTag();
		else
			readChars(c);
	}
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public void startRootElement() throws XMLException, IOException {
		start();
		while (!Type.START_ELEMENT.equals(event.type)) next();
	}
	
	
	/* Private methods */
	
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	private void readTag() throws XMLException, IOException {
		try {
			char c;
			if (isNameStartChar(c = stream.read())) {
				readStartTag(c);
			} else if (c == '/') {
				readEndTag();
			} else if (c == '!') {
				readTagExclamation();
			} else if (c == '?') {
				readProcessingInstruction();
			} else {
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(stream, event.context,
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_IN_XML_DOCUMENT));
		}
	}
	
	private static final char[] CDATA = new char[] { 'C','D','A','T','A','[' };
	private static final char[] OCTYPE = new char[] { 'O','C','T','Y','P','E' };
	
	private void readTagExclamation() throws XMLException, IOException {
		// can be a comment, a CDATA, or a DOCTYPE
		char c;
		if ((c = stream.read()) == '-') {
			// comment ?
			if (stream.read() == '-')
				readComment();
			else
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
		} else if (c == '[') {
			// CDATA ?
			if (!readExpectedChars(CDATA))
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
			readCData();
		} else if (c == 'D') {
			// DOCTYPE ?
			if (!readExpectedChars(OCTYPE))
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
			readDocType();
		} else {
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
		}
	}
	
	private static final char[] YSTEM = new char[] { 'Y', 'S', 'T', 'E', 'M' };
	private static final char[] UBLIC = new char[] { 'U', 'B', 'L', 'I', 'C' };
	
	private void readDocType() throws XMLException, IOException {
		event.type = Type.DOCTYPE;
		event.text = new UnprotectedStringBuffer();
		readSpace();
		readName(event.text);
		char c;
		while (isSpaceChar(c = stream.read()));
		if (c == 'S') {
			if (!readExpectedChars(YSTEM))
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
			readSpace();
			event.system = readSystemLiteral();
			c = stream.read();
			while (isSpaceChar(c)) c = stream.read();
		} else if (c == 'P') {
			if (!readExpectedChars(UBLIC))
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
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
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
	}
	
	private void readComment() throws XMLException, IOException {
		event.type = Type.COMMENT;
		UnprotectedStringBuffer t = event.text =
			maxTextSize <= 0 ? new UnprotectedStringBuffer() : new UnprotectedStringBufferLimited(maxTextSize);
		char c;
		try {
			do {
				if (!stream.readUntil('-', t))
					throw new XMLException(stream, event.context, new LocalizableString(
						XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
						new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside comment"));
				if ((c = stream.read()) != '-') {
					t.append(new UnprotectedString(new char[] { '-', c }));
					continue;
				}
				do {
					if ((c = stream.read()) == '>')
						return;
					if (c == '-') {
						t.append('-');
						continue;
					}
					t.append(new UnprotectedString(new char[] { '-', '-', c }));
					break;
				} while (true);
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(stream, event.context,
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside comment"));
		}
	}
	
	private boolean readExpectedChars(char[] expected) throws IOException {
		for (int i = 0; i < expected.length; ++i)
			if (expected[i] != stream.read())
				return false;
		return true;
	}
	
	private void readCData() throws XMLException, IOException {
		event.type = Type.CDATA;
		UnprotectedStringBuffer t = event.text =
			maxCDataSize <= 0 ? new UnprotectedStringBuffer() : new UnprotectedStringBufferLimited(maxCDataSize);
		char c;
		try {
			do {
				if (!stream.readUntil(']', t))
					throw new XMLException(stream, event.context, new LocalizableString(
						XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
						new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside CDATA"));
				if ((c = stream.read()) != ']') {
					t.append(new char[] { ']', c });
					continue;
				}
				do {
					if ((c = stream.read()) == '>')
						return;
					if (c == ']') {
						t.append(']');
						continue;
					}
					t.append(new char[] { ']', ']', c });
					break;
				} while (true);
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(stream, event.context,
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside CDATA"));
		}
	}
	
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	private void readStartTag(char c) throws XMLException, IOException {
		event.type = Type.START_ELEMENT;
		event.attributes = new LinkedList<>();
		int colonPos = continueReadName(event.text = new UnprotectedStringBuffer(), c);
		do {
			while (isSpaceChar(c = stream.read()));
			if (c == '>') break;
			if (c == '/') {
				if (stream.read() != '>')
					throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
				event.isClosed = true;
				break;
			}
			if (!isNameStartChar(c))
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			Attribute a = new Attribute();
			
			// attribute name
			int i = continueReadName(a.text = new UnprotectedStringBuffer(), c);
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
				throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_EXPECTED_CHARACTER,
					Character.valueOf(c), Character.valueOf('='));
			
			// attribute value
			while (isSpaceChar(c = stream.read()));
			a.value = new UnprotectedStringBuffer();
			if (c == '"' || c == '\'') readAttrValue(a.value, c);
			else throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			event.attributes.add(a);
		} while (true);
		onStartElement(colonPos);
	}
	
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
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
			int i = continueReadName(a.text = new UnprotectedStringBuffer(), c);
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
			else throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			event.attributes.add(a);
		} while (true);
	}
	
	private void readAttrValue(UnprotectedStringBuffer value, char quote) throws XMLException, IOException {
		int pos = 0;
		char[] chars = null;
		do {
			char c = stream.read();
			if (c == quote) break;
			if (c == '&') {
				if (pos > 0) {
					value.append(new UnprotectedString(chars, 0, pos, chars.length));
					pos = 0;
				}
				value.append(readReference());
			} else if (c != '\r') {
				if (c == '\n') c = ' ';
				else if (c == '<') throw new XMLException(stream, event.context, new LocalizableString(
					XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER,
					Character.valueOf(c)),new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,"in attribute value"));
				if (pos == 0)
					chars = new char[16];
				chars[pos++] = c;
				if (pos == chars.length) {
					value.append(new UnprotectedString(chars, 0, chars.length, chars.length));
					pos = 0;
				}
			}
		} while (true);
		if (pos > 0)
			value.append(new UnprotectedString(chars, 0, pos, chars.length));
	}
	
	@SuppressWarnings("squid:S1141") // nested try
	private void readChars(char c) throws XMLException, IOException {
		event.type = Type.TEXT;
		UnprotectedStringBuffer t = event.text =
			maxTextSize <= 0 ? new UnprotectedStringBuffer() : new UnprotectedStringBufferLimited(maxTextSize);
		try {
			do {
				if (c == '<') { // 0x3C
					stream.back(c);
					break;
				} else if (c == '\r') { // 0x0A
					try {
						c = stream.read();
					} catch (EOFException e) {
						t.append(c);
						break;
					}
					if (c == '\n') {
						t.append(c);
					} else {
						t.append('\r');
						continue;
					}
				} else if (c == '&') { // 0x26
					CharSequence ref = readReference();
					t.append(ref);
				} else {
					t.append(c);
				}
				c = stream.read();
			} while (true);
		} catch (EOFException e) {
			// the end
		}
	}
	
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	private CharSequence readReference() throws XMLException, IOException {
		char c = stream.read();
		if (c == '#') return new UnprotectedString(readCharRef());
		UnprotectedStringBuffer name;
		continueReadName(name = new UnprotectedStringBuffer(), c);
		c = stream.read();
		if (c != ';')
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
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
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
		}
		do {
			c = stream.read();
			if (c == ';') break;
			if (!n.addChar(c)) throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER,
				Character.valueOf(c));
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
			//throw new XMLException(stream, event.context, "Invalid XML entity", name.toString());
		}
		char c = name.charAt(0);
		if (c == 'a') {
			if (l == 3) {
				if (name.charAt(1) == 'm' && name.charAt(2) == 'p')
					return new UnprotectedString('&');
			} else if (l == 4 && name.charAt(1) == 'p' && name.charAt(2) == 'o' && name.charAt(3) == 's') {
				return new UnprotectedString('\'');
			}
		} else if (c == 'l') {
			if (l == 2 && name.charAt(1) == 't')
				return new UnprotectedString('<');
		} else if (c == 'g') {
			if (l == 2 && name.charAt(1) == 't')
				return new UnprotectedString('>');
		} else if (c == 'q' && l == 4 && name.charAt(1) == 'u' && name.charAt(2) == 'o' && name.charAt(3) == 't') {
			return new UnprotectedString('"');
		}
		UnprotectedStringBuffer s = new UnprotectedStringBuffer();
		s.append('&');
		s.append(name);
		s.append(';');
		return s;
		//throw new XMLException(stream, event.context, "Invalid XML entity", name.toString());
	}
	
	private UnprotectedStringBuffer readSystemLiteral() throws XMLException, IOException {
		char c = stream.read();
		if (c != '"' && c != '\'')
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_EXPECTED_CHARACTER, Character.valueOf(c),
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
		if (quote != '"' && quote != '\'')
			throw new XMLException(stream, event.context,
				XMLException.LOCALIZED_MESSAGE_EXPECTED_CHARACTER, Character.valueOf(quote), "\",\'");
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
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
		} while (true);
		ElementContext ctx = event.context.peekFirst();
		if (ctx == null)
			throw new XMLException(stream, event.context, "Unexpected end element", event.text.asString());
		if (!ctx.text.equals(event.text))
			throw new XMLException(stream, event.context,
				"Unexpected end element expected is", event.text.asString(), ctx.text.asString());
		event.namespacePrefix = ctx.namespacePrefix;
		event.localName = ctx.localName;
		event.namespaceURI = getNamespaceURI(event.namespacePrefix);
	}
	
	private void readIntSubset() throws XMLException, IOException {
		Event save = event.copy();
		
		do {
			char c;
			try { c = stream.read(); }
			catch (EOFException e) {
				throw new XMLException(stream, event.context,
					new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
						XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
					new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
						XMLException.LOCALIZED_MESSAGE_IN_INTERNAL_SUBSET));
			}
			if (isSpaceChar(c)) continue;
			if (c == ']')
				break;
			if (c == '%') {
				readIntSubsetPEReference();
				continue;
			}
			if (c != '<')
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
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
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(stream, event.context,
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_IN_INTERNAL_SUBSET));
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
				throw new XMLException(stream, event.context,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(stream, event.context, new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_IN_XML_DOCUMENT));
		}
	}
	
	@SuppressWarnings("squid:S1764")
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
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_INVALID_XML);
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
			} else if ((c == 'N') &&
				stream.read() == 'T' &&
				stream.read() == 'I' &&
				stream.read() == 'T' &&
				stream.read() == 'Y' &&
				isSpaceChar(stream.read())) {
				readEntityDeclaration();
				return;
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
		} else if ((c == 'N') &&
			stream.read() == 'O' &&
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
		throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
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
		readElementDeclaration();
	}
	
	private void readNotationDeclaration() throws XMLException, IOException {
		readElementDeclaration();
	}

	
	
	private int readName(UnprotectedStringBuffer name) throws XMLException, IOException {
		char c = stream.read();
		if (!isNameStartChar(c))
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
		return continueReadName(name, c);
	}
	
	@SuppressWarnings({"squid:AssignmentInSubExpressionCheck", "squid:S3776"})
	private int continueReadName(UnprotectedStringBuffer name, char c) throws IOException {
		int charsSize = 16;
		char[] chars = new char[charsSize];
		chars[0] = c;
		int pos = 1;
		int colonPos = c == ':' ? 0 : -1;
		if (maxTextSize <= 0)
			do {
				if (!isNameChar(c = chars[pos] = stream.read())) {
					stream.back(chars[pos]);
					if (pos > 0)
						name.append(new UnprotectedString(chars, 0, pos, charsSize));
					return colonPos;
				}
				if (c == ':' && colonPos == -1) colonPos = name.length() + pos;
				if (++pos == charsSize) {
					name.append(new UnprotectedString(chars));
					charsSize *= 2;
					chars = new char[charsSize];
					pos = 0;
				}
			} while (true);
		int len = 1;
		do {
			if (!isNameChar(c = stream.read())) {
				stream.back(c);
				if (pos > 0)
					name.append(new UnprotectedString(chars, 0, pos, charsSize));
				return colonPos;
			}
			if (len < maxTextSize) {
				chars[pos] = c;
				if (c == ':' && colonPos == -1) colonPos = name.length() + pos;
				if (++pos == charsSize) {
					name.append(new UnprotectedString(chars));
					charsSize *= 2;
					chars = new char[charsSize];
					pos = 0;
				}
			}
		} while (true);
	}
	
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	private void readSpace() throws XMLException, IOException {
		char c;
		if (!isSpaceChar(c = stream.read()))
			throw new XMLException(stream, event.context, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c));
		do {
			if (!isSpaceChar(c = stream.read())) {
				stream.back(c);
				return;
			}
		} while (true);
	}

}

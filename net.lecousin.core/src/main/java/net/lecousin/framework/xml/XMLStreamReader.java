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
	private BufferedReadableCharacterStreamLocation stream;
	
	/* Public methods */
	
	/** Utility method that initialize a XMLStreamReader, initialize it, and
	 * return an AsyncWork which is unblocked when characters are available to be read.
	 */
	public static AsyncWork<XMLStreamReader, Exception> start(IO.Readable.Buffered io, int charactersBufferSize, int maxBuffers) {
		AsyncWork<XMLStreamReader, Exception> result = new AsyncWork<>();
		new Task.Cpu.FromRunnable("Start reading XML " + io.getSourceDescription(), io.getPriority(), () -> {
			XMLStreamReader reader = new XMLStreamReader(io, charactersBufferSize, maxBuffers);
			try {
				Starter start = new Starter(io, reader.defaultEncoding, reader.charactersBuffersSize, reader.maxBuffers);
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
		Starter start = new Starter(io, defaultEncoding, charactersBuffersSize, maxBuffers);
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
			char c;
			if ((c = stream.read()) == '!') {
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
		char c;
		if ((c = stream.read()) == '-') {
			// comment ?
			if ((c = stream.read()) == '-')
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
		UnprotectedStringBuffer t = event.text = new UnprotectedStringBuffer();
		char[] chars = new char[64];
		int pos = 0;
		char c;
		try {
			if (maxTextSize <= 0)
				do {
					if ((c = stream.read()) != '-') {
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						continue;
					}
					if ((c = stream.read()) != '-') {
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						continue;
					}
					do {
						if ((c = stream.read()) == '>') {
							if (pos > 0)
								t.append(new UnprotectedString(chars, 0, pos, chars.length));
							return;
						}
						if (c == '-') {
							chars[pos] = '-';
							if (++pos == chars.length) {
								t.append(new UnprotectedString(chars));
								chars = new char[chars.length * 2];
								pos = 0;
							}
							continue;
						}
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						break;
					} while (true);
				} while (true);
			int len = 0;
			do {
				if ((c = stream.read()) != '-') {
					if (len < maxTextSize) {
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len++;
					}
					continue;
				}
				if ((c = stream.read()) != '-') {
					if (len < maxTextSize) {
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len += 2;
					}
					continue;
				}
				do {
					if ((c = stream.read()) == '>') {
						if (pos > 0)
							t.append(new UnprotectedString(chars, 0, pos, chars.length));
						return;
					}
					if (c == '-') {
						if (len < maxTextSize) {
							chars[pos] = '-';
							if (++pos == chars.length) {
								t.append(new UnprotectedString(chars));
								chars = new char[chars.length * 2];
								pos = 0;
							}
							len++;
						}
						continue;
					}
					if (len < maxTextSize) {
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = '-';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len += 2;
					}
					break;
				} while (true);
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "inside comment"));
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
		UnprotectedStringBuffer t = event.text = new UnprotectedStringBuffer();
		char[] chars = new char[64];
		int pos = 0;
		char c;
		try {
			if (maxCDataSize <= 0) {
				do {
					if ((c = stream.read()) != ']') {
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						continue;
					}
					if ((c = stream.read()) != ']') {
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						continue;
					}
					do {
						if ((c = stream.read()) == '>') {
							if (pos > 0)
								t.append(new UnprotectedString(chars, 0, pos, chars.length));
							return;
						}
						if (c == ']') {
							chars[pos] = ']';
							if (++pos == chars.length) {
								t.append(new UnprotectedString(chars));
								chars = new char[chars.length * 2];
								pos = 0;
							}
							continue;
						}
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						break;
					} while (true);
				} while (true);
			}
			int len = 0;
			do {
				if ((c = stream.read()) != ']') {
					if (len < maxCDataSize) {
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len++;
					}
					continue;
				}
				if ((c = stream.read()) != ']') {
					if (len < maxCDataSize) {
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len += 2;
					}
					continue;
				}
				do {
					if ((c = stream.read()) == '>') {
						if (pos > 0)
							t.append(new UnprotectedString(chars, 0, pos, chars.length));
						return;
					}
					if (c == ']') {
						if (len < maxCDataSize) {
							chars[pos] = ']';
							if (++pos == chars.length) {
								t.append(new UnprotectedString(chars));
								chars = new char[chars.length * 2];
								pos = 0;
							}
							len++;
						}
						continue;
					}
					if (len < maxCDataSize) {
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = ']';
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						chars[pos] = c;
						if (++pos == chars.length) {
							t.append(new UnprotectedString(chars));
							chars = new char[chars.length * 2];
							pos = 0;
						}
						len += 3;
					}
					break;
				} while (true);
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "inside CDATA"));
		}
	}
	
	private void readStartTag(char c) throws XMLException, IOException {
		event.type = Type.START_ELEMENT;
		event.attributes = new LinkedList<>();
		continueReadName(event.text = new UnprotectedStringBuffer(), c);
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
			continueReadName(a.text = new UnprotectedStringBuffer(), c);
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
			a.value = new UnprotectedStringBuffer(new UnprotectedString(32));
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
			continueReadName(a.text = new UnprotectedStringBuffer(), c);
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
		UnprotectedStringBuffer t = event.text = new UnprotectedStringBuffer(new UnprotectedString(64));
		try {
			if (maxTextSize <= 0) {
				do {
					if (c == '&') {
						CharSequence ref = readReference();
						t.append(ref);
					} else if (c == '<') {
						stream.back(c);
						break;
					} else if (c == '\r') {
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
					} else
						t.append(c);
					c = stream.read();
				} while (true);
			} else {
				do {
					if (c == '&') {
						CharSequence ref = readReference();
						if (t.length() < maxTextSize)
							t.append(ref);
					} else if (c == '<') {
						stream.back(c);
						break;
					} else if (c == '\r') {
						try {
							c = stream.read();
						} catch (EOFException e) {
							if (t.length() < maxTextSize)
								t.append(c);
							break;
						}
						if (c == '\n') {
							if (t.length() < maxTextSize)
								t.append(c);
						} else {
							if (t.length() < maxTextSize)
								t.append('\r');
							continue;
						}
					} else if (t.length() < maxTextSize)
						t.append(c);
					c = stream.read();
				} while (true);
			}
		} catch (EOFException e) {
			// the end
		}
	}
	
	private CharSequence readReference() throws XMLException, IOException {
		char c = stream.read();
		if (c == '#') return new UnprotectedString(readCharRef());
		UnprotectedStringBuffer name;
		continueReadName(name = new UnprotectedStringBuffer(), c);
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
		event.text = new UnprotectedStringBuffer(new UnprotectedString(20));
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
		continueReadName(name, c);
	}
	
	private void continueReadName(UnprotectedStringBuffer name, char c) throws IOException {
		char[] chars = new char[16];
		chars[0] = c;
		int pos = 1;
		if (maxTextSize <= 0)
			do {
				if (!isNameChar(c = stream.read())) {
					stream.back(c);
					if (pos > 0)
						name.append(new UnprotectedString(chars, 0, pos, chars.length));
					return;
				}
				chars[pos] = c;
				if (++pos == chars.length) {
					name.append(new UnprotectedString(chars));
					chars = new char[chars.length * 2];
					pos = 0;
				}
			} while (true);
		int len = 1;
		do {
			if (!isNameChar(c = stream.read())) {
				stream.back(c);
				if (pos > 0)
					name.append(new UnprotectedString(chars, 0, pos, chars.length));
				return;
			}
			if (len < maxTextSize) {
				chars[pos] = c;
				if (++pos == chars.length) {
					name.append(new UnprotectedString(chars));
					chars = new char[chars.length * 2];
					pos = 0;
				}
			}
		} while (true);
	}
	
	private void readSpace() throws XMLException, IOException {
		char c;
		if (!isSpaceChar(c = stream.read())) throw new XMLException(getPosition(), "Unexpected character", Character.valueOf(c));
		do {
			if (!isSpaceChar(c = stream.read())) {
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

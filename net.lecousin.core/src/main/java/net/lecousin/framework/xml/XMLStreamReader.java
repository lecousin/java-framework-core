package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.encoding.DecimalNumber;
import net.lecousin.framework.io.encoding.HexadecimalNumber;
import net.lecousin.framework.io.encoding.INumberEncoding;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Read an XML in a similar way as {@link javax.xml.stream.XMLStreamReader}: read-only, forward, event based.
 * The method next() allows to move forward to the next event (such as start element, end element, comment, text...).
 * It uses {@link UnprotectedString} to avoid allocating many character arrays.
 * Charset is automatically detected by reading the beginning of the XML (either with auto-detection or with the specified encoding).
 */
public class XMLStreamReader {

	/** Initialize with the given IO.
	 * If it is not buffered, a {@link PreBufferedReadable} is created.
	 * If the charset is null, it will be automatically detected.
	 */
	public XMLStreamReader(IO.Readable io, Charset forcedEncoding, int charactersBuffersSize) {
		if (io instanceof IO.Readable.Buffered)
			this.io = (IO.Readable.Buffered)io;
		else
			this.io = new PreBufferedReadable(io, 1024, io.getPriority(), charactersBuffersSize, (byte)(io.getPriority() - 1), 4);
		this.forcedCharset = forcedEncoding;
		this.charactersBuffersSize = charactersBuffersSize;
	}
	
	/** Constructor, without charset. */
	public XMLStreamReader(IO.Readable io, int charactersBuffersSize) {
		this(io, null, charactersBuffersSize);
	}
	
	private Charset forcedCharset;
	private int charactersBuffersSize;
	
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
	/** When type if START_ELEMENT, it specifies if it is an empty-element. */
	public boolean isClosed = false;
	/** When type if START_ELEMENT, it contains the list of attributes. */
	public ArrayList<Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>> attributes = null;
	/** System specified in the DOCTYPE tag. */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public UnprotectedStringBuffer system = null;
	/** Public ID specified in the DOCTYPE tag. */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public UnprotectedStringBuffer publicId = null;
	/** Allows to specify a listener that will be called on each START_ELEMENT event. */
	@SuppressFBWarnings("UWF_NULL_FIELD")
	public Listener<XMLStreamReader> onElement = null;
	
	/** Shortcut to get the value of the given attribute name as a String. */
	public String getAttribute(String name) {
		for (Pair<UnprotectedStringBuffer,UnprotectedStringBuffer> attr : attributes)
			if (attr.getValue1().equals(name))
				return attr.getValue2().toString();
		return null;
	}
	
	/** Remove an attribute from the list if it exists. */
	public UnprotectedStringBuffer removeAttribute(String name) {
		for (Iterator<Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>> it = attributes.iterator(); it.hasNext(); ) {
			Pair<UnprotectedStringBuffer,UnprotectedStringBuffer> attr = it.next();
			if (attr.getValue1().equals(name)) {
				it.remove();
				return attr.getValue2();
			}
		}
		return null;
	}
	
	private void reset() {
		type = null;
		text = null;
		isClosed = false;
		attributes = null;
	}
	
	private IO.Readable.Buffered io;
	private CharacterProvider cp;
	
	private abstract static class CharacterProvider {
		public CharacterProvider(char... firstChars) {
			this.firstChars = firstChars;
			this.pos = firstChars.length > 0 ? 0 : -1;
		}
		
		private char[] firstChars;
		private int pos;
		private char back;
		private boolean hasBack = false;
		private int lastLine = 0;
		private int lastPosInLine = 0;
		private boolean lastIsNewLine = true;
		
		public final char nextChar() throws IOException {
			if (hasBack) {
				hasBack = false;
				return back;
			}
			if (pos >= 0) {
				char c = firstChars[pos++];
				if (pos == firstChars.length) pos = -1;
				return updatePos(c);
			}
			return updatePos(getNextChar());
		}
		
		public final void back(char c) {
			hasBack = true;
			back = c;
		}
		
		protected abstract char getNextChar() throws IOException;
		
		private char updatePos(char c) {
			if (lastIsNewLine) {
				lastLine++;
				lastPosInLine = 1;
			} else
				lastPosInLine++;
			lastIsNewLine = (c == '\n');
			return c;
		}
		
		public final Pair<Integer,Integer> getPosition() {
			return new Pair<>(Integer.valueOf(lastLine), Integer.valueOf(lastPosInLine));
		}
	}
	
	private class StartCharacterProvider extends CharacterProvider {
		public StartCharacterProvider(char... firstChars) {
			super(firstChars);
		}
		
		protected int nextUTF8 = -1;
		
		@Override
		protected char getNextChar() throws IOException {
			if (nextUTF8 != -1) {
				char c = (char)nextUTF8;
				nextUTF8 = -1;
				return c;
			}
			int c = io.read();
			if (c < 0) throw new EOFException();
			// UTF-8 decoding by default
			if (c < 0x80)
				return (char)c;
			int up = c & 0xF0;
			if (up == 0xC0) {
				// two bytes
				int c2 = io.read();
				if (c2 < 0) return (char)c;
				if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				return (char)((c2 & 0x3F) | ((c & 0x1F) << 6));
			}
			if (up == 0xE0) {
				// 3 bytes
				int c2 = io.read();
				if (c2 < 0) return (char)c;
				int c3 = io.read();
				if (c3 < 0) throw new IOException("Invalid UTF-8 character");
				if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				if ((c3 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				return (char)((c3 & 0x3F) | ((c2 & 0x3F) << 6) | ((c & 0xF) << 12));
			}
			// 4 bytes
			if ((c & 0xF8) != 0xF0) throw new IOException("Invalid UTF-8 character");
			int c2 = io.read();
			if (c2 < 0) return (char)c;
			if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
			int c3 = io.read();
			if (c3 < 0) throw new IOException("Invalid UTF-8 character");
			if ((c3 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
			int c4 = io.read();
			if (c4 < 0) throw new IOException("Invalid UTF-8 character");
			if ((c4 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
			int ch = (c4 & 0x3F) | ((c3 & 0x3F) << 6) | ((c2 & 0x3F) << 12) | (c & 0xF) << 18;
			ch -= 0x010000;
			nextUTF8 = (ch & 0x3FF) + 0xDC00;
			return (char)(((ch & 0xFFC00) >> 10) + 0xD800);
		}
	}
	
	private class CharacterStreamProvider extends CharacterProvider {
		public CharacterStreamProvider(Charset charset, char... firstChars) {
			super(firstChars);
			stream = new BufferedReadableCharacterStream(io, charset, charactersBuffersSize, 8);
		}
		
		private BufferedReadableCharacterStream stream;
		
		@Override
		protected char getNextChar() throws IOException {
			return stream.read();
		}
		
	}
	
	private void initCharacterProvider() throws XMLException, IOException {
		// detect BOM
		// even we have a forced charset given, we read the BOM but ignore its signification
		int c1 = io.read();
		if (c1 < 0) throw new XMLException(null, "File is empty");
		int c2 = io.read();
		if (c2 < 0) throw new XMLException(null, "Not an XML file");
		switch (c1) {
		case 0xEF:
			// it may be a UTF-8 BOM
			if (c2 == 0xBB) {
				int c3 = io.read();
				if (c3 < 0) throw new XMLException(null, "Not an XML file");
				if (c3 == 0xBF) {
					// UTF-8 BOM
					if (forcedCharset == null)
						cp = new CharacterStreamProvider(StandardCharsets.UTF_8);
					else
						cp = new CharacterStreamProvider(forcedCharset);
					return;
				}
				cp = new StartCharacterProvider((char)c1, (char)c2, (char)c3);
				return;
			}
			cp = new StartCharacterProvider((char)c1, (char)c2);
			return;
		case 0xFE:
			// it may be a UTF-16 big-endian BOM
			if (c2 == 0xFF) {
				// UTF-16 big-endian
				if (forcedCharset == null)
					cp = new CharacterStreamProvider(StandardCharsets.UTF_16BE);
				else
					cp = new CharacterStreamProvider(forcedCharset);
				return;
			}
			cp = new StartCharacterProvider((char)c1, (char)c2);
			return;
		case 0xFF:
			// it may be a BOM for UTF-16 little-endian or UTF-32 little-endian
			if (c2 == 0xFE) {
				int c3 = io.read();
				if (c3 < 0) throw new XMLException(null, "Not an XML file");
				if (c3 == 0x00) {
					int c4 = io.read();
					if (c4 < 0) throw new XMLException(null, "Not an XML file");
					if (c4 == 0x00) {
						// UTF-32 little-endian
						if (forcedCharset == null)
							cp = new CharacterStreamProvider(Charset.forName("UTF-32LE"));
						else
							cp = new CharacterStreamProvider(forcedCharset);
						return;
					}
					cp = new StartCharacterProvider((char)c1, (char)c2, (char)c3, (char)c4);
					return;
				}
				// UTF-16 little-endian
				if (forcedCharset == null) {
					int c4 = io.read();
					if (c4 < 0) throw new XMLException(null, "Not an XML file");
					cp = new CharacterStreamProvider(StandardCharsets.UTF_16LE, (char)(c3 | (c4 << 8)));
				} else
					cp = new CharacterStreamProvider(forcedCharset);
				return;
			}
			cp = new StartCharacterProvider((char)c1, (char)c2);
			return;
		case 0x00:
			// it may be a UTF-32 big-endian BOM, but it may also be UTF-16 without BOM
			if (c2 == 0x00) {
				int c3 = io.read();
				if (c3 < 0) throw new XMLException(null, "Not an XML file");
				if (c3 == 0xFE) {
					int c4 = io.read();
					if (c4 < 0) throw new XMLException(null, "Not an XML file");
					if (c4 == 0xFF) {
						// UTF-32 big-endian
						if (forcedCharset == null)
							cp = new CharacterStreamProvider(Charset.forName("UTF-32BE"));
						else
							cp = new CharacterStreamProvider(forcedCharset);
						return;
					}
					cp = new StartCharacterProvider((char)c1, (char)c2, (char)c3, (char)c4);
					return;
				}
				// UTF-16 without BOM
				if (forcedCharset == null)
					cp = new CharacterStreamProvider(StandardCharsets.UTF_16BE, (char)((c1 << 8) | c2));
				else
					cp = new CharacterStreamProvider(forcedCharset);
				return;
			}
			cp = new StartCharacterProvider((char)c1, (char)c2);
			return;
		// TODO other BOM ? (https://en.wikipedia.org/wiki/Byte_order_mark#Representations_of_byte_order_marks_by_encoding)
		// TODO https://www.w3.org/TR/REC-xml/#sec-guessing
		default: break;
		}
		cp = new StartCharacterProvider((char)c1, (char)c2);
	}
	
	/* Public methods */
	
	/** Utility method that initialize a XMLStreamReader, initialize it, and
	 * return an AsyncWork which is unblocked when characters are available to be read.
	 */
	public static AsyncWork<XMLStreamReader, Exception> start(IO.Readable.Buffered io, int charactersBufferSize) {
		AsyncWork<XMLStreamReader, Exception> result = new AsyncWork<>();
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>(
			"Start reading XML " + io.getSourceDescription(), io.getPriority()
		) {
			@Override
			public Void run() {
				XMLStreamReader reader = new XMLStreamReader(io, charactersBufferSize);
				try {
					reader.initCharacterProvider();
				} catch (Exception e) {
					result.unblockError(e);
					return null;
				}
				if (reader.cp instanceof CharacterStreamProvider) {
					((CharacterStreamProvider)reader.cp).stream.canStartReading().listenAsynch(
					new Task.Cpu<Void, NoException>("Start reading XML" + io.getSourceDescription(), io.getPriority()) {
						@Override
						public Void run() {
							try {
								reader.next();
								if (reader.checkFirstElement())
									reader.next();
								result.unblockSuccess(reader);
							} catch (EOFException e) {
								result.unblockError(new XMLException(null, "Invalid XML"));
							} catch (Exception e) {
								result.unblockError(e);
							}
							return null;
						}
					}, true);
					return null;
				}
				try {
					reader.next();
					if (reader.checkFirstElement()) {
						if (reader.cp instanceof CharacterStreamProvider) {
							((CharacterStreamProvider)reader.cp).stream.canStartReading().listenAsynch(
							new Task.Cpu<Void, NoException>(
								"Start reading XML" + io.getSourceDescription(), io.getPriority()
							) {
								@Override
								public Void run() {
									try {
										reader.next();
										result.unblockSuccess(reader);
									} catch (EOFException e) {
										result.unblockError(new XMLException(null, "Invalid XML"));
									} catch (Exception e) {
										result.unblockError(e);
									}
									return null;
								}
							}, true);
							return null;
						}
						reader.next();
					}
				} catch (EOFException e) {
					result.unblockError(new XMLException(null, "Invalid XML"));
					return null;
				} catch (Exception e) {
					result.unblockError(e);
					return null;
				}
				if (reader.cp instanceof CharacterStreamProvider)
					((CharacterStreamProvider)reader.cp).stream.canStartReading().listenInline(() -> {
						result.unblockSuccess(reader);
					});
				else
					result.unblockSuccess(reader);
				return null;
			}
		};
		task.startOn(io.canStartReading(), true);
		return result;
	}
	
	/** Start reading the XML to provide the first event.
	 * If the first tag is a processing instruction XML it reads it and goes to the next event.
	 */
	public void start() throws XMLException, IOException {
		try {
			initCharacterProvider();
			next();
			if (checkFirstElement()) next();
		} catch (EOFException e) {
			throw new XMLException(null, "Invalid XML");
		}
	}
	
	private boolean checkFirstElement() {
		if (Type.PROCESSING_INSTRUCTION.equals(type)) {
			if (text.length() == 3) {
				char c = text.charAt(0);
				if (c == 'x' || c == 'X') {
					c = text.charAt(1);
					if (c == 'm' || c == 'M') {
						c = text.charAt(2);
						if (c == 'l' || c == 'L') {
							String encoding = getAttribute("encoding");
							if (encoding != null)
								cp = new CharacterStreamProvider(Charset.forName(encoding));
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/** Move forward to the next event. */
	public void next() throws XMLException, IOException {
		char c = cp.nextChar();
		if (c == '<')
			readTag();
		else
			readChars(c);
	}
	
	/* Public utility methods */
	
	// TODO optimization: when searching for something, do not load every information in between
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public void startRootElement() throws XMLException, IOException {
		start();
		while (!Type.START_ELEMENT.equals(type)) next();
	}
	
	/** Shortcut to move forward to the next START_ELEMENT, return false if no START_ELEMENT event was found. */
	public boolean nextStartElement() throws XMLException, IOException {
		try {
			do {
				next();
			} while (!Type.START_ELEMENT.equals(type));
			return true;
		} catch (EOFException e) {
			return false;
		}
	}
	
	/** Go to the next inner element. Return true if one is found, false if the closing tag of the parent is found. */
	public boolean nextInnerElement(String parentName) throws XMLException, IOException {
		try {
			if (Type.START_ELEMENT.equals(type) && text.equals(parentName) && isClosed)
				return false; // we are one the opening tag of the parent, and it is closed, so it has no content
			do {
				next();
				if (Type.END_ELEMENT.equals(type)) {
					if (!text.equals(parentName))
						throw new XMLException(cp.getPosition(), "Unexpected end element", text.asString());
					return false;
				}
				if (Type.START_ELEMENT.equals(type))
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
				if (Type.END_ELEMENT.equals(type)) {
					if (!text.equals(parentName))
						throw new XMLException(cp.getPosition(), "Unexpected end element", text.asString());
					return false;
				}
				if (Type.START_ELEMENT.equals(type)) {
					if (!text.equals(childName)) {
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
		if (!Type.START_ELEMENT.equals(type))
			throw new IOException("Invalid call of readInnerText: it must be called on a start element");
		if (isClosed) return new UnprotectedStringBuffer();
		UnprotectedStringBuffer elementName = text;
		UnprotectedStringBuffer innerText = new UnprotectedStringBuffer();
		do {
			try { next(); }
			catch (EOFException e) {
				throw new XMLException(cp.getPosition(), "Unexpected end", "readInnerText(" + elementName.toString() + ")");
			}
			if (Type.COMMENT.equals(type)) continue;
			if (Type.TEXT.equals(type)) {
				innerText.append(text);
				continue;
			}
			if (Type.START_ELEMENT.equals(type)) {
				closeElement();
				continue;
			}
			if (Type.END_ELEMENT.equals(type)) {
				if (!text.equals(elementName))
					throw new XMLException(cp.getPosition(), "Unexpected end element", text.asString());
				return innerText;
			}
		} while (true);
	}
	
	/** Go the the END_ELEMENT event corresponding to the current START_ELEMENT (must be called with a current event to START_ELEMENT). */
	public void closeElement() throws XMLException, IOException {
		if (!Type.START_ELEMENT.equals(type))
			throw new IOException("Invalid call of closeElement: it must be called on a start element");
		if (isClosed) return;
		UnprotectedStringBuffer elementName = text;
		do {
			try { next(); }
			catch (EOFException e) {
				throw new XMLException(cp.getPosition(), "Unexpected end", "closeElement(" + elementName.asString() + ")");
			}
			if (Type.END_ELEMENT.equals(type)) {
				if (!text.equals(elementName))
					throw new XMLException(cp.getPosition(), "Unexpected end element expected is",
							text.asString(), elementName.asString());
				return;
			}
			if (Type.START_ELEMENT.equals(type))
				closeElement();
		} while (true);
	}
	
	/** Move forward until an element with the given name is found (whatever its depth).
	 * @return true if found, false if the end of file is reached.
	 */
	public boolean searchElement(String elementName) throws XMLException, IOException {
		try {
			do {
				if (Type.START_ELEMENT.equals(type)) {
					if (text.equals(elementName))
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
		String parentName = text.toString();
		return nextInnerElement(parentName, innerElement);
	}
	
	/** Read all inner elements with their text, and return a mapping with the element's name as key and inner text as value. */
	public Map<String,String> readInnerElementsText(String parentName) throws IOException, XMLException {
		Map<String, String> texts = new HashMap<>();
		while (nextInnerElement(parentName)) {
			String name = text.toString();
			texts.put(name, readInnerText().asString());
		}
		return texts;
	}
	
	/* Private methods */
	
	private void readTag() throws XMLException, IOException {
		try {
			char c = cp.nextChar();
			if (c == '!') {
				readTagExclamation();
			} else if (c == '?') {
				readProcessingInstruction();
			} else if (c == '/') {
				readEndTag();
			} else if (isNameStartChar(c)) {
				readStartTag(c);
			} else {
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in XML document"));
		}
	}
	
	private static char[] CDATA = new char[] { 'C','D','A','T','A','[' };
	private static char[] OCTYPE = new char[] { 'O','C','T','Y','P','E' };
	
	private void readTagExclamation() throws XMLException, IOException {
		// can be a comment, a CDATA, or a DOCTYPE
		char c = cp.nextChar();
		if (c == '-') {
			// comment ?
			c = cp.nextChar();
			if (c == '-')
				readComment();
			else
				throw new XMLException(cp.getPosition(), "Invalid XML");
		} else if (c == '[') {
			// CDATA ?
			if (!readExpectedChars(CDATA))
				throw new XMLException(cp.getPosition(), "Invalid XML");
			readCData();
		} else if (c == 'D') {
			// DOCTYPE ?
			if (!readExpectedChars(OCTYPE))
				throw new XMLException(cp.getPosition(), "Invalid XML");
			readDocType();
		} else
			throw new XMLException(cp.getPosition(), "Invalid XML");
	}
	
	private static char[] YSTEM = new char[] { 'Y', 'S', 'T', 'E', 'M' };
	private static char[] UBLIC = new char[] { 'U', 'B', 'L', 'I', 'C' };
	
	private void readDocType() throws XMLException, IOException {
		reset();
		type = Type.DOCTYPE;
		text = new UnprotectedStringBuffer();
		readSpace();
		readName(text);
		char c;
		while (isSpaceChar(c = cp.nextChar()));
		if (c == 'S') {
			if (!readExpectedChars(YSTEM))
				throw new XMLException(cp.getPosition(), "Invalid XML");
			readSpace();
			system = readSystemLiteral();
			c = cp.nextChar();
			while (isSpaceChar(c)) c = cp.nextChar();
		} else if (c == 'P') {
			if (!readExpectedChars(UBLIC))
				throw new XMLException(cp.getPosition(), "Invalid XML");
			readSpace();
			publicId = readPublicIDLiteral();
			readSpace();
			system = readSystemLiteral();
			c = cp.nextChar();
			while (isSpaceChar(c)) c = cp.nextChar();
		}
		if (c == '[') {
			readIntSubset();
			c = cp.nextChar();
			while (isSpaceChar(c)) c = cp.nextChar();
		}
		if (c != '>')
			throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
	}
	
	private void readComment() throws XMLException, IOException {
		reset();
		type = Type.COMMENT;
		text = new UnprotectedStringBuffer();
		do {
			char c;
			try { c = cp.nextChar(); }
			catch (EOFException e) {
				throw new XMLException(cp.getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "inside comment"));
			}
			if (c == '-') {
				try { c = cp.nextChar(); }
				catch (EOFException e) {
					throw new XMLException(cp.getPosition(),
						new LocalizableString("lc.xml.error", "Unexpected end"),
						new LocalizableString("lc.xml.error", "inside comment"));
				}
				if (c != '-') {
					text.append('-');
					text.append(c);
					continue;
				}
				do {
					try { c = cp.nextChar(); }
					catch (EOFException e) {
						throw new XMLException(cp.getPosition(),
							new LocalizableString("lc.xml.error", "Unexpected end"),
							new LocalizableString("lc.xml.error", "inside comment"));
					}
					if (c == '>')
						return;
					if (c == '-') {
						text.append('-');
						continue;
					}
					text.append('-');
					text.append('-');
					break;
				} while (true);
				continue;
			}
			text.append(c);
		} while (true);
	}
	
	private boolean readExpectedChars(char[] expected) throws IOException {
		for (int i = 0; i < expected.length; ++i)
			if (expected[i] != cp.nextChar())
				return false;
		return true;
	}
	
	private void readCData() throws XMLException, IOException {
		reset();
		type = Type.CDATA;
		text = new UnprotectedStringBuffer();
		do {
			char c;
			try { c = cp.nextChar(); }
			catch (EOFException e) {
				throw new XMLException(cp.getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "inside CDATA"));
			}
			if (c == ']') {
				try { c = cp.nextChar(); }
				catch (EOFException e) {
					throw new XMLException(cp.getPosition(),
						new LocalizableString("lc.xml.error", "Unexpected end"),
						new LocalizableString("lc.xml.error", "inside CDATA"));
				}
				if (c != ']') {
					text.append(']');
					text.append(c);
					continue;
				}
				do {
					try { c = cp.nextChar(); }
					catch (EOFException e) {
						throw new XMLException(cp.getPosition(),
							new LocalizableString("lc.xml.error", "Unexpected end"),
							new LocalizableString("lc.xml.error", "inside CDATA"));
					}
					if (c == '>')
						return;
					if (c == ']') {
						text.append(']');
						continue;
					}
					text.append(']');
					text.append(']');
					text.append(c);
					break;
				} while (true);
				continue;
			}
			text.append(c);
		} while (true);
	}
	
	private void readStartTag(char c) throws XMLException, IOException {
		reset();
		type = Type.START_ELEMENT;
		attributes = new ArrayList<>();
		text = new UnprotectedStringBuffer();
		text.append(c);
		continueReadName(text);
		do {
			while (isSpaceChar(c = cp.nextChar()));
			if (c == '>') break;
			if (c == '/') {
				if (cp.nextChar() != '>')
					throw new XMLException(cp.getPosition(), "Invalid XML");
				isClosed = true;
				break;
			}
			if (!isNameStartChar(c))
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			// attribute name
			UnprotectedStringBuffer attrName = new UnprotectedStringBuffer();
			attrName.append(c);
			continueReadName(attrName);
			// equal
			while (isSpaceChar(c = cp.nextChar()));
			if (c != '=')
				throw new XMLException(cp.getPosition(), "Expected character", Character.valueOf(c), Character.valueOf('='));
			// attribute value
			while (isSpaceChar(c = cp.nextChar()));
			UnprotectedStringBuffer attrValue = new UnprotectedStringBuffer();
			if (c == '"' || c == '\'') readAttrValue(attrValue, c);
			else throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			attributes.add(new Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>(attrName, attrValue));
		} while (true);
		if (onElement != null)
			onElement.fire(this);
	}
	
	private void readProcessingInstruction() throws XMLException, IOException {
		reset();
		type = Type.PROCESSING_INSTRUCTION;
		attributes = new ArrayList<>();
		text = new UnprotectedStringBuffer();
		readName(text);
		char c;
		do {
			while (isSpaceChar(c = cp.nextChar()));
			if (c == '?') {
				c = cp.nextChar();
				if (c == '>') {
					isClosed = true;
					return;
				}
				cp.back(c);
				continue;
			}
			if (!isNameStartChar(c))
				continue;
			// attribute name
			UnprotectedStringBuffer attrName = new UnprotectedStringBuffer();
			attrName.append(c);
			continueReadName(attrName);
			// equal
			while (isSpaceChar(c = cp.nextChar()));
			if (c != '=') {
				// empty attribute
				attributes.add(new Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>(attrName, new UnprotectedStringBuffer()));
				cp.back(c);
				continue;
			}
			// attribute value
			while (isSpaceChar(c = cp.nextChar()));
			UnprotectedStringBuffer attrValue = new UnprotectedStringBuffer();
			if (c == '"' || c == '\'') readAttrValue(attrValue, c);
			else throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			attributes.add(new Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>(attrName, attrValue));
		} while (true);
	}
	
	private void readAttrValue(UnprotectedStringBuffer value, char quote) throws XMLException, IOException {
		do {
			char c = cp.nextChar();
			if (c == quote) break;
			if (c == '<') throw new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected character", Character.valueOf(c)),
				new LocalizableString("lc.xml.error", "in attribute value"));
			if (c == '&') value.append(readReference());
			else if (c == '\n') value.append(' ');
			else if (c == '\r') continue;
			else value.append(c);
		} while (true);
	}
	
	private void readChars(char c) throws XMLException, IOException {
		reset();
		type = Type.TEXT;
		text = new UnprotectedStringBuffer();
		do {
			if (c == '&')
				text.append(readReference());
			else if (c == '<') {
				cp.back(c);
				break;
			} else {
				if (c == '\r') {
					try {
						c = cp.nextChar();
					} catch (EOFException e) {
						text.append(c);
						break;
					}
					if (c == '\n')
						text.append(c);
					else {
						text.append('\r');
						continue;
					}
				} else
					text.append(c);
			}
			try {
				c = cp.nextChar();
			} catch (EOFException e) {
				break;
			}
		} while (true);
	}
	
	private CharSequence readReference() throws XMLException, IOException {
		char c = cp.nextChar();
		if (c == '#') return new UnprotectedString(readCharRef());
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		name.append(c);
		continueReadName(name);
		c = cp.nextChar();
		if (c != ';')
			throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
		return resolveEntityRef(name);
	}
	
	private char[] readCharRef() throws XMLException, IOException {
		char c = cp.nextChar();
		INumberEncoding n;
		if (c == 'x') {
			n = new HexadecimalNumber();
		} else {
			n = new DecimalNumber();
			if (!n.addChar(c))
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
		}
		do {
			c = cp.nextChar();
			if (c == ';') break;
			if (!n.addChar(c)) throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
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
			//throw new XMLException(cp.getPosition(), "Invalid XML entity", name.toString());
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
		//throw new XMLException(cp.getPosition(), "Invalid XML entity", name.toString());
	}
	
	private UnprotectedStringBuffer readSystemLiteral() throws XMLException, IOException {
		char c = cp.nextChar();
		if (c != '"') throw new XMLException(cp.getPosition(), "Expected character", Character.valueOf(c), Character.valueOf('"'));
		UnprotectedStringBuffer literal = new UnprotectedStringBuffer();
		do {
			c = cp.nextChar();
			if (c == '"') break;
			literal.append(c);
		} while (true);
		return literal;
	}

	private UnprotectedStringBuffer readPublicIDLiteral() throws XMLException, IOException {
		char quote = cp.nextChar();
		if (quote != '"' && quote != '\'') throw new XMLException(cp.getPosition(), "Expected character", Character.valueOf(quote), "\",\'");
		UnprotectedStringBuffer literal = new UnprotectedStringBuffer();
		do {
			char c = cp.nextChar();
			if (c == quote) break;
			literal.append(c);
		} while (true);
		return literal;
	}
	
	private void readEndTag() throws XMLException, IOException {
		reset();
		type = Type.END_ELEMENT;
		text = new UnprotectedStringBuffer();
		readName(text);
		do {
			char c = cp.nextChar();
			if (c == '>') return;
			if (!isSpaceChar(c))
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
		} while (true);
	}
	
	// skip checkstyle: VariableDeclarationUsageDistance
	private void readIntSubset() throws XMLException, IOException {
		UnprotectedStringBuffer saveText = text;
		Type saveType = type;
		ArrayList<Pair<UnprotectedStringBuffer,UnprotectedStringBuffer>> saveAttributes = attributes;
		
		do {
			char c;
			try { c = cp.nextChar(); }
			catch (EOFException e) {
				throw new XMLException(cp.getPosition(),
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
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			readIntSubsetTag();
		} while (true);
		
		text = saveText;
		type = saveType;
		attributes = saveAttributes;
	}
	
	private void readIntSubsetPEReference() throws XMLException, IOException {
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		try {
			do {
				char c = cp.nextChar();
				if (isSpaceChar(c)) continue;
				if (c == ';') break;
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			} while (true);
		} catch (EOFException e) {
			throw new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in internal subset declaration"));
		}
	}
	
	private void readIntSubsetTag() throws XMLException, IOException {
		try {
			char c = cp.nextChar();
			if (c == '!') {
				readIntSubsetTagExclamation();
			} else if (c == '?') {
				readProcessingInstruction();
			} else {
				throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
			}
		} catch (EOFException e) {
			throw new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in XML document"));
		}
	}
	
	private void readIntSubsetTagExclamation() throws XMLException, IOException {
		// can be a comment, a CDATA, or a DOCTYPE
		char c = cp.nextChar();
		if (c == '-') {
			// comment ?
			c = cp.nextChar();
			if (c == '-') {
				readComment();
				return;
			}
			throw new XMLException(cp.getPosition(), "Invalid XML");
		} else if (c == 'E') {
			c = cp.nextChar();
			if (c == 'L') {
				if (cp.nextChar() == 'E' &&
					cp.nextChar() == 'M' &&
					cp.nextChar() == 'E' &&
					cp.nextChar() == 'N' &&
					cp.nextChar() == 'T' &&
					isSpaceChar(cp.nextChar())) {
					readElementDeclaration();
					return;
				}
			} else if (c == 'N') {
				if (cp.nextChar() == 'T' &&
					cp.nextChar() == 'I' &&
					cp.nextChar() == 'T' &&
					cp.nextChar() == 'Y' &&
					isSpaceChar(cp.nextChar())) {
					readEntityDeclaration();
					return;
				}
			}
		} else if (c == 'A') {
			if (cp.nextChar() == 'T' &&
				cp.nextChar() == 'T' &&
				cp.nextChar() == 'L' &&
				cp.nextChar() == 'I' &&
				cp.nextChar() == 'S' &&
				cp.nextChar() == 'T' &&
				isSpaceChar(cp.nextChar())) {
				readAttListDeclaration();
				return;
			}
		} else if (c == 'N') {
			if (cp.nextChar() == 'O' &&
				cp.nextChar() == 'T' &&
				cp.nextChar() == 'A' &&
				cp.nextChar() == 'T' &&
				cp.nextChar() == 'I' &&
				cp.nextChar() == 'O' &&
				cp.nextChar() == 'N' &&
				isSpaceChar(cp.nextChar())) {
				readNotationDeclaration();
				return;
			}
		}
		throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
	}
	
	private void readElementDeclaration() throws XMLException, IOException {
		char c = cp.nextChar();
		while (isSpaceChar(c)) c = cp.nextChar();
		cp.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = cp.nextChar();
		while (c != '>') c = cp.nextChar();
	}

	private void readEntityDeclaration() throws IOException {
		char c = cp.nextChar();
		while (isSpaceChar(c)) c = cp.nextChar();
		boolean inString = false;
		do {
			c = cp.nextChar();
			if (inString) {
				if (c == '"') inString = false;
				continue;
			}
			if (c == '"') inString = true;
			else if (c == '>') break;
		} while (true);
	}
	
	private void readAttListDeclaration() throws XMLException, IOException {
		char c = cp.nextChar();
		while (isSpaceChar(c)) c = cp.nextChar();
		cp.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = cp.nextChar();
		while (c != '>') c = cp.nextChar();
	}
	
	private void readNotationDeclaration() throws XMLException, IOException {
		char c = cp.nextChar();
		while (isSpaceChar(c)) c = cp.nextChar();
		cp.back(c);
		UnprotectedStringBuffer name = new UnprotectedStringBuffer();
		readName(name);
		c = cp.nextChar();
		while (c != '>') c = cp.nextChar();
	}

	
	
	private void readName(UnprotectedStringBuffer name) throws XMLException, IOException {
		char c = cp.nextChar();
		if (!isNameStartChar(c)) throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
		name.append(c);
		continueReadName(name);
	}
	
	private void continueReadName(UnprotectedStringBuffer name) throws IOException {
		do {
			char c = cp.nextChar();
			if (!isNameChar(c)) {
				cp.back(c);
				return;
			}
			name.append(c);
		} while (true);
	}
	
	private void readSpace() throws XMLException, IOException {
		char c = cp.nextChar();
		if (!isSpaceChar(c)) throw new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c));
		do {
			c = cp.nextChar();
			if (!isSpaceChar(c)) {
				cp.back(c);
				return;
			}
		} while (true);
	}
	

	private static boolean[] isSpace = new boolean[] { true, true, false, false, true };
	
	private static boolean isSpaceChar(char c) {
		if (c == 0x20) return true;
		if (c > 0xD) return false;
		if (c < 0x9) return false;
		return isSpace[c - 9];
	}
	
	private static boolean isNameStartChar(char c) {
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

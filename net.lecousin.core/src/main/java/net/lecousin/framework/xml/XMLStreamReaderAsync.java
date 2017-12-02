package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
public class XMLStreamReaderAsync {

	/** Initialize with the given IO.
	 * If it is not buffered, a {@link PreBufferedReadable} is created.
	 * If the charset is null, it will be automatically detected.
	 */
	public XMLStreamReaderAsync(IO.Readable io, Charset forcedEncoding, int charactersBuffersSize) {
		if (io instanceof IO.Readable.Buffered)
			this.io = (IO.Readable.Buffered)io;
		else
			this.io = new PreBufferedReadable(io, 1024, io.getPriority(), charactersBuffersSize, (byte)(io.getPriority() - 1), 4);
		this.forcedCharset = forcedEncoding;
		this.charactersBuffersSize = charactersBuffersSize;
	}
	
	/** Constructor, without charset (automatic detection). */
	public XMLStreamReaderAsync(IO.Readable io, int charactersBuffersSize) {
		this(io, null, charactersBuffersSize);
	}
	
	private Charset forcedCharset;
	private int charactersBuffersSize;
	private int maxTextSize = -1;
	private int maxCDataSize = -1;
	
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
	/** On START_ELEMENT or END_ELEMENT events, it contains the local name part of the text. */
	public UnprotectedStringBuffer localName = null;
	/** When type if START_ELEMENT, it specifies if it is an empty-element. */
	public boolean isClosed = false;
	/** When type if START_ELEMENT, it contains the list of attributes. */
	public LinkedList<Attribute> attributes = null;
	/** System specified in the DOCTYPE tag. */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public UnprotectedStringBuffer system = null;
	/** Public ID specified in the DOCTYPE tag. */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
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
	/** Allows to specify a listener that will be called on each START_ELEMENT event. */
	@SuppressFBWarnings("UWF_NULL_FIELD")
	public Listener<XMLStreamReaderAsync> onElement = null;
	
	public static class ElementContext {
		public UnprotectedStringBuffer text;
		public UnprotectedStringBuffer namespacePrefix;
		public UnprotectedStringBuffer localName;
		public List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> namespaces = new LinkedList<>();
	}
	
	public static class Attribute {
		public UnprotectedStringBuffer text;
		public UnprotectedStringBuffer namespacePrefix;
		public UnprotectedStringBuffer localName;
		public UnprotectedStringBuffer value;
	}
	
	public int getMaximumTextSize() {
		return maxTextSize;
	}
	
	public void setMaximumTextSize(int max) {
		maxTextSize = max;
	}
	
	public int getMaximumCDataSize() {
		return maxCDataSize;
	}
	
	public void setMaximumCDataSize(int max) {
		maxCDataSize = max;
	}
	
	public UnprotectedStringBuffer getNamespaceURI(CharSequence namespacePrefix) {
		for (ElementContext ctx : context) {
			for (Pair<UnprotectedStringBuffer, UnprotectedStringBuffer> ns : ctx.namespaces)
				if (ns.getValue1().equals(namespacePrefix))
					return ns.getValue2();
		}
		return new UnprotectedStringBuffer();
	}
	
	/** Shortcut to get the attribute for the given full name. */
	public Attribute getAttributeByFullName(CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.text.equals(name))
				return attr;
		return null;
	}
	
	/** Shortcut to get the attibute for the given local name. */
	public Attribute getAttributeByLocalName(CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name))
				return attr;
		return null;
	}

	/** Shortcut to get the attribute for the given namespace prefix and local name. */
	public Attribute getAttributeWithPrefix(CharSequence prefix, CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name) && attr.namespacePrefix.equals(prefix))
				return attr;
		return null;
	}
	
	/** Shortcut to get the attribute for the given namespace URI and local name. */
	public Attribute getAttributeWithNamespaceURI(CharSequence uri, CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri))
				return attr;
		return null;
	}

	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueByFullName(CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.text.equals(name))
				return attr.value;
		return null;
	}
	
	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueByLocalName(CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name))
				return attr.value;
		return null;
	}

	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueWithPrefix(CharSequence prefix, CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name) && attr.namespacePrefix.equals(prefix))
				return attr.value;
		return null;
	}
	
	/** Shortcut to get the value of the given attribute name. */
	public UnprotectedStringBuffer getAttributeValueWithNamespaceURI(CharSequence uri, CharSequence name) {
		for (Attribute attr : attributes)
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri))
				return attr.value;
		return null;
	}
	
	/** Remove and return the attribute for the given full name if it exists. */
	public Attribute removeAttributeByFullName(CharSequence name) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
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
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
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
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
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
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
			Attribute attr = it.next();
			if (attr.localName.equals(name) && getNamespaceURI(attr.namespacePrefix).equals(uri)) {
				it.remove();
				return attr;
			}
		}
		return null;
	}
	
	private void reset() {
		if (Type.START_ELEMENT.equals(type) && isClosed)
			context.removeFirst();
		else if (Type.END_ELEMENT.equals(type))
			context.removeFirst();
		type = null;
		text = null;
		namespacePrefix = null;
		localName = null;
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
		
		public ISynchronizationPoint<IOException> onCharAvailable() {
			if (back != -1 || pos >= 0)
				return new SynchronizationPoint<>(true);
			return charAvailable();
		}
		
		protected abstract ISynchronizationPoint<IOException> charAvailable();
		
		public final int nextChar() throws IOException {
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
		
		protected abstract int getNextChar() throws IOException;
		
		private int updatePos(int c) {
			if (c < 0) return c;
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
		protected int firstChar = -1;
		protected int secondChar = -1;
		protected int thirdChar = -1;
		
		@Override
		protected ISynchronizationPoint<IOException> charAvailable() {
			if (nextUTF8 != -1)
				return new SynchronizationPoint<>(true);
			return io.canStartReading();
		}
		
		@Override
		protected int getNextChar() throws IOException {
			if (nextUTF8 != -1) {
				int c = nextUTF8;
				nextUTF8 = -1;
				return c;
			}
			int c;
			if (firstChar == -1) {
				c = io.readAsync();
				if (c < 0) return c;
			} else {
				c = firstChar;
				firstChar = -1;
			}
			// UTF-8 decoding by default
			if (c < 0x80)
				return (char)c;
			int up = c & 0xF0;
			if (up == 0xC0) {
				// two bytes
				int c2;
				if (secondChar == -1)
					c2 = io.readAsync();
				else {
					c2 = secondChar;
					secondChar = -1;
				}
				if (c2 == -1) return (char)c;
				if (c2 == -2) {
					firstChar = c;
					return -2;
				}
				if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				return (char)((c2 & 0x3F) | ((c & 0x1F) << 6));
			}
			if (up == 0xE0) {
				// 3 bytes
				int c2;
				if (secondChar == -1)
					c2 = io.readAsync();
				else {
					c2 = secondChar;
					secondChar = -1;
				}
				if (c2 == -1) return (char)c;
				if (c2 == -2) {
					firstChar = c;
					return -2;
				}
				int c3;
				if (thirdChar == -1)
					c3 = io.readAsync();
				else {
					c3 = thirdChar;
					thirdChar = -1;
				}
				if (c3 == -1) throw new IOException("Invalid UTF-8 character");
				if (c3 == -2) {
					firstChar = c;
					secondChar = c2;
					return -2;
				}
				if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				if ((c3 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
				return (char)((c3 & 0x3F) | ((c2 & 0x3F) << 6) | ((c & 0xF) << 12));
			}
			// 4 bytes
			if ((c & 0xF8) != 0xF0) throw new IOException("Invalid UTF-8 character");
			int c2;
			if (secondChar == -1)
				c2 = io.readAsync();
			else {
				c2 = secondChar;
				secondChar = -1;
			}
			if (c2 == -1) return (char)c;
			if (c2 == -2) {
				firstChar = c;
				return -2;
			}
			if ((c2 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
			int c3;
			if (thirdChar == -1)
				c3 = io.readAsync();
			else {
				c3 = thirdChar;
				thirdChar = -1;
			}
			if (c3 == -1) throw new IOException("Invalid UTF-8 character");
			if (c3 == -2) {
				firstChar = c;
				secondChar = c2;
				return -2;
			}
			if ((c3 & 0xC0) != 0x80) throw new IOException("Invalid UTF-8 character");
			int c4 = io.readAsync();
			if (c4 == -1) throw new IOException("Invalid UTF-8 character");
			if (c4 == -2) {
				firstChar = c;
				secondChar = c2;
				thirdChar = c3;
				return -2;
			}
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
		protected int getNextChar() throws IOException {
			return stream.readAsync();
		}
		
		@Override
		protected ISynchronizationPoint<IOException> charAvailable() {
			return stream.canStartReading();
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
	
	/** Start reading the XML to provide the first event.
	 * If the first tag is a processing instruction XML it reads it and goes to the next event.
	 */
	public ISynchronizationPoint<Exception> start() {
		try {
			initCharacterProvider();
		} catch (EOFException e) {
			return new SynchronizationPoint<>(new XMLException(null, "Invalid XML"));
		} catch (Exception e) {
			return new SynchronizationPoint<>(e);
		}
		ISynchronizationPoint<Exception> next = next();
		if (next.isUnblocked()) {
			if (next.hasError()) return next;
			if (checkFirstElement()) return next();
			return next;
		}
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		next.listenAsync(new ParsingTask(() -> {
			if (next.hasError()) result.error(next.getError());
			else if (!checkFirstElement()) result.unblock();
			else next().listenInline(result);
		}), result);
		return result;
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
							UnprotectedStringBuffer encoding = getAttributeValueByFullName("encoding");
							if (encoding != null)
								cp = new CharacterStreamProvider(Charset.forName(encoding.asString()));
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/** Move forward to the next event.
	 * If the next event can be read synchronously, the result is unblocked, else the caller has to wait for it.
	 */
	public SynchronizationPoint<Exception> next() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		reset();
		state = State.START;
		nextChar(sp);
		return sp;
	}
	
	private class ParsingTask extends Task.Cpu<Void, NoException> {
		public ParsingTask(Runnable r) {
			super("Parse XML", io.getPriority());
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() {
			r.run();
			return null;
		}
	}
	
	private class Next extends ParsingTask {
		public Next(SynchronizationPoint<Exception> sp) {
			super(null);
			this.sp = sp;
		}
		
		protected SynchronizationPoint<Exception> sp;
		
		@Override
		public Void run() {
			ISynchronizationPoint<Exception> next = next();
			if (next.isUnblocked()) {
				if (next.hasError()) sp.error(next.getError());
				else onNext();
				return null;
			}
			next.listenAsync(new Task.Cpu<Void, NoException>("Parse XML", getPriority()) {
				@Override
				public Void run() {
					onNext();
					return null;
				}
			}, sp);
			return null;
		}
		
		protected void onNext() {
			sp.unblock();
		}
	}
	
	/* Public utility methods */
	
	// TODO optimization: when searching for something, do not load every information in between
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public ISynchronizationPoint<Exception> startRootElement() {
		ISynchronizationPoint<Exception> start = start();
		if (start.isUnblocked()) {
			if (start.hasError()) return start;
			if (Type.START_ELEMENT.equals(type)) return start;
			return nextStartElement();
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		start.listenAsync(new Next(sp) {
			@Override
			protected void onNext() {
				if (Type.START_ELEMENT.equals(type)) sp.unblock();
				else nextStartElement().listenInline(sp);
			}
		}, sp);
		return sp;
	}
	
	/** Shortcut to move forward to the next START_ELEMENT. */
	public ISynchronizationPoint<Exception> nextStartElement() {
		ISynchronizationPoint<Exception> next = next();
		if (next.isUnblocked()) {
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(type)) return next;
			return nextStartElement();
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		next.listenInline(
			() -> {
				if (Type.START_ELEMENT.equals(type)) {
					sp.unblock();
					return;
				}
				new Next(sp) {
					@Override
					protected void onNext() {
						if (Type.START_ELEMENT.equals(type)) sp.unblock();
						else nextStartElement().listenInline(sp);
					}
				}.start();
			},
			sp
		);
		return sp;
	}
	
	/** Go to the next inner element. The result is false if the parent element has been closed. */
	public AsyncWork<Boolean, Exception> nextInnerElement() {
		if (Type.START_ELEMENT.equals(type) && isClosed)
			return new AsyncWork<>(Boolean.FALSE, null);
		ISynchronizationPoint<Exception> next = next();
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) return new AsyncWork<>(null, next.getError());
				if (Type.END_ELEMENT.equals(type)) return new AsyncWork<>(Boolean.FALSE, null);
				if (Type.START_ELEMENT.equals(type)) return new AsyncWork<>(Boolean.TRUE, null);
				next = next();
				continue;
			}
			break;
		} while (true);
		AsyncWork<Boolean, Exception> result = new AsyncWork<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (Type.END_ELEMENT.equals(type)) result.unblockSuccess(Boolean.FALSE);
				else if (Type.START_ELEMENT.equals(type)) result.unblockSuccess(Boolean.TRUE);
				else new ParsingTask(() -> {
					nextInnerElement().listenInline(result);
				}).start();
			}, result
		);
		return result;
	}
	
	/** Go to the next inner element having the given name. The result is false if the parent element has been closed. */
	public AsyncWork<Boolean, Exception> nextInnerElement(String childName) {
		AsyncWork<Boolean, Exception> next = nextInnerElement();
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) return next;
				if (!next.getResult().booleanValue()) return next;
				if (text.equals(childName)) return next;
				next = nextInnerElement();
				continue;
			}
			break;
		} while (true);
		AsyncWork<Boolean, Exception> result = new AsyncWork<>();
		AsyncWork<Boolean, Exception> n = next;
		next.listenInline(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (!n.getResult().booleanValue()) result.unblockSuccess(Boolean.FALSE);
				else if (text.equals(childName)) result.unblockSuccess(Boolean.TRUE);
				else new ParsingTask(() -> {
					nextInnerElement(childName).listenInline(result);
				}).start();
			}, result
		);
		return result;
	}
	
	/** Read inner text and close element. */
	public AsyncWork<UnprotectedStringBuffer, Exception> readInnerText() {
		if (!Type.START_ELEMENT.equals(type))
			return new AsyncWork<>(null, new Exception("Invalid call of readInnerText: it must be called on a start element"));
		if (isClosed)
			return new AsyncWork<>(new UnprotectedStringBuffer(), null);
		UnprotectedStringBuffer innerText = new UnprotectedStringBuffer();
		AsyncWork<UnprotectedStringBuffer, Exception> result = new AsyncWork<>();
		readInnerText(innerText, result);
		return result;
	}
	
	private void readInnerText(UnprotectedStringBuffer innerText, AsyncWork<UnprotectedStringBuffer, Exception> result) {
		ISynchronizationPoint<Exception> next = next();
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				if (Type.COMMENT.equals(type)) {
					next = next();
					continue;
				}
				if (Type.TEXT.equals(type)) {
					innerText.append(text);
					next = next();
					continue;
				}
				if (Type.START_ELEMENT.equals(type)) {
					if (isClosed) {
						next = next();
						continue;
					}
					closeElement().listenAsync(new ParsingTask(() -> {
						readInnerText(innerText, result);
					}), result);
					return;
				}
				if (Type.END_ELEMENT.equals(type)) {
					result.unblockSuccess(innerText);
					return;
				}
				next = next();
				continue;
			}
			break;
		} while (true);
		next.listenInline(() -> {
			if (Type.START_ELEMENT.equals(type)) {
				if (isClosed) {
					new ParsingTask(() -> { readInnerText(innerText, result); }).start();
					return;
				}
				closeElement().listenAsync(new ParsingTask(() -> {
					readInnerText(innerText, result);
				}), result);
				return;
			}
			if (Type.COMMENT.equals(type)) {
				new ParsingTask(() -> { readInnerText(innerText, result); }).start();
				return;
			}
			if (Type.TEXT.equals(type)) {
				innerText.append(text);
				new ParsingTask(() -> { readInnerText(innerText, result); }).start();
				return;
			}
			if (Type.END_ELEMENT.equals(type)) {
				result.unblockSuccess(innerText);
				return;
			}
			new ParsingTask(() -> { readInnerText(innerText, result); }).start();
		}, result);
	}
	
	/** Go the the END_ELEMENT event corresponding to the current START_ELEMENT (must be called with a current event to START_ELEMENT). */
	public ISynchronizationPoint<Exception> closeElement() {
		if (!Type.START_ELEMENT.equals(type))
			return new SynchronizationPoint<>(new Exception("Invalid call of closeElement: it must be called on a start element"));
		if (isClosed)
			return new SynchronizationPoint<>(true);
		ISynchronizationPoint<Exception> next = next();
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			if (Type.END_ELEMENT.equals(type)) return next;
			next = next();
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (Type.END_ELEMENT.equals(type)) result.unblock();
			else new ParsingTask(() -> { closeElement().listenInline(result); }).start();
		}, result);
		return result;
	}

	/** Move forward until an element with the given name is found (whatever its depth).
	 */
	public ISynchronizationPoint<Exception> searchElement(String elementName) {
		ISynchronizationPoint<Exception> next = next();
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(type) && text.equals(elementName)) return next;
			next = next();
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (Type.START_ELEMENT.equals(type) && text.equals(elementName)) result.unblock();
			else new ParsingTask(() -> { searchElement(elementName).listenInline(result); }).start();
		}, result);
		return result;
	}
	
	/** Go successively into the given elements. */
	public ISynchronizationPoint<Exception> goInto(String... innerElements) {
		return goInto(0, innerElements);
	}
	
	private ISynchronizationPoint<Exception> goInto(int i, String... innerElements) {
		ISynchronizationPoint<Exception> next = nextInnerElement(innerElements[0]);
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			i++;
			if (i == innerElements.length) return next;
			next = nextInnerElement(innerElements[i]);
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		int ii = i;
		next.listenInline(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (ii == innerElements.length - 1) result.unblock();
			else new ParsingTask(() -> { goInto(ii + 1, innerElements).listenInline(result); }).start();
		}, result);
		return result;
	}
	
	/** Read all inner elements with their text, and return a mapping with the element's name as key and inner text as value. */
	public AsyncWork<Map<String,String>, Exception> readInnerElementsText() {
		AsyncWork<Map<String,String>, Exception> result = new AsyncWork<>();
		Map<String, String> texts = new HashMap<>();
		readInnerElementsText(texts, result);
		return result;
	}
	
	private void readInnerElementsText(Map<String, String> texts, AsyncWork<Map<String,String>, Exception> result) {
		AsyncWork<Boolean, Exception> next = nextInnerElement();
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) {
				result.error(next.getError());
				return;
			}
			if (!next.getResult().booleanValue()) {
				result.unblockSuccess(texts);
				return;
			}
			String name = text.asString();
			AsyncWork<UnprotectedStringBuffer, Exception> read = readInnerText();
			if (read.isUnblocked()) {
				if (read.hasError()) {
					result.error(read.getError());
					return;
				}
				texts.put(name, read.getResult().asString());
				next = nextInnerElement();
				continue;
			}
			read.listenInline((value) -> {
				texts.put(name,  value.asString());
				new ParsingTask(() -> { readInnerElementsText(texts, result); }).start();
			}, result);
			return;
		} while (true);
		next.listenInline(() -> {
			String name = text.asString();
			new ParsingTask(() -> {
				AsyncWork<UnprotectedStringBuffer, Exception> read = readInnerText();
				if (read.isUnblocked()) {
					if (read.hasError()) {
						result.error(read.getError());
						return;
					}
					texts.put(name, read.getResult().asString());
					readInnerElementsText(texts, result);
					return;
				}
				read.listenInline((value) -> {
					texts.put(name,  value.asString());
					new ParsingTask(() -> { readInnerElementsText(texts, result); }).start();
				}, result);
			}).start();
		}, result);
	}
	
	/* Private methods */
	
	private static enum State {
		START,
		TAG,
		TAG_EXCLAMATION,
		PROCESSING_INSTRUCTION,
		PROCESSING_INSTRUCTION_CLOSED,
		END_ELEMENT_NAME,
		START_ELEMENT_NAME,
		START_ELEMENT_SPACE,
		START_ELEMENT_CLOSED,
		ATTRIBUTE_NAME,
		ATTRIBUTE_NAME_SPACE,
		ATTRIBUTE_VALUE_SPACE,
		ATTRIBUTE_VALUE,
		COMMENT,
		CDATA,
		DOCTYPE,
		DOCTYPE_NAME,
		DOCTYPE_SPACE,
		DOCTYPE_PUBLIC_NAME,
		DOCTYPE_PUBLIC_SPACE,
		DOCTYPE_PUBLIC_ID,
		DOCTYPE_SYSTEM_NAME,
		DOCTYPE_SYSTEM_SPACE,
		DOCTYPE_SYSTEM_VALUE,
		DOCTYPE_INTERNAL_SUBSET,
		DOCTYPE_INTERNAL_SUBSET_PEREFERENCE,
		DOCTYPE_INTERNAL_SUBSET_TAG,
		CHARS,
	}
	
	private State state;
	private int statePos = 0;

	private void nextChar(SynchronizationPoint<Exception> sp) {
		do {
			int c;
			try { c = cp.nextChar(); }
			catch (IOException e) {
				sp.error(e);
				return;
			}
			if (c == -2) {
				cp.onCharAvailable().listenAsync(new ParsingTask(() -> { nextChar(sp); }), true);
				return;
			}
			switch (state) {
			case START:
				// start a new event
				if (c == -1) {
					// end of stream
					sp.error(new EOFException());
					return;
				}
				if (c == '<') {
					// start a tag
					state = State.TAG;
					break;
				}
				// start characters
				state = State.CHARS;
				type = Type.TEXT;
				text = new UnprotectedStringBuffer();
				if (!readChars(c, sp))
					return;
				break;
				
			case TAG:
				if (!readTag(c, sp))
					return;
				break;
			
			case TAG_EXCLAMATION:
				if (!readTagExclamation(c, sp))
					return;
				break;
				
			case PROCESSING_INSTRUCTION:
				if (!readProcessingInstruction(c, sp))
					return;
				break;
				
			case PROCESSING_INSTRUCTION_CLOSED:
				if (!readProcessingInstructionClosed(c, sp))
					return;
				break;
				
			case END_ELEMENT_NAME:
				if (!readEndElementName(c, sp))
					return;
				break;
				
			case START_ELEMENT_NAME:
				if (!readStartElementName(c, sp))
					return;
				break;
				
			case START_ELEMENT_SPACE:
				if (!readStartElementSpace(c, sp))
					return;
				break;
				
			case START_ELEMENT_CLOSED:
				if (!readStartElementClosed(c, sp))
					return;
				break;
				
			case ATTRIBUTE_NAME:
				if (!readAttributeName(c, sp))
					return;
				break;
				
			case ATTRIBUTE_NAME_SPACE:
				if (!readAttributeNameSpace(c, sp))
					return;
				break;
				
			case ATTRIBUTE_VALUE_SPACE:
				if (!readAttributeValueSpace(c, sp))
					return;
				break;
				
			case ATTRIBUTE_VALUE:
				if (!readAttributeValue(c, sp))
					return;
				break;
				
			case COMMENT:
				if (!readComment(c, sp))
					return;
				break;
				
			case CDATA:
				if (!readCData(c, sp))
					return;
				break;
				
			case DOCTYPE:
				if (!readDocType(c, sp))
					return;
				break;
				
			case DOCTYPE_NAME:
				if (!readDocTypeName(c, sp))
					return;
				break;

			case DOCTYPE_SPACE:
				if (!readDocTypeSpace(c, sp))
					return;
				break;

			case DOCTYPE_PUBLIC_NAME:
				if (!readDocTypePublicName(c, sp))
					return;
				break;

			case DOCTYPE_PUBLIC_SPACE:
				if (!readDocTypePublicSpace(c, sp))
					return;
				break;

			case DOCTYPE_PUBLIC_ID:
				if (!readDocTypePublicId(c, sp))
					return;
				break;

			case DOCTYPE_SYSTEM_NAME:
				if (!readDocTypeSystemName(c, sp))
					return;
				break;

			case DOCTYPE_SYSTEM_SPACE:
				if (!readDocTypeSystemSpace(c, sp))
					return;
				break;

			case DOCTYPE_SYSTEM_VALUE:
				if (!readDocTypeSystemValue(c, sp))
					return;
				break;

			case DOCTYPE_INTERNAL_SUBSET:
				if (!readInternalSubset(c, sp))
					return;
				break;

			case DOCTYPE_INTERNAL_SUBSET_PEREFERENCE:
				if (!readInternalSubsetPEReference(c, sp))
					return;
				break;

			case DOCTYPE_INTERNAL_SUBSET_TAG:
				if (!readInternalSubsetTag(c, sp))
					return;
				break;
				
			case CHARS:
				if (!readChars(c, sp))
					return;
				break;

			default:
				sp.error(new Exception("Unexpected state " + state));
				return;
			}
		} while (true);
	}
	
	private boolean readTag(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (c == '!') {
			state = State.TAG_EXCLAMATION;
			text = new UnprotectedStringBuffer();
			return true;
		}
		if (c == '?') {
			state = State.PROCESSING_INSTRUCTION;
			text = new UnprotectedStringBuffer();
			attributes = new LinkedList<>();
			type = Type.PROCESSING_INSTRUCTION;
			return true;
		}
		if (c == '/') {
			state = State.END_ELEMENT_NAME;
			text = new UnprotectedStringBuffer();
			return true;
		}
		if (isNameStartChar((char)c)) {
			state = State.START_ELEMENT_NAME;
			type = Type.START_ELEMENT;
			attributes = new LinkedList<>();
			text = new UnprotectedStringBuffer();
			text.append((char)c);
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readTagExclamation(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		// can be a comment, a CDATA, or a DOCTYPE
		if (text.length() == 0) {
			if (c == '-') {
				// can be a comment
				text.append((char)c);
				return true;
			}
			if (c == '[') {
				// can be CDATA
				text.append((char)c);
				return true;
			}
			if (c == 'D') {
				// can be DOCTYPE
				text.append((char)c);
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		char c1 = text.charAt(0);
		// comment
		if (c1 == '-') {
			if (c == '-') {
				// this is a comment
				state = State.COMMENT;
				type = Type.COMMENT;
				text = new UnprotectedStringBuffer();
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		// CDATA
		if (c1 == '[') {
			text.append((char)c);
			if (text.length() < 7) {
				if (text.isStartOf("[CDATA["))
					return true;
				sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
				return false;
			}
			if (!text.equals("[CDATA[")) {
				sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
				return false;
			}
			state = State.CDATA;
			type = Type.CDATA;
			text = new UnprotectedStringBuffer();
			return true;
		}
		// DOCTYPE
		if (text.length() < 7) {
			text.append((char)c);
			if (text.isStartOf("DOCTYPE"))
				return true;
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		state = State.DOCTYPE;
		type = Type.DOCTYPE;
		text = new UnprotectedStringBuffer();
		return true;
	}
	
	private boolean readDocType(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c)) return true;
		if (!isNameStartChar((char)c)) {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		text.append((char)c);
		state = State.DOCTYPE_NAME;
		return true;
	}
	
	private boolean readDocTypeName(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (isNameChar((char)c)) {
			text.append((char)c);
			return true;
		}
		if (isSpaceChar((char)c)) {
			state = State.DOCTYPE_SPACE;
			return true;
		}
		if (c == '[') {
			state = State.DOCTYPE_INTERNAL_SUBSET;
			return true;
		}
		if (c == '>') {
			sp.unblock();
			return false;
		}
		if (c == 'P') {
			state = State.DOCTYPE_PUBLIC_NAME;
			statePos = 1;
			return true;
		}
		if (c == 'S') {
			state = State.DOCTYPE_SYSTEM_NAME;
			statePos = 1;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypeSpace(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c))
			return true;
		if (c == '[') {
			state = State.DOCTYPE_INTERNAL_SUBSET;
			return true;
		}
		if (c == '>') {
			sp.unblock();
			return false;
		}
		if (c == 'P') {
			state = State.DOCTYPE_PUBLIC_NAME;
			statePos = 1;
			return true;
		}
		if (c == 'S') {
			state = State.DOCTYPE_SYSTEM_NAME;
			statePos = 1;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypePublicName(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (statePos < 6) {
			if (c != "PUBLIC".charAt(statePos)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
				return false;
			}
			statePos++;
			return true;
		}
		if (publicId.length() > 0) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Invalid XML")));
			return false;
		}			
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_PUBLIC_SPACE;
		return true;
	}
	
	private boolean readDocTypePublicSpace(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c))
			return true;
		if (c != '"' && c != '\'') {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_PUBLIC_ID;
		statePos = c;
		return true;
	}
	
	private boolean readDocTypePublicId(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (c == statePos) {
			state = State.DOCTYPE_SYSTEM_SPACE;
			return true;
		}
		if ((c >= 'a' && c <= 'z') ||
			(c >= 'A' && c <= 'Z') ||
			(c >= '0' && c <= '9') ||
			c == ' ' || c == '\r' || c == '\n' ||
			c == '-' || c == '\'' || c == '(' || c == ')' || c == '+' || c == ',' || c == '.' ||
			c == '/' || c == ':' || c == '=' || c == '?' || c == ';' || c == '!' || c == '*' ||
			c == '#' || c == '@' || c == '$' || c == '_' || c == '%'
		) {
			publicId.append((char)c);
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypeSystemName(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (statePos < 6) {
			if (c != "SYSTEM".charAt(statePos)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
				return false;
			}
			statePos++;
			return true;
		}
		if (system.length() > 0) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Invalid XML")));
			return false;
		}			
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_SYSTEM_SPACE;
		return true;
	}

	private boolean readDocTypeSystemSpace(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c))
			return true;
		if (c != '"' && c != '\'') {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_SYSTEM_VALUE;
		statePos = c;
		return true;
	}
	
	private boolean readDocTypeSystemValue(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (c == statePos) {
			state = State.DOCTYPE_SPACE;
			return true;
		}
		system.append((char)c);
		return true;
	}
	
	private boolean readComment(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "inside comment")));
			return false;
		}
		if (maxTextSize > 0 && text.length() >= maxTextSize) {
			if (text.charAt(text.length() - 1) == '-') {
				if (text.charAt(text.length() - 2) == '-') {
					if (c == '>') {
						text.removeEndChars(2);
						sp.unblock();
						return false;
					}
					text.setCharAt(text.length() - 1, (char)c);
					return true;
				}
				if (c != '-') {
					text.setCharAt(text.length() - 1, (char)c);
					return true;
				}
				text.setCharAt(text.length() - 2, (char)c);
				return true;
			}
			text.setCharAt(text.length() - 1, (char)c);
			return true;
		}
		text.append((char)c);
		if (text.endsWith("-->")) {
			text.removeEndChars(3);
			sp.unblock();
			return false;
		}
		return true;
	}
	
	private boolean readCData(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "inside CDATA")));
			return false;
		}
		if (maxCDataSize > 0 && text.length() >= maxCDataSize) {
			if (text.charAt(text.length() - 1) == ']') {
				if (text.charAt(text.length() - 2) == ']') {
					if (c == '>') {
						text.removeEndChars(2);
						sp.unblock();
						return false;
					}
					text.setCharAt(text.length() - 1, (char)c);
					return true;
				}
				if (c != ']') {
					text.setCharAt(text.length() - 1, (char)c);
					return true;
				}
				text.setCharAt(text.length() - 2, (char)c);
				return true;
			}
			text.setCharAt(text.length() - 1, (char)c);
			return true;
		}
		text.append((char)c);
		if (text.endsWith("]]>")) {
			text.removeEndChars(3);
			sp.unblock();
			return false;
		}
		return true;
	}
	
	private boolean readStartElementName(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isNameChar(c)) {
			text.append(c);
			return true;
		}
		if (isSpaceChar(c)) {
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		if (c == '>') {
			i = text.indexOf(':');
			if (i < 0) {
				namespacePrefix = new UnprotectedStringBuffer();
				localName = text;
			} else {
				namespacePrefix = text.substring(0, i);
				localName = text.substring(i + 1);
			}
			if (onElement != null)
				onElement.fire(this);
			sp.unblock();
			return false;
		}
		if (c == '/') {
			state = State.START_ELEMENT_CLOSED;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
	}
	
	private boolean readStartElementClosed(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (c != '>') {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		isClosed = true;
		int i = text.indexOf(':');
		if (i < 0) {
			namespacePrefix = new UnprotectedStringBuffer();
			localName = text;
		} else {
			namespacePrefix = text.substring(0, i);
			localName = text.substring(i + 1);
		}
		ElementContext ctx = new ElementContext();
		ctx.text = text;
		ctx.namespacePrefix = namespacePrefix;
		ctx.localName = localName;
		ctx.namespaces = readNamespaces();
		context.addFirst(ctx);
		if (onElement != null)
			onElement.fire(this);
		sp.unblock();
		return false;
	}
	
	private boolean readStartElementSpace(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (isNameStartChar(c)) {
			Attribute a = new Attribute();
			a.text = new UnprotectedStringBuffer();
			a.text.append(c);
			attributes.add(a);
			state = State.ATTRIBUTE_NAME;
			return true;
		}
		if (Type.PROCESSING_INSTRUCTION.equals(type)) {
			if (c == '?') {
				state = State.PROCESSING_INSTRUCTION_CLOSED;
				return true;
			}
		} else {
			if (c == '>') {
				ElementContext ctx = new ElementContext();
				ctx.text = text;
				ctx.namespacePrefix = namespacePrefix;
				ctx.localName = localName;
				ctx.namespaces = readNamespaces();
				context.addFirst(ctx);
				if (onElement != null)
					onElement.fire(this);
				sp.unblock();
				return false;
			}
			if (c == '/') {
				state = State.START_ELEMENT_CLOSED;
				return true;
			}
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
	}
	
	private List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> readNamespaces() {
		List<Pair<UnprotectedStringBuffer, UnprotectedStringBuffer>> list = new LinkedList<>();
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext(); ) {
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
	
	private boolean readAttributeName(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		Attribute a = attributes.getLast();
		if (isNameChar(c)) {
			a.text.append(c);
			return true;
		}
		i = a.text.indexOf(':');
		if (i < 0) {
			a.namespacePrefix = new UnprotectedStringBuffer();
			a.localName = a.text;
		} else {
			a.namespacePrefix = a.text.substring(0, i);
			a.localName = a.text.substring(i + 1);
		}
		state = State.ATTRIBUTE_NAME_SPACE;
		return readAttributeNameSpace(c, sp);
	}
	
	private boolean readAttributeNameSpace(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (c == '=') {
			state = State.ATTRIBUTE_VALUE_SPACE;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
	}
	
	private boolean readAttributeValueSpace(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (c == '"') {
			state = State.ATTRIBUTE_VALUE;
			Attribute a = attributes.getLast();
			a.value = new UnprotectedStringBuffer();
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
	}
	
	private boolean readAttributeValue(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		Attribute a = attributes.getLast();
		if (c == '"') {
			resolveReferences(a.value);
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		if (c == '<') {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected character", Character.valueOf(c)),
				new LocalizableString("lc.xml.error", "in attribute value")));
			return false;
		}
		a.value.append(c);
		return true;
	}
	
	private boolean readProcessingInstruction(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (text.length() == 0) {
			if (!isNameStartChar(c)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
				return false;
			}
			text.append(c);
			return true;
		}
		if (isNameChar(c)) {
			text.append(c);
			return true;
		}
		state = State.START_ELEMENT_SPACE;
		return readStartElementSpace(i, sp);
	}
	
	private boolean readProcessingInstructionClosed(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		if (c != '>') {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf((char)c)));
			return false;
		}
		sp.unblock();
		return false;
	}
	
	private boolean readChars(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			text.replace("\r\n", "\n");
			resolveReferences(text);
			sp.unblock();
			return false;
		}
		char c = (char)i;
		if (c == '<') {
			cp.back(c);
			text.replace("\r\n", "\n");
			resolveReferences(text);
			sp.unblock();
			return false;
		}
		text.append(c);
		if (maxTextSize > 0 && text.length() >= maxTextSize) {
			text.replace("\r\n", "\n");
			resolveReferences(text);
			sp.unblock();
			return false;
		}
		return true;
	}
	
	private boolean readEndElementName(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		if (text.length() == 0) {
			if (!isNameStartChar(c)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
				return false;
			}
			text.append(c);
			statePos = 0;
			return true;
		}
		if (c == '>') {
			ElementContext ctx = context.getFirst();
			if (!ctx.text.equals(text)) {
				sp.error(new XMLException(cp.getPosition(),
					"Unexpected end element expected is", text.asString(), ctx.text.asString()));
				return false;
			}
			i = text.indexOf(':');
			if (i < 0) {
				namespacePrefix = new UnprotectedStringBuffer();
				localName = text;
			} else {
				namespacePrefix = text.substring(0, i);
				localName = text.substring(i + 1);
			}
			sp.unblock();
			return false;
		}
		if (isSpaceChar(c)) {
			statePos = 1;
			return true;
		}
		if (statePos != 0 || !isNameChar(c)) {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
			return false;
		}
		text.append(c);
		return true;
	}
	
	private static void resolveReferences(UnprotectedStringBuffer s) {
		int pos = 0;
		int i;
		while ((i = s.indexOf('&', pos)) != -1) {
			int j = s.indexOf(';', i + 1);
			if (j < 0) break;
			if (s.charAt(i + 1) == '#') {
				// character reference
				INumberEncoding n;
				if (s.charAt(i + 2) == 'x') {
					n = new HexadecimalNumber();
					i = i + 3;
				} else {
					n = new DecimalNumber();
					i = i + 2;
				}
				while (i < j)
					n.addChar(s.charAt(i++));
				char[] chars = Character.toChars((int)n.getNumber());
				s.replace(i, j, chars);
				pos = i + chars.length;
				continue;
			}
			UnprotectedStringBuffer name = s.substring(i + 1, j);
			char c;
			if (name.equals("amp"))
				c = '&';
			else if (name.equals("apos"))
				c = '\'';
			else if (name.equals("lt"))
				c = '<';
			else if (name.equals("gt"))
				c = '>';
			else if (name.equals("quot"))
				c = '"';
			else {
				pos = i + 1;
				continue;
			}
			s.replace(i, j, c);
			pos = i + 1;
		}
	}
	
	private boolean readInternalSubset(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in internal subset declaration")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (c == ']') {
			state = State.DOCTYPE_SPACE;
			return true;
		}
		if (c == '%') {
			state = State.DOCTYPE_INTERNAL_SUBSET_PEREFERENCE;
			statePos = 0;
			return true;
		}
		if (c != '<') {
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
			return false;
		}
		state = State.DOCTYPE_INTERNAL_SUBSET_TAG;
		statePos = 0;
		return true;
	}
	
	private boolean readInternalSubsetPEReference(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in internal subset declaration")));
			return false;
		}
		char c = (char)i;
		if (statePos == 0) {
			if (!isNameStartChar(c)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
				return false;
			}
			statePos = 1;
			return true;
		}
		if (statePos == 1) {
			if (isNameChar(c))
				return true;
			if (isSpaceChar(c)) {
				statePos = 2;
				return true;
			}
			if (c == ';') {
				state = State.DOCTYPE_INTERNAL_SUBSET;
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
			return false;
		}
		if (isSpaceChar(c))
			return true;
		if (c == ';') {
			state = State.DOCTYPE_INTERNAL_SUBSET;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
	}
	
	private boolean readInternalSubsetTag(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(),
					new LocalizableString("lc.xml.error", "Unexpected end"),
					new LocalizableString("lc.xml.error", "in internal subset declaration")));
				return false;
		}
		char c = (char)i;
		if (statePos == 0) {
			if (c == '!') {
				statePos = 1;
				return true;
			}
			if (c == '?') {
				statePos = 10;
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
			return false;
		}
		if (statePos == 1) {
			// exclamation
			if (c == '-') {
				statePos = 2;
				return true;
			}
			statePos = 10;
			return true;
		}
		if (statePos == 2) {
			// <!-
			if (c == '-')
				statePos = 3;
			else
				statePos = 10;
			return true;
		}
		if (statePos == 3) {
			// inside a comment
			if (c == '-')
				statePos = 4;
			return true;
		}
		if (statePos == 4) {
			// comment + -
			if (c == '-')
				statePos = 5;
			else
				statePos = 3;
			return true;
		}
		if (statePos == 5) {
			// comment + --
			if (c == '>') {
				state = State.DOCTYPE_INTERNAL_SUBSET;
				return true;
			}
			if (c != '-')
				statePos = 3;
			return true;
		}
		if (statePos == 10) {
			// element or entity declaration or processing instruction, outside string
			if (c == '>') {
				state = State.DOCTYPE_INTERNAL_SUBSET;
				return true;
			}
			if (c == '"')
				statePos = 11;
			return true;
		}
		if (statePos == 11) {
			// element or entity declaration or processing instruction, inside string
			if (c == '"')
				statePos = 10;
			return true;
		}
		sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
		return false;
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

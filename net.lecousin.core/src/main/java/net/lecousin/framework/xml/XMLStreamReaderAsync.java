package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;

/**
 * Read an XML in a similar way as {@link javax.xml.stream.XMLStreamReader}: read-only, forward, event based.
 * The method next() allows to move forward to the next event (such as start element, end element, comment, text...).
 * It uses {@link UnprotectedString} to avoid allocating many character arrays.
 * Charset is automatically detected by reading the beginning of the XML (either with auto-detection or with the specified encoding).
 */
public class XMLStreamReaderAsync extends XMLStreamEventsAsync {

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
	private IO.Readable.Buffered io;
	private CharacterProvider cp;
	
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
		if (Type.PROCESSING_INSTRUCTION.equals(event.type)) {
			if (event.text.length() == 3) {
				char c = event.text.charAt(0);
				if (c == 'x' || c == 'X') {
					c = event.text.charAt(1);
					if (c == 'm' || c == 'M') {
						c = event.text.charAt(2);
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
	
	@Override
	public SynchronizationPoint<Exception> next() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		reset();
		state = State.START;
		nextChar(sp);
		return sp;
	}
	
	/* Public utility methods */

	@Override
	public byte getPriority() {
		return io.getPriority();
	}
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public ISynchronizationPoint<Exception> startRootElement() {
		ISynchronizationPoint<Exception> start = start();
		if (start.isUnblocked()) {
			if (start.hasError()) return start;
			if (Type.START_ELEMENT.equals(event.type)) return start;
			return nextStartElement();
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		start.listenAsync(new Next(sp) {
			@Override
			protected void onNext() {
				if (Type.START_ELEMENT.equals(event.type)) sp.unblock();
				else nextStartElement().listenInline(sp);
			}
		}, sp);
		return sp;
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
				event.type = Type.TEXT;
				event.text = new UnprotectedStringBuffer();
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
			event.text = new UnprotectedStringBuffer();
			return true;
		}
		if (c == '?') {
			state = State.PROCESSING_INSTRUCTION;
			event.text = new UnprotectedStringBuffer();
			event.attributes = new LinkedList<>();
			event.type = Type.PROCESSING_INSTRUCTION;
			return true;
		}
		if (c == '/') {
			state = State.END_ELEMENT_NAME;
			event.type = Type.END_ELEMENT;
			event.text = new UnprotectedStringBuffer();
			return true;
		}
		if (isNameStartChar((char)c)) {
			state = State.START_ELEMENT_NAME;
			event.type = Type.START_ELEMENT;
			event.attributes = new LinkedList<>();
			event.text = new UnprotectedStringBuffer();
			event.text.append((char)c);
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
		if (event.text.length() == 0) {
			if (c == '-') {
				// can be a comment
				event.text.append((char)c);
				return true;
			}
			if (c == '[') {
				// can be CDATA
				event.text.append((char)c);
				return true;
			}
			if (c == 'D') {
				// can be DOCTYPE
				event.text.append((char)c);
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		char c1 = event.text.charAt(0);
		// comment
		if (c1 == '-') {
			if (c == '-') {
				// this is a comment
				state = State.COMMENT;
				event.type = Type.COMMENT;
				event.text = new UnprotectedStringBuffer();
				return true;
			}
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		// CDATA
		if (c1 == '[') {
			event.text.append((char)c);
			if (event.text.length() < 7) {
				if (event.text.isStartOf("[CDATA["))
					return true;
				sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
				return false;
			}
			if (!event.text.equals("[CDATA[")) {
				sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
				return false;
			}
			state = State.CDATA;
			event.type = Type.CDATA;
			event.text = new UnprotectedStringBuffer();
			return true;
		}
		// DOCTYPE
		if (event.text.length() < 7) {
			event.text.append((char)c);
			if (event.text.isStartOf("DOCTYPE"))
				return true;
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(cp.getPosition(), "Invalid XML"));
			return false;
		}
		state = State.DOCTYPE;
		event.type = Type.DOCTYPE;
		event.text = new UnprotectedStringBuffer();
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
		event.text.append((char)c);
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
			event.text.append((char)c);
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
		if (event.publicId.length() > 0) {
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
			event.publicId.append((char)c);
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
		if (event.system.length() > 0) {
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
		event.system.append((char)c);
		return true;
	}
	
	private boolean readComment(int c, SynchronizationPoint<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(cp.getPosition(),
				new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "inside comment")));
			return false;
		}
		if (maxTextSize > 0 && event.text.length() >= maxTextSize) {
			if (event.text.charAt(event.text.length() - 1) == '-') {
				if (event.text.charAt(event.text.length() - 2) == '-') {
					if (c == '>') {
						event.text.removeEndChars(2);
						sp.unblock();
						return false;
					}
					event.text.setCharAt(event.text.length() - 1, (char)c);
					return true;
				}
				if (c != '-') {
					event.text.setCharAt(event.text.length() - 1, (char)c);
					return true;
				}
				event.text.setCharAt(event.text.length() - 2, (char)c);
				return true;
			}
			event.text.setCharAt(event.text.length() - 1, (char)c);
			return true;
		}
		event.text.append((char)c);
		if (event.text.endsWith("-->")) {
			event.text.removeEndChars(3);
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
		if (maxCDataSize > 0 && event.text.length() >= maxCDataSize) {
			if (event.text.charAt(event.text.length() - 1) == ']') {
				if (event.text.charAt(event.text.length() - 2) == ']') {
					if (c == '>') {
						event.text.removeEndChars(2);
						sp.unblock();
						return false;
					}
					event.text.setCharAt(event.text.length() - 1, (char)c);
					return true;
				}
				if (c != ']') {
					event.text.setCharAt(event.text.length() - 1, (char)c);
					return true;
				}
				event.text.setCharAt(event.text.length() - 2, (char)c);
				return true;
			}
			event.text.setCharAt(event.text.length() - 1, (char)c);
			return true;
		}
		event.text.append((char)c);
		if (event.text.endsWith("]]>")) {
			event.text.removeEndChars(3);
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
			event.text.append(c);
			return true;
		}
		if (isSpaceChar(c)) {
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		if (c == '>') {
			onStartElement();
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
		event.isClosed = true;
		onStartElement();
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
			event.attributes.add(a);
			state = State.ATTRIBUTE_NAME;
			return true;
		}
		if (Type.PROCESSING_INSTRUCTION.equals(event.type)) {
			if (c == '?') {
				state = State.PROCESSING_INSTRUCTION_CLOSED;
				return true;
			}
		} else {
			if (c == '>') {
				onStartElement();
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
	
	private boolean readAttributeName(int i, SynchronizationPoint<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(cp.getPosition(), new LocalizableString("lc.xml.error", "Unexpected end"),
				new LocalizableString("lc.xml.error", "in XML document")));
			return false;
		}
		char c = (char)i;
		Attribute a = event.attributes.getLast();
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
			Attribute a = event.attributes.getLast();
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
		Attribute a = event.attributes.getLast();
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
		if (event.text.length() == 0) {
			if (!isNameStartChar(c)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
				return false;
			}
			event.text.append(c);
			return true;
		}
		if (isNameChar(c)) {
			event.text.append(c);
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
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
			sp.unblock();
			return false;
		}
		char c = (char)i;
		if (c == '<') {
			cp.back(c);
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
			sp.unblock();
			return false;
		}
		event.text.append(c);
		if (maxTextSize > 0 && event.text.length() >= maxTextSize) {
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
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
		if (event.text.length() == 0) {
			if (!isNameStartChar(c)) {
				sp.error(new XMLException(cp.getPosition(), "Unexpected character", Character.valueOf(c)));
				return false;
			}
			event.text.append(c);
			statePos = 0;
			return true;
		}
		if (c == '>') {
			ElementContext ctx = event.context.peekFirst();
			if (ctx == null) {
				sp.error(new XMLException(cp.getPosition(),
					"Unexpected end element", event.text.asString()));
				return false;
			}
			if (!ctx.text.equals(event.text)) {
				sp.error(new XMLException(cp.getPosition(),
					"Unexpected end element expected is", event.text.asString(), ctx.text.asString()));
				return false;
			}
			i = event.text.indexOf(':');
			if (i < 0) {
				event.namespacePrefix = new UnprotectedStringBuffer();
				event.localName = event.text;
			} else {
				event.namespacePrefix = event.text.substring(0, i);
				event.localName = event.text.substring(i + 1);
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
		event.text.append(c);
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
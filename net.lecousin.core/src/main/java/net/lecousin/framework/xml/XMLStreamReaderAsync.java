package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
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
public class XMLStreamReaderAsync extends XMLStreamEventsAsync {

	/** Initialize with the given IO.
	 * If it is not buffered, a {@link PreBufferedReadable} is created.
	 * If the charset is null, it will be automatically detected.
	 */
	public XMLStreamReaderAsync(IO.Readable io, Charset defaultEncoding, int charactersBuffersSize, int maxBuffers) {
		if (io instanceof IO.Readable.Buffered)
			this.io = (IO.Readable.Buffered)io;
		else
			this.io = new PreBufferedReadable(io, 1024, io.getPriority(), charactersBuffersSize,
				(byte)(io.getPriority() - 1), maxBuffers);
		this.defaultEncoding = defaultEncoding;
		this.charactersBuffersSize = charactersBuffersSize;
		this.maxBuffers = maxBuffers;
	}
	
	/** Constructor, without charset (automatic detection). */
	public XMLStreamReaderAsync(IO.Readable io, int charactersBuffersSize, int maxBuffers) {
		this(io, null, charactersBuffersSize, maxBuffers);
	}
	
	private Charset defaultEncoding;
	private int charactersBuffersSize;
	private int maxBuffers;
	private IO.Readable.Buffered io;
	private BufferedReadableCharacterStreamLocation stream;
	
	/* Public methods */

	@Override
	public IAsync<Exception> start() {
		Async<Exception> sp = new Async<>();
		io.canStartReading().thenStart(new Task.Cpu.FromRunnable(() -> {
			try {
				Starter start = new Starter(io, defaultEncoding, charactersBuffersSize, maxBuffers);
				stream = start.start();
				next().onDone(sp);
			} catch (Exception e) {
				sp.error(e);
			}
		}, "Start parsing XML", io.getPriority()), true);
		return sp;
	}
	
	@Override
	public Async<Exception> next() {
		Async<Exception> sp = new Async<>();
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
	
	public Pair<Integer, Integer> getPosition() {
		return new Pair<>(Integer.valueOf(stream.getLine()), Integer.valueOf(stream.getPositionInLine()));
	}
	
	/** Shortcut to move forward to the first START_ELEMENT event, skipping the header or comments. */
	public IAsync<Exception> startRootElement() {
		IAsync<Exception> start = start();
		if (start.isDone()) {
			if (start.hasError()) return start;
			if (Type.START_ELEMENT.equals(event.type)) return start;
			return nextStartElement();
		}
		Async<Exception> sp = new Async<>();
		start.thenStart(new ParsingTask(() -> {
			if (Type.START_ELEMENT.equals(event.type)) sp.unblock();
			else nextStartElement().onDone(sp);
		}), sp);
		return sp;
	}
	
	
	/* Private methods */
	
	private enum State {
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

	private void nextChar(Async<Exception> sp) {
		do {
			int c;
			try { c = stream.readAsync(); }
			catch (IOException e) {
				sp.error(e);
				return;
			}
			if (c == -2) {
				stream.canStartReading().thenStart(new ParsingTask(() -> nextChar(sp)), true);
				return;
			}
			boolean continu;
			switch (state) {
			case START:
				// start a new event
				if (c == -1) {
					// end of stream
					sp.error(new EOFException("End of file reached at line "
						+ stream.getLine() + ":" + stream.getPositionInLine()
						+ ", context = " + event.context));
					return;
				}
				if (c == '<') {
					// start a tag
					state = State.TAG;
					continu = true;
					break;
				}
				// start characters
				state = State.CHARS;
				event.type = Type.TEXT;
				event.text = new UnprotectedStringBuffer();
				continu = readChars(c, sp);
				break;
				
			case TAG:
				continu = readTag(c, sp);
				break;
			
			case TAG_EXCLAMATION:
				continu = readTagExclamation(c, sp);
				break;
				
			case PROCESSING_INSTRUCTION:
				continu = readProcessingInstruction(c, sp);
				break;
				
			case PROCESSING_INSTRUCTION_CLOSED:
				continu = readProcessingInstructionClosed(c, sp);
				break;
				
			case END_ELEMENT_NAME:
				continu = readEndElementName(c, sp);
				break;
				
			case START_ELEMENT_NAME:
				continu = readStartElementName(c, sp);
				break;
				
			case START_ELEMENT_SPACE:
				continu = readStartElementSpace(c, sp);
				break;
				
			case START_ELEMENT_CLOSED:
				continu = readStartElementClosed(c, sp);
				break;
				
			case ATTRIBUTE_NAME:
				continu = readAttributeName(c, sp);
				break;
				
			case ATTRIBUTE_NAME_SPACE:
				continu = readAttributeNameSpace(c, sp);
				break;
				
			case ATTRIBUTE_VALUE_SPACE:
				continu = readAttributeValueSpace(c, sp);
				break;
				
			case ATTRIBUTE_VALUE:
				continu = readAttributeValue(c, sp);
				break;
				
			case COMMENT:
				continu = readComment(c, sp);
				break;
				
			case CDATA:
				continu = readCData(c, sp);
				break;
				
			case DOCTYPE:
				continu = readDocType(c, sp);
				break;
				
			case DOCTYPE_NAME:
				continu = readDocTypeName(c, sp);
				break;

			case DOCTYPE_SPACE:
				continu = readDocTypeSpace(c, sp);
				break;

			case DOCTYPE_PUBLIC_NAME:
				continu = readDocTypePublicName(c, sp);
				break;

			case DOCTYPE_PUBLIC_SPACE:
				continu = readDocTypePublicSpace(c, sp);
				break;

			case DOCTYPE_PUBLIC_ID:
				continu = readDocTypePublicId(c, sp);
				break;

			case DOCTYPE_SYSTEM_NAME:
				continu = readDocTypeSystemName(c, sp);
				break;

			case DOCTYPE_SYSTEM_SPACE:
				continu = readDocTypeSystemSpace(c, sp);
				break;

			case DOCTYPE_SYSTEM_VALUE:
				continu = readDocTypeSystemValue(c, sp);
				break;

			case DOCTYPE_INTERNAL_SUBSET:
				continu = readInternalSubset(c, sp);
				break;

			case DOCTYPE_INTERNAL_SUBSET_PEREFERENCE:
				continu = readInternalSubsetPEReference(c, sp);
				break;

			case DOCTYPE_INTERNAL_SUBSET_TAG:
				continu = readInternalSubsetTag(c, sp);
				break;
				
			case CHARS:
				continu = readChars(c, sp);
				break;

			default:
				sp.error(new Exception("Unexpected state " + state));
				return;
			}
			if (!continu) return;
		} while (true);
	}
	
	private boolean readTag(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
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
		if (XMLStreamReader.isNameStartChar((char)c)) {
			state = State.START_ELEMENT_NAME;
			event.type = Type.START_ELEMENT;
			event.attributes = new LinkedList<>();
			event.text = new UnprotectedStringBuffer();
			event.text.append((char)c);
			return true;
		}
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readTagExclamation(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
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
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
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
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
			return false;
		}
		// CDATA
		if (c1 == '[') {
			event.text.append((char)c);
			if (event.text.length() < 7) {
				if (event.text.isStartOf("[CDATA["))
					return true;
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
				return false;
			}
			if (!event.text.equals("[CDATA[")) {
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
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
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
			return false;
		}
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_INVALID_XML));
			return false;
		}
		state = State.DOCTYPE;
		event.type = Type.DOCTYPE;
		event.text = new UnprotectedStringBuffer();
		return true;
	}
	
	private boolean readDocType(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c)) return true;
		if (!XMLStreamReader.isNameStartChar((char)c)) {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		event.text.append((char)c);
		state = State.DOCTYPE_NAME;
		return true;
	}
	
	private boolean readDocTypeName(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (XMLStreamReader.isNameChar((char)c)) {
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
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypeSpace(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
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
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypePublicName(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (statePos < 6) {
			if (c != "PUBLIC".charAt(statePos)) {
				sp.error(new XMLException(getPosition(),
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
				return false;
			}
			statePos++;
			return true;
		}
		if (event.publicId == null)
			event.publicId = new UnprotectedStringBuffer();
		if (event.publicId.length() > 0) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_INVALID_XML)));
			return false;
		}			
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_PUBLIC_SPACE;
		return true;
	}
	
	private boolean readDocTypePublicSpace(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c))
			return true;
		if (c != '"' && c != '\'') {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_PUBLIC_ID;
		statePos = c;
		return true;
	}
	
	private boolean readDocTypePublicId(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
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
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
		return false;
	}
	
	private boolean readDocTypeSystemName(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (statePos < 6) {
			if (c != "SYSTEM".charAt(statePos)) {
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER,
					Character.valueOf((char)c)));
				return false;
			}
			statePos++;
			return true;
		}
		if (event.system == null)
			event.system = new UnprotectedStringBuffer();
		if (event.system.length() > 0) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_INVALID_XML)));
			return false;
		}			
		if (!isSpaceChar((char)c)) {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_SYSTEM_SPACE;
		return true;
	}

	private boolean readDocTypeSystemSpace(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (isSpaceChar((char)c))
			return true;
		if (c != '"' && c != '\'') {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		state = State.DOCTYPE_SYSTEM_VALUE;
		statePos = c;
		return true;
	}
	
	private boolean readDocTypeSystemValue(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (c == statePos) {
			state = State.DOCTYPE_SPACE;
			return true;
		}
		if (event.system == null)
			event.system = new UnprotectedStringBuffer();
		event.system.append((char)c);
		return true;
	}
	
	private boolean readComment(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside comment")));
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
	
	private boolean readCData(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "inside CDATA")));
			return false;
		}
		if (maxCDataSize > 0 && event.text.length() >= maxCDataSize + 2) {
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
	
	private boolean readStartElementName(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (XMLStreamReader.isNameChar(c)) {
			if (maxTextSize <= 0 || event.text.length() < maxTextSize)
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
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
		return false;
	}
	
	@SuppressWarnings("squid:S3516") // always return false
	private boolean readStartElementClosed(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (c != '>') {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf((char)c)));
			return false;
		}
		event.isClosed = true;
		onStartElement();
		sp.unblock();
		return false;
	}
	
	private boolean readStartElementSpace(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (XMLStreamReader.isNameStartChar(c)) {
			Attribute a = new Attribute();
			a.text = new UnprotectedStringBuffer();
			a.text.append(c);
			event.attributes.add(a);
			state = State.ATTRIBUTE_NAME;
			return true;
		}
		if (!Type.PROCESSING_INSTRUCTION.equals(event.type)) {
			if (c == '>') {
				onStartElement();
				sp.unblock();
				return false;
			}
			if (c == '/') {
				state = State.START_ELEMENT_CLOSED;
				return true;
			}
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
			return false;
		}
		if (c == '?') {
			state = State.PROCESSING_INSTRUCTION_CLOSED;
			return true;
		}
		// skip any other character
		return true;
	}
	
	private boolean readAttributeName(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		Attribute a = event.attributes.getLast();
		if (XMLStreamReader.isNameChar(c)) {
			if (maxTextSize <= 0 || a.text.length() < maxTextSize)
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
	
	private boolean readAttributeNameSpace(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (c == '=') {
			state = State.ATTRIBUTE_VALUE_SPACE;
			return true;
		}
		// no equals, we accept the attribute without value
		state = State.START_ELEMENT_SPACE;
		stream.back(c);
		return true;
	}
	
	private boolean readAttributeValueSpace(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (isSpaceChar(c))
			return true;
		if (c == '"') {
			state = State.ATTRIBUTE_VALUE;
			statePos = 1;
			Attribute a = event.attributes.getLast();
			a.value = new UnprotectedStringBuffer();
			return true;
		}
		if (c == '\'') {
			state = State.ATTRIBUTE_VALUE;
			statePos = 2;
			Attribute a = event.attributes.getLast();
			a.value = new UnprotectedStringBuffer();
			return true;
		}
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
		return false;
	}
	
	private boolean readAttributeValue(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		Attribute a = event.attributes.getLast();
		if (c == '"' && statePos == 1) {
			resolveReferences(a.value);
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		if (c == '\'' && statePos == 2) {
			resolveReferences(a.value);
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		if (c == '<') {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
					XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in attribute value")));
			return false;
		}
		if (c == '\n') {
			if (maxTextSize <= 0 || a.value.length() < maxTextSize)
				a.value.append(' ');
		} else if (c == '\r') return true;
		else {
			if (maxTextSize <= 0 || a.value.length() < maxTextSize)
				a.value.append(c);
		}
		return true;
	}
	
	private boolean readProcessingInstruction(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (event.text.length() == 0) {
			if (!XMLStreamReader.isNameStartChar(c)) {
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
				return false;
			}
			event.text.append(c);
			return true;
		}
		if (XMLStreamReader.isNameChar(c)) {
			if (maxTextSize <= 0 || event.text.length() < maxTextSize)
				event.text.append(c);
			return true;
		}
		state = State.START_ELEMENT_SPACE;
		return readStartElementSpace(i, sp);
	}
	
	private boolean readProcessingInstructionClosed(int c, Async<Exception> sp) {
		if (c == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		if (c != '>') {
			// accept a unuseful ? alone
			stream.back((char)c);
			state = State.START_ELEMENT_SPACE;
			return true;
		}
		sp.unblock();
		return false;
	}
	
	private boolean readChars(int i, Async<Exception> sp) {
		if (i == -1) {
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
			sp.unblock();
			return false;
		}
		char c = (char)i;
		if (c == '<') {
			stream.back(c);
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
			sp.unblock();
			return false;
		}
		event.text.append(c);
		if (maxTextSize > 0 && event.text.length() >= maxTextSize) {
			event.text.replace("\r\n", "\n");
			resolveReferences(event.text);
			if (event.text.length() < maxTextSize) return true;
			sp.unblock();
			return false;
		}
		return true;
	}
	
	private boolean readEndElementName(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(), new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
				XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in XML document")));
			return false;
		}
		char c = (char)i;
		if (event.text.length() == 0) {
			if (!XMLStreamReader.isNameStartChar(c)) {
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
				return false;
			}
			event.text.append(c);
			statePos = 0;
			return true;
		}
		if (c == '>') {
			ElementContext ctx = event.context.peekFirst();
			if (ctx == null) {
				sp.error(new XMLException(getPosition(),
					"Unexpected end element", event.text.asString()));
				return false;
			}
			if (!ctx.text.equals(event.text)) {
				sp.error(new XMLException(getPosition(),
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
			event.namespaceURI = getNamespaceURI(event.namespacePrefix);
			sp.unblock();
			return false;
		}
		if (isSpaceChar(c)) {
			statePos = 1;
			return true;
		}
		if (statePos != 0 || !XMLStreamReader.isNameChar(c)) {
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
			return false;
		}
		if (maxTextSize <= 0 || event.text.length() < maxTextSize)
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
				pos = i;
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
				s.replace(pos, j, chars);
				pos = pos + chars.length;
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
	
	private boolean readInternalSubset(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in internal subset declaration")));
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
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
			return false;
		}
		state = State.DOCTYPE_INTERNAL_SUBSET_TAG;
		statePos = 0;
		return true;
	}
	
	private boolean readInternalSubsetPEReference(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
				new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in internal subset declaration")));
			return false;
		}
		char c = (char)i;
		if (statePos == 0) {
			if (!XMLStreamReader.isNameStartChar(c)) {
				sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
				return false;
			}
			statePos = 1;
			return true;
		}
		if (statePos == 1) {
			if (XMLStreamReader.isNameChar(c))
				return true;
			if (isSpaceChar(c)) {
				statePos = 2;
				return true;
			}
			if (c == ';') {
				state = State.DOCTYPE_INTERNAL_SUBSET;
				return true;
			}
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
			return false;
		}
		if (isSpaceChar(c))
			return true;
		if (c == ';') {
			state = State.DOCTYPE_INTERNAL_SUBSET;
			return true;
		}
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
		return false;
	}
	
	private boolean readInternalSubsetTag(int i, Async<Exception> sp) {
		if (i == -1) {
			sp.error(new XMLException(getPosition(),
					new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR,
						XMLException.LOCALIZED_MESSAGE_UNEXPECTED_END),
					new LocalizableString(XMLException.LOCALIZED_NAMESPACE_XML_ERROR, "in internal subset declaration")));
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
			sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
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
		sp.error(new XMLException(getPosition(), XMLException.LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER, Character.valueOf(c)));
		return false;
	}

}

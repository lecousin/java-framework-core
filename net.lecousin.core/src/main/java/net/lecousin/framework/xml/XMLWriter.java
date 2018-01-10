package net.lecousin.framework.xml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.CharacterStreamWritePool;
import net.lecousin.framework.io.text.ICharacterStream;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/** Utility class to generate XML. */
public class XMLWriter {

	/** Constructor. */
	@SuppressWarnings("resource")
	public XMLWriter(IO.Writable.Buffered output, Charset encoding, boolean includeXMLDeclaration) {
		this(new BufferedWritableCharacterStream(output, encoding != null ? encoding : StandardCharsets.UTF_8, 4096), includeXMLDeclaration);
	}
	
	/** Constructor. */
	public XMLWriter(ICharacterStream.Writable.Buffered output, boolean includeXMLDeclaration) {
		this.output = output;
		writer = new CharacterStreamWritePool(output);
		this.includeXMLDeclaration = includeXMLDeclaration;
	}
	
	private ICharacterStream.Writable.Buffered output;
	private CharacterStreamWritePool writer;
	private boolean includeXMLDeclaration;
	private LinkedList<Context> context = new LinkedList<>();
	
	private static final class Context {
		private String namespaceURI = null;
		private String localName;
		private Map<String, String> namespaces = null;
		private boolean open = true;
	}
	
	private String getNamespace(String uri) {
		for (Context ctx : context) {
			if (ctx.namespaces == null) continue;
			String ns = ctx.namespaces.get(uri);
			if (ns != null)
				return ns;
		}
		return null;
	}
	
	/** Escape a string to include it in XML (this is automatically called by other methods of this class when needed). */
	public static String escape(CharSequence s) {
		StringBuilder str = new StringBuilder();
		int len = s.length();
		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);
			if (c == '&') str.append("&amp;");
			else if (c == '"') str.append("&quot;");
			else if (c == '\'') str.append("&apos;");
			else if (c == '>') str.append("&gt;");
			else if (c == '<') str.append("&lt;");
			else if (c < 32) str.append("&#").append((int)c).append(';');
			else str.append(c);
		}
		return str.toString();
	}

	private static final char[] XML_DECLARATION_START = new char[] {
		'<', '?', 'x', 'm', 'l', ' ', 'v', 'e', 'r', 's', 'i', 'o', 'n', '=', '"', '1', '.', '1', '"',
		' ', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', '=', '"'
	};
	private static final char[] XML_DECLARATION_END = new char[] { '"', '?', '>', '\n' };
	private static final char[] XMLNS = new char[] { ' ', 'x', 'm', 'l', 'n', 's' };
	private static final char[] ATTRIBUTE_EQUALS = new char[] { '=', '"' };
	private static final char[] CLOSE_EMPTY_TAG = new char[] { '/', '>' };
	private static final char[] START_CLOSE = new char[] { '<', '/' };
	private static final char[] START_CDATA = new char[] { '[', 'C', 'D', 'A', 'T', 'A', '[' };
	private static final char[] END_CDATA = new char[] { ']', ']' };
	private static final char[] START_COMMENT = new char[] { '<', '!', '-', '-', ' ' };
	private static final char[] END_COMMENT = new char[] { ' ', '-', '-', '>' };
	
	/**
	 * Start the document with the XML processing instruction if needed, and opening the root element.
	 * @param rootNamespaceURI namespace of the root element
	 * @param rootLocalName name of the root element
	 * @param namespaces mapping from namespace URI and prefix, prefix may be empty for default namespace
	 */
	public ISynchronizationPoint<IOException> start(String rootNamespaceURI, String rootLocalName, Map<String, String> namespaces) {
		if (includeXMLDeclaration) {
			writer.write(XML_DECLARATION_START);
			writer.write(output.getEncoding().name());
			writer.write(XML_DECLARATION_END);
		}
		writer.write('<');
		if (rootNamespaceURI != null) {
			String ns = namespaces.get(rootNamespaceURI);
			if (ns != null && ns.length() > 0) {
				writer.write(ns);
				writer.write(':');
			}
		}
		ISynchronizationPoint<IOException> result = writer.write(rootLocalName);
		if (namespaces != null && !namespaces.isEmpty()) {
			for (Map.Entry<String, String> ns : namespaces.entrySet()) {
				writer.write(XMLNS);
				if (ns.getValue().length() > 0) {
					writer.write(':');
					writer.write(ns.getValue());
				}
				writer.write(ATTRIBUTE_EQUALS);
				writer.write(escape(ns.getKey()));
				result = writer.write('"');
			}
		}
		Context ctx = new Context();
		ctx.namespaces = new HashMap<>();
		if (namespaces != null) ctx.namespaces.putAll(namespaces);
		ctx.namespaceURI = rootNamespaceURI;
		ctx.localName = rootLocalName;
		ctx.open = true;
		context.addFirst(ctx);
		return result;
	}
	
	/** End the document, close any open element, and flush the output stream. */
	public ISynchronizationPoint<IOException> end() {
		while (!context.isEmpty())
			closeElement();
		ISynchronizationPoint<IOException> write = writer.flush();
		if (!write.isUnblocked()) {
			SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
			write.listenInline(() -> { output.flush().listenInline(sp); }, sp);
			return sp;
		}
		if (write.hasError())
			return write;
		return output.flush();
	}
	
	/** Add an attribute to the current element. */
	public ISynchronizationPoint<IOException> addAttribute(CharSequence name, CharSequence value) {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (!ctx.open)
			return new SynchronizationPoint<>(new IOException("Cannot add attribute to XML element when the opening tag is closed"));
		writer.write(' ');
		writer.write(name);
		writer.write(ATTRIBUTE_EQUALS);
		writer.write(escape(value));
		return writer.write('"');
	}
	
	/** Signal the end of attributes, so the opening tag can be closed and the content can start.
	 * This is optional to call this method, as any other method writing something inside the current element
	 * will first check the opening tag is already closed or not.
	 */
	public ISynchronizationPoint<IOException> endOfAttributes() {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (!ctx.open)
			return new SynchronizationPoint<>(new IOException("Opening tag already closed"));
		ctx.open = false;
		return writer.write('>');
	}
	
	/** Open a new element. */
	public ISynchronizationPoint<IOException> openElement(String namespaceURI, String localName, Map<String, String> namespaces) {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		ctx = new Context();
		ctx.namespaces = namespaces != null && !namespaces.isEmpty() ? new HashMap<>(namespaces) : null;
		ctx.namespaceURI = namespaceURI;
		ctx.localName = localName;
		ctx.open = true;
		context.addFirst(ctx);
		String ns = getNamespace(namespaceURI);
		writer.write('<');
		if (ns != null && ns.length() > 0) {
			writer.write(ns);
			writer.write(':');
		}
		if (namespaces == null || namespaces.isEmpty())
			return writer.write(localName);
		writer.write(localName);
		Iterator<Map.Entry<String,String>> it = namespaces.entrySet().iterator();
		do {
			Map.Entry<String,String> e = it.next();
			String name = "xmlns";
			if (!e.getValue().isEmpty())
				name = name + ':' + e.getValue();
			if (!it.hasNext())
				return addAttribute(name, e.getKey());
			addAttribute(name, e.getKey());
		} while (true);
	}
	
	/** Close the current element. */
	public ISynchronizationPoint<IOException> closeElement() {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (ctx.open) {
			context.removeFirst();
			return writer.write(CLOSE_EMPTY_TAG);
		}
		String ns = getNamespace(ctx.namespaceURI);
		context.removeFirst();
		writer.write(START_CLOSE);
		if (ns != null && ns.length() > 0) {
			writer.write(ns);
			writer.write(':');
		}
		writer.write(ctx.localName);
		return writer.write('>');
	}
	
	/** Add text inside the current element. */
	public ISynchronizationPoint<IOException> addText(CharSequence text) {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		return writer.write(escape(text));
	}
	
	/** Add a CDATA section inside the current element. */
	public ISynchronizationPoint<IOException> addCData(CharSequence data) {
		Context ctx = context.peekFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		writer.write(START_CDATA);
		writer.write(data);
		return writer.write(END_CDATA);
	}
	
	/** Add a comment inside the current element. */
	public ISynchronizationPoint<IOException> addComment(CharSequence comment) {
		Context ctx = context.peekFirst();
		if (ctx != null && ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		writer.write(START_COMMENT);
		writer.write(comment);
		return writer.write(END_COMMENT);
	}
	
	/** Write the given DOM element. */
	public ISynchronizationPoint<IOException> write(Element element) {
		String name = element.getLocalName();
		if (name == null) name = element.getNodeName();
		String uri = element.getNamespaceURI();
		String prefix = element.getPrefix();
		Map<String, String> namespaces = null;
		if (uri != null) {
			namespaces = new HashMap<>(5);
			namespaces.put(uri, prefix);
		}
		openElement(uri, name, namespaces);
		NamedNodeMap attrs = element.getAttributes();
		if (attrs != null)
			for (int i = 0; i < attrs.getLength(); ++i) {
				Node a = attrs.item(i);
				addAttribute(a.getNodeName(), a.getNodeValue());
			}
		NodeList children = element.getChildNodes();
		if (children.getLength() == 0)
			return closeElement();
		ISynchronizationPoint<IOException> open = endOfAttributes();
		if (open.isUnblocked()) {
			if (open.hasError()) return open;
			return writeChild(children, 0);
		}
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		open.listenAsync(new Task.Cpu.FromRunnable("Write DOM", output.getPriority(), () -> {
			writeChild(children, 0).listenInline(sp);
		}), sp);
		return sp;
	}
	
	private ISynchronizationPoint<IOException> writeChild(NodeList children, int childIndex) {
		do {
			Node child = children.item(childIndex);
			ISynchronizationPoint<IOException> sp;
			if (child instanceof Element)
				sp = write((Element)child);
			else if (child instanceof Comment)
				sp = addComment(((Comment)child).getData());
			else if (child instanceof CDATASection)
				sp = addCData(((CDATASection)child).getData());
			else if (child instanceof Text)
				sp = addText(((Text)child).getData());
			else
				sp = new SynchronizationPoint<>(true);
			if (sp.isUnblocked()) {
				if (sp.hasError()) return sp;
				childIndex++;
				if (childIndex == children.getLength()) return sp;
				continue;
			}
			SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
			int nextIndex = childIndex + 1;
			sp.listenAsync(new Task.Cpu.FromRunnable("Write DOM", output.getPriority(), () -> {
				if (nextIndex == children.getLength()) {
					result.unblock();
					return;
				}
				writeChild(children, nextIndex).listenInline(result);
			}), result);
			return result;
		} while (true);
	}
	
}

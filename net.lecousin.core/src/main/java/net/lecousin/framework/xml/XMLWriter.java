package net.lecousin.framework.xml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.CharacterStreamWritePool;
import net.lecousin.framework.io.text.ICharacterStream;

public class XMLWriter {

	@SuppressWarnings("resource")
	public XMLWriter(IO.Writable.Buffered output, Charset encoding, boolean includeXMLDeclaration) {
		this(new BufferedWritableCharacterStream(output, encoding != null ? encoding : StandardCharsets.UTF_8, 4096), includeXMLDeclaration);
	}
	
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
	
	public static String toAttribute(CharSequence s) {
		return s.toString()
			.replace("&", "&amp;")
			.replace("\"", "&quot;")
			.replace("'", "&apos;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
	
	public static final char[] XML_DECLARATION_START = new char[] {
		'<', '?', 'x', 'm', 'l', ' ', 'v', 'e', 'r', 's', 'i', 'o', 'n', '=', '"', '1', '.', '0', '"',
		' ', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', '=', '"'
	};
	public static final char[] XML_DECLARATION_END = new char[] { '"', '?', '>', '\n' };
	public static final char[] XMLNS = new char[] { ' ', 'x', 'm', 'l', 'n', 's' };
	public static final char[] ATTRIBUTE_EQUALS = new char[] { '=', '"' };
	public static final char[] CLOSE_EMPTY_TAG = new char[] { '/', '>' };
	public static final char[] START_CLOSE = new char[] { '<', '/' };
	public static final char[] START_CDATA = new char[] { '[', 'C', 'D', 'A', 'T', 'A', '[' };
	public static final char[] END_CDATA = new char[] { ']', ']' };
	public static final char[] START_COMMENT = new char[] { '<', '!', '-', '-', ' ' };
	public static final char[] END_COMMENT = new char[] { ' ', '-', '-', '>' };
	
	/**
	 * 
	 * @param rootNamespaceURI
	 * @param rootLocalName
	 * @param namespaces mapping from namespace URI and prefix, prefix may be empty for default namespace
	 * @return
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
				writer.write(toAttribute(ns.getKey()));
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
	
	public ISynchronizationPoint<IOException> end() {
		ISynchronizationPoint<IOException> write = null;
		while (!context.isEmpty())
			write = closeElement();
		if (write != null) {
			SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
			write.listenInline(() -> { output.flush().listenInline(sp); }, sp);
			return sp;
		}
		return output.flush();
	}
	
	public ISynchronizationPoint<IOException> addAttribute(CharSequence name, CharSequence value) {
		Context ctx = context.getFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (!ctx.open)
			return new SynchronizationPoint<>(new IOException("Cannot add attribute to XML element when the opening tag is closed"));
		writer.write(' ');
		writer.write(name);
		writer.write(ATTRIBUTE_EQUALS);
		writer.write(toAttribute(value));
		return writer.write('"');
	}
	
	public ISynchronizationPoint<IOException> endOfAttributes() {
		Context ctx = context.getFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (!ctx.open)
			return new SynchronizationPoint<>(new IOException("Opening tag already closed"));
		ctx.open = false;
		return writer.write('>');
	}
	
	public ISynchronizationPoint<IOException> openElement(String namespaceURI, String localName, Map<String, String> namespaces) {
		Context ctx = context.getFirst();
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
		return writer.write(localName);
	}
	
	public ISynchronizationPoint<IOException> closeElement() {
		Context ctx = context.getFirst();
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
	
	public ISynchronizationPoint<IOException> addText(CharSequence text) {
		Context ctx = context.getFirst();
		if (ctx == null)
			return new SynchronizationPoint<>(new IOException("XML document closed"));
		if (ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		return writer.write(text);
	}
	
	public ISynchronizationPoint<IOException> addCData(CharSequence data) {
		Context ctx = context.getFirst();
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
	
	public ISynchronizationPoint<IOException> addComment(CharSequence comment) {
		Context ctx = context.getFirst();
		if (ctx != null && ctx.open) {
			ctx.open = false;
			writer.write('>');
		}
		writer.write(START_COMMENT);
		writer.write(comment);
		return writer.write(END_COMMENT);
	}
	
}

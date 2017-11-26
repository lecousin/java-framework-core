package net.lecousin.framework.xml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.CharacterStreamWritePool;
import net.lecousin.framework.io.text.ICharacterStream;

public class XMLWriter {

	@SuppressWarnings("resource")
	public XMLWriter(IO.Writable.Buffered output, Charset encoding, boolean includeXMLDeclaration) {
		this(new BufferedWritableCharacterStream(output, encoding, 4096), includeXMLDeclaration);
	}
	
	public XMLWriter(ICharacterStream.Writable.Buffered output, boolean includeXMLDeclaration) {
		this.output = output;
		writer = new CharacterStreamWritePool(output);
		this.includeXMLDeclaration = includeXMLDeclaration;
	}
	
	private ICharacterStream.Writable.Buffered output;
	private CharacterStreamWritePool writer;
	private boolean includeXMLDeclaration;
	
	public static final char[] XML_DECLARATION_START = new char[] {
		'<', '?', 'x', 'm', 'l', ' ', 'v', 'e', 'r', 's', 'i', 'o', 'n', '=', '"', '1', '.', '0', '"',
		' ', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', '=', '"'
	};
	public static final char[] XML_DECLARATION_END = new char[] { '"', '?', '>', '\n' };
	public static final char[] XMLNS = new char[] { ' ', 'x', 'm', 'l', 'n', 's' };
	public static final char[] ATTRIBUTE_EQUALS = new char[] { '=', '"' };
	
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
		return result;
	}
	
	public ISynchronizationPoint<IOException> end() {
		endRoot;
		output.flush();
	}
	
}

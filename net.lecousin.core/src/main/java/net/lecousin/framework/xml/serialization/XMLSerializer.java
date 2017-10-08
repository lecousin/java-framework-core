package net.lecousin.framework.xml.serialization;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.serialization.AbstractSerializer;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.io.text.ICharacterStream.Writable.Buffered;
import net.lecousin.framework.util.Triple;

/** Serialize objects into XML. */
public class XMLSerializer extends AbstractSerializer<ICharacterStream.Writable.Buffered> {

	/** Constructor.
	 * @param encoding charset to use.
	 * @param pretty add indentation
	 * @param bufferSize for output bufferization
	 */
	public XMLSerializer(Charset encoding, boolean pretty, int bufferSize) {
		this.encoding = encoding;
		this.pretty = pretty;
		this.bufferSize = bufferSize;
	}
	
	private Charset encoding;
	private boolean pretty;
	private int bufferSize;
	
	@Override
	protected ICharacterStream.Writable.Buffered adaptOutput(Writable output) {
		return new BufferedWritableCharacterStream(output, encoding, bufferSize);
	}
	
	private static char[] xmlInstructionStart = new char[]
		{ '<','?','x','m','l',' ','v','e','r','s','i','o','n','=','"','1','.','0','"',' ','e','n','c','o','d','i','n','g','=','"' };
	private static char[] xmlInstructionEnd = new char[] { '"','?','>','\r','\n' };
	//private static char[] xmlnsStart = new char[] { ' ','x','m','l','n', 's', '=', '"' };
	private static char[] sepAttrValue = new char[] { '=', '"' };
	private static char[] _true = new char[] { 't','r','u','e' };
	private static char[] _false = new char[] { 'f','a','l','s','e' };
	private static char[] _lt = new char[] { '&','l','t',';' };
	private static char[] _gt = new char[] { '&','g','t',';' };
	private static char[] _amp = new char[] { '&','a','m','p',';' };
	private static char[] _quote = new char[] { '&','q','u','o','t',';' };
	private static char[] _apos = new char[] { '&','a','p','o','s',';' };
	private static char[] CRLF = new char[] { '\r','\n' };
	private static char[] endEmptyTag = new char[] { '/','>' };
	private static char[] endTag = new char[] { '<','/' };
	
	private static class Element {
		private Element(String name) {
			this.name = name;
		}
		
		private String name;
		private ArrayList<Triple<Attribute,Collection<?>,String>> collections = new ArrayList<>();
		private ArrayList<Triple<Attribute,Object,String>> objects = new ArrayList<>();
		private boolean contentStarted = false;
	}
	
	private Element element;
	private LinkedList<Element> elements = new LinkedList<>();
	
	private int indent = 0;
	
	private void indent(Buffered output) throws Exception {
		for (int i = 0; i < indent; ++i)
			output.write('\t');
	}
	
	@Override
	protected void startSerialization(
		Object rootObject, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		output.write(xmlInstructionStart);
		output.write(encoding.name());
		output.write(xmlInstructionEnd);
		// start root element
		output.write('<');
		output.write(rootObject.getClass().getSimpleName());
		/* TODO
		if (namespaceURI != null) {
			output.write(xmlnsStart);
			output.write(namespaceURI);
			output.write('"');
		}*/
		element = new Element(rootObject.getClass().getSimpleName());
	}
	
	@Override
	protected ISynchronizationPoint<? extends Exception> endSerialization(
		Object rootObject, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		endElement(output, rules, alreadySerialized);
		return output.flush();
	}
	
	@Override
	protected void startObject(Object obj, Class<?> cl, Buffered output) {
	}
	
	@Override
	protected void endObject(Object obj, Class<?> cl, Buffered output) {
	}

	private static void startAttribute(String name, Buffered output) throws Exception {
		output.write(' ');
		output.write(name);
		output.write(sepAttrValue);
	}
	
	private void endElement(Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized) throws Exception {
		if (element.collections.isEmpty() && element.objects.isEmpty() && !element.contentStarted) {
			output.write(endEmptyTag);
			if (pretty) {
				output.write(CRLF);
			}
		} else {
			// skip checkstyle: VariableDeclarationUsageDistance
			boolean sameLine = element.contentStarted;
			if (!element.contentStarted) {
				startElementContent(output);
				if (pretty) {
					output.write(CRLF);
					indent++;
				}
			}
			for (Triple<Attribute,Object,String> o : element.objects) {
				Attribute a = o.getValue1();
				if (a != null) {
					if (pretty)
						indent(output);
					output.write('<');
					output.write(a.getName());
					elements.add(element);
					element = new Element(a.getName());
					super.serializeObject(o.getValue2(), o.getValue3(), output, rules, alreadySerialized);
					endElement(output, rules, alreadySerialized);
				}
			}
			for (Triple<Attribute,Collection<?>,String> collection : element.collections) {
				Attribute a = collection.getValue1();
				if (a != null)
					super.serializeCollection(collection.getValue1(), collection.getValue2(), collection.getValue3(),
						output, rules, alreadySerialized);
				else
					super.serializeCollectionValue(collection.getValue2(), collection.getValue3(),
						output, rules, alreadySerialized);
			}
			if (pretty && !sameLine) {
				indent--;
				indent(output);
			}
			output.write(endTag);
			output.write(element.name);
			output.write('>');
			if (pretty) {
				output.write(CRLF);
			}
		}
		if (elements.isEmpty())
			element = null;
		else
			element = elements.removeLast();
	}
	
	private void startElementContent(Buffered output) throws Exception {
		output.write('>');
		element.contentStarted = true;
	}
	
	
	@Override
	protected void serializeNullValue(Buffered output) {
	}
	
	@Override
	protected void serializeBooleanAttribute(Attribute a, Boolean value, Buffered output) throws Exception {
		if (value == null)
			return;
		startAttribute(a.getName(), output);
		writeBoolean(value.booleanValue(), output);
		output.write('"');
	}
	
	@Override
	protected void serializeBooleanValue(Boolean value, Buffered output) throws Exception {
		if (value == null)
			return;
		if (!element.contentStarted)
			startElementContent(output);
		else
			throw new IllegalStateException("Cannot write value inside element " + element.name + ": element content already written");
		writeBoolean(value.booleanValue(), output);
	}
	
	protected void writeBoolean(boolean value, Buffered output) throws Exception {
		output.write(value ? _true : _false);
	}
	
	@Override
	protected void serializeNumericAttribute(Attribute a, Number value, Buffered output) throws Exception {
		if (value == null)
			return;
		startAttribute(a.getName(), output);
		output.write(value.toString());
		output.write('"');
	}
	
	@Override
	protected void serializeNumericValue(Number value, Buffered output) throws Exception {
		if (value == null)
			return;
		if (!element.contentStarted)
			startElementContent(output);
		else
			throw new IllegalStateException("Cannot write value inside element " + element.name + ": element content already written");
		output.write(value.toString());
	}
	
	@Override
	protected void serializeCharacterAttribute(Attribute a, Character value, Buffered output) throws Exception {
		if (value == null)
			return;
		startAttribute(a.getName(), output);
		char c = value.charValue();
		if (c == '<') output.write(_lt);
		else if (c == '&') output.write(_amp);
		else if (c == '"') output.write(_quote);
		else output.write(c);
		output.write('"');
	}
	
	@Override
	protected void serializeCharacterValue(Character value, Buffered output) throws Exception {
		if (value == null)
			return;
		if (!element.contentStarted)
			startElementContent(output);
		else
			throw new IllegalStateException("Cannot write value inside element " + element.name + ": element content already written");
		char c = value.charValue();
		if (c == '<') output.write(_lt);
		else if (c == '&') output.write(_amp);
		else if (c == '"') output.write(_quote);
		else if (c == '>') output.write(_gt);
		else if (c == '\'') output.write(_apos);
		else output.write(c);
	}
	
	@Override
	protected void serializeStringAttribute(Attribute a, CharSequence value, Buffered output) throws Exception {
		if (value == null)
			return;
		startAttribute(a.getName(), output);
		
		char[] chars = value.toString().toCharArray();
		int start = 0;
		for (int pos = 0; pos < chars.length; ++pos) {
			if (chars[pos] == '<') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_lt);
				start = pos + 1;
			} else if (chars[pos] == '&') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_amp);
				start = pos + 1;
			} else if (chars[pos] == '"') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_quote);
				start = pos + 1;
			}
		}
		if (start < chars.length)
			output.write(chars, start, chars.length - start);

		output.write('"');
	}
	
	@Override
	protected void serializeStringValue(CharSequence value, Buffered output) throws Exception {
		if (value == null)
			return;
		if (!element.contentStarted)
			startElementContent(output);
		else
			throw new IllegalStateException("Cannot write value inside element " + element.name + ": element content already written");
		
		char[] chars = value.toString().toCharArray();
		int start = 0;
		for (int pos = 0; pos < chars.length; ++pos) {
			if (chars[pos] == '<') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_lt);
				start = pos + 1;
			} else if (chars[pos] == '&') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_amp);
				start = pos + 1;
			} else if (chars[pos] == '"') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_quote);
				start = pos + 1;
			} else if (chars[pos] == '\'') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_apos);
				start = pos + 1;
			} else if (chars[pos] == '>') {
				if (start < pos)
					output.write(chars, start, pos - start);
				output.write(_gt);
				start = pos + 1;
			}
		}
		if (start < chars.length)
			output.write(chars, start, chars.length - start);
	}
	
	@Override
	protected void serializeCollection(
		Attribute a, Collection<?> collection, String path, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) {
		if (collection != null && !collection.isEmpty())
			element.collections.add(new Triple<>(a, collection, path));
	}
	
	@Override
	protected void serializeCollectionValue(
		Collection<?> collection, String path, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) {
		if (!collection.isEmpty())
			element.collections.add(new Triple<>(null, collection, path));
	}
	
	@Override
	protected void startCollection(
		Attribute a, Collection<?> collection, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) {
	}
	
	@Override
	protected void endCollection(
		Attribute a, Collection<?> collection, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) {
	}
	
	@Override
	protected void startCollectionValue(Collection<?> collection, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized) {
	}
	
	@Override
	protected void endCollectionValue(Collection<?> collection, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized) {
	}
	
	@Override
	protected void startCollectionElement(
		Attribute a, Object element, int index, int size, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		if (pretty) {
			indent(output);
		}
		output.write('<');
		output.write(a.getName());
		elements.add(this.element);
		this.element = new Element(a.getName());
	}
	
	@Override
	protected void endCollectionElement(
		Attribute a, Object element, int index, int size, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		endElement(output, rules, alreadySerialized);
	}
	
	@Override
	protected void startCollectionValueElement(
		Object element, int index, int size, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		if (pretty) {
			indent(output);
		}
		output.write('<');
		output.write("element");
		elements.add(this.element);
		this.element = new Element("element");
	}
	
	@Override
	protected void endCollectionValueElement(
		Object element, int index, int size, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		endElement(output, rules, alreadySerialized);
	}
	
	@Override
	protected void serializeObjectAttribute(
		Attribute a, Object obj, String path, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) {
		element.objects.add(new Triple<>(a, obj, path));
	}
	
	@Override
	protected void serializeObjectValue(
		Object obj, String path, Buffered output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		super.serializeObject(obj, path, output, rules, alreadySerialized);
	}
}

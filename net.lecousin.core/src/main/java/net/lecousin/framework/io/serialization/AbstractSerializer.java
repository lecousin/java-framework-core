package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRule;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public abstract class AbstractSerializer<Output> implements Serializer {

	protected byte priority;
	
	protected abstract AsyncWork<Output, Exception> initializeOutput(IO.Writable output);
	
	protected abstract ISynchronizationPoint<? extends Exception> finalizeOutput(Output output);
	
	@Override
	public ISynchronizationPoint<Exception> serialize(Object object, IO.Writable output, List<SerializationRule> rules) {
		priority = output.getPriority();
		AsyncWork<Output, Exception> init = initializeOutput(output);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		init.listenAsynch(
			new Task.Cpu<Void, NoException>("Serialization", priority) {
				@Override
				public Void run() {
					Output out = init.getResult();
					ISynchronizationPoint<Exception> sp = serializeValue(object, "", out, rules);
					sp.listenInline(() -> {
						finalizeOutput(out).listenInlineSP(result);
					}, result);
					return null;
				}
			},
			result
		);
		return result;
	}
	
	public ISynchronizationPoint<Exception> serializeValue(
		Object value, String path, Output output, List<SerializationRule> rules
	) {
		if (value == null)
			return serializeNullValue(output);
		Class<?> type = value.getClass();

		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return serializeBooleanValue(((Boolean)value).booleanValue(), output);
		
		if (byte.class.equals(type) ||
			short.class.equals(type) ||
			int.class.equals(type) ||
			long.class.equals(type) ||
			float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type))
			return serializeNumericValue((Number)value, output);
		
		if (char.class.equals(type) || Character.class.equals(type))
			return serializeCharacterValue(((Character)value).charValue(), output);
		
		if (CharSequence.class.isAssignableFrom(type))
			return serializeStringValue((CharSequence)value, output);

		if (type.isEnum())
			return serializeStringValue(((Enum<?>)value).name(), output);

		if (Collection.class.isAssignableFrom(type))
			return serializeCollectionValue((Collection<?>)value, path, output, rules);

		if (Map.class.isAssignableFrom(type))
			return serializeMapValue((Map<?,?>)value, path, output, rules);
		
		if (InputStream.class.isAssignableFrom(type))
			return serializeInputStreamValue((InputStream)value, path, output, rules);
		
		if (IO.Readable.class.isAssignableFrom(type))
			return serializeIOReadableValue((IO.Readable)value, path, output, rules);

		return serializeObjectValue(value, path, output, rules);
	}
	
	// *** null ***
	
	protected abstract ISynchronizationPoint<Exception> serializeNullValue(Output output);
	
	protected abstract ISynchronizationPoint<Exception> serializeNullAttribute(Attribute a, String path, Output output);

	// *** boolean ***
	
	protected abstract ISynchronizationPoint<Exception> serializeBooleanValue(boolean value, Output output);

	protected abstract ISynchronizationPoint<Exception> serializeBooleanAttribute(boolean value, Attribute a, String path, Output output);
	
	// *** numeric ***
	
	protected abstract ISynchronizationPoint<Exception> serializeNumericValue(Number value, Output output);

	protected abstract ISynchronizationPoint<Exception> serializeNumericAttribute(Number value, Attribute a, String path, Output output);

	// *** character ***
	
	protected abstract ISynchronizationPoint<Exception> serializeCharacterValue(char value, Output output);

	protected abstract ISynchronizationPoint<Exception> serializeCharacterAttribute(char value, Attribute a, String path, Output output);

	// *** string ***
	
	protected abstract ISynchronizationPoint<Exception> serializeStringValue(CharSequence value, Output output);

	protected abstract ISynchronizationPoint<Exception> serializeStringAttribute(CharSequence value, Attribute a, String path, Output output);
	
	// *** Object ***
	
	protected ISynchronizationPoint<Exception> serializeObjectValue(Object value, String path, Output output, List<SerializationRule> rules) {
		Class<?> type = value.getClass();
		SerializationClass sc = new SerializationClass(new TypeDefinition(type));
		rules = TypeAnnotationToRule.addRules(type, rules);
		sc.apply(rules, value);
		return serializeObjectValue(sc, value, path, output, rules);
	}
	
	protected ISynchronizationPoint<Exception> serializeObjectValue(
		SerializationClass sc, Object value, String path, Output output, List<SerializationRule> rules
	) {
		List<Attribute> attributes = sortAttributes(sc.getAttributes());
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		serializeAttribute(attributes, 0, value, path, output, rules, result);
		return result;
	}
	
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		return attributes;
	}
	
	protected void serializeAttribute(
		List<Attribute> attributes, int attributeIndex, Object containerInstance, String containerPath,
		Output output, List<SerializationRule> rules, SynchronizationPoint<Exception> result
	) {
		if (attributeIndex >= attributes.size()) {
			result.unblock();
			return;
		}
		Attribute a = attributes.get(attributeIndex);
		if (a.ignore() || !a.canGet()) {
			serializeAttribute(attributes, attributeIndex + 1, containerInstance, containerPath, output, rules, result);
			return;
		}
		List<SerializationRule> newRules = AttributeAnnotationToRule.addRules(a, true, rules);
		ISynchronizationPoint<Exception> sp = serializeAttribute(
			a, containerPath + '.' + a.getOriginalName(), containerInstance, output, newRules);
		if (sp.isUnblocked()) {
			if (sp.hasError()) result.error(sp.getError());
			else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
			else serializeAttribute(attributes, attributeIndex + 1, containerInstance, containerPath, output, rules, result);
			return;
		}
		sp.listenAsynch(new Task.Cpu<Void, NoException>("Serialization", priority) {
			@Override
			public Void run() {
				if (sp.hasError()) result.error(sp.getError());
				else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
				else serializeAttribute(attributes, attributeIndex + 1, containerInstance, containerPath, output, rules, result);
				return null;
			}
		}, true);
	}
	
	protected ISynchronizationPoint<Exception> serializeAttribute(
		Attribute a, String path, Object containerInstance, Output output, List<SerializationRule> rules
	) {
		Object value;
		try { value = a.getValue(containerInstance); }
		catch (Exception e) {
			return new SynchronizationPoint<>(
				new Exception("Unable to get value of attribute " + a.getOriginalName()
					+ " on " + a.getOriginalType().getClass().getName(), e));
		}
		
		if (value == null)
			return serializeNullAttribute(a, path, output);
		
		Class<?> type = value.getClass();
		
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return serializeBooleanAttribute(((Boolean)value).booleanValue(), a, path, output);
		
		if (byte.class.equals(type) ||
			short.class.equals(type) ||
			int.class.equals(type) ||
			long.class.equals(type) ||
			float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type))
			return serializeNumericAttribute((Number)value, a, path, output);
		
		if (char.class.equals(type) || Character.class.equals(type))
			return serializeCharacterAttribute(((Character)value).charValue(), a, path, output);
		
		if (CharSequence.class.isAssignableFrom(type))
			return serializeStringAttribute((CharSequence)value, a, path, output);

		if (type.isEnum())
			return serializeStringAttribute(((Enum<?>)value).name(), a, path, output);

		if (Collection.class.isAssignableFrom(type))
			return serializeCollectionAttribute((Collection<?>)value, a, path, output, rules);

		if (Map.class.isAssignableFrom(type))
			return serializeMapAttribute((Map<?,?>)value, a, path, output, rules);
		
		if (InputStream.class.isAssignableFrom(type))
			return serializeInputStreamAttribute((InputStream)value, a, path, output, rules);
		
		if (IO.Readable.class.isAssignableFrom(type))
			return serializeIOReadableAttribute((IO.Readable)value, a, path, output, rules);

		return serializeObjectAttribute(value, a, path, output, rules);
	}
	
	protected abstract ISynchronizationPoint<Exception> serializeObjectAttribute(
		Object value, Attribute a, String path, Output output, List<SerializationRule> rules);
	
	// *** Collection ***
	
	protected ISynchronizationPoint<Exception> serializeCollectionValue(
		Collection<?> col, String path, Output output, List<SerializationRule> rules
	) {
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		serializeCollectionElement(col.iterator(), 0, path, output, rules, result);
		return result;
	}
	
	protected void serializeCollectionElement(
		Iterator<?> it, int elementIndex, String colPath, Output output, List<SerializationRule> rules, SynchronizationPoint<Exception> result
	) {
		if (!it.hasNext()) {
			result.unblock();
			return;
		}
		Object element = it.next();
		ISynchronizationPoint<Exception> sp = serializeValue(element, colPath + '[' + elementIndex + ']', output, rules);
		if (sp.isUnblocked()) {
			if (sp.hasError()) result.error(sp.getError());
			else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
			else serializeCollectionElement(it, elementIndex + 1, colPath, output, rules, result);
			return;
		}
		sp.listenAsynch(new Task.Cpu<Void, NoException>("Serialization", priority) {
			@Override
			public Void run() {
				if (sp.hasError()) result.error(sp.getError());
				else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
				else serializeCollectionElement(it, elementIndex + 1, colPath, output, rules, result);
				return null;
			}
		}, true);
	}

	protected abstract ISynchronizationPoint<Exception> serializeCollectionAttribute(
		Collection<?> col, Attribute a, String path, Output output, List<SerializationRule> rules);
	
	// *** Map ***
	
	protected abstract ISynchronizationPoint<Exception> serializeMapValue(
		Map<?,?> map, String path, Output output, List<SerializationRule> rules);
	
	protected abstract ISynchronizationPoint<Exception> serializeMapAttribute(
		Map<?,?> map, Attribute a, String path, Output output, List<SerializationRule> rules);

	// *** InputStream ***

	@SuppressWarnings("resource")
	protected ISynchronizationPoint<Exception> serializeInputStreamValue(
		InputStream in, String path, Output output, List<SerializationRule> rules) {
		return serializeIOReadableValue(new IOFromInputStream(in, in.toString(), Threading.getCPUTaskManager(), priority),
			path, output, rules);
	}

	@SuppressWarnings("resource")
	protected ISynchronizationPoint<Exception> serializeInputStreamAttribute(
		InputStream in, Attribute a, String path, Output output, List<SerializationRule> rules) {
		return serializeIOReadableAttribute(new IOFromInputStream(in, in.toString(), Threading.getCPUTaskManager(), priority),
			a, path, output, rules);
	}
	
	// *** IOReadable ***

	protected abstract ISynchronizationPoint<Exception> serializeIOReadableValue(
		IO.Readable io, String path, Output output, List<SerializationRule> rules);

	protected abstract ISynchronizationPoint<Exception> serializeIOReadableAttribute(
		IO.Readable io, Attribute a, String path, Output output, List<SerializationRule> rules);
}

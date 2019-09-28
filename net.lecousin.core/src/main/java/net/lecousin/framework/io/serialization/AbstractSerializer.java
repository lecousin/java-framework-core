package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationUtil.MapEntry;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnAttribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Implements most of the logic of a serializer. */
public abstract class AbstractSerializer implements Serializer {

	protected byte priority;
	
	protected abstract IAsync<SerializationException> initializeSerialization(IO.Writable output);
	
	protected abstract IAsync<SerializationException> finalizeSerialization();
	
	/** Utility class to create tasks along the process. */
	protected class SerializationTask extends Task.Cpu<Void, NoException> {
		public SerializationTask(Runnable r) {
			super("Serialization", priority);
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() throws CancelException {
			try { r.run(); }
			catch (Exception t) {
				throw new CancelException("Error thrown by serialization", t);
			}
			return null;
		}
	}
	
	@Override
	public IAsync<SerializationException> serialize(
		Object object, TypeDefinition typeDef, IO.Writable output, List<SerializationRule> rules
	) {
		priority = output.getPriority();
		IAsync<SerializationException> init = initializeSerialization(output);
		Async<SerializationException> result = new Async<>();
		init.thenStart(new SerializationTask(() -> {
			IAsync<SerializationException> sp = serializeValue(null, object, typeDef, "", rules);
			sp.onDone(() -> finalizeSerialization().onDone(result), result);
		}), result);
		return result;
	}
	
	
	protected List<SerializationRule> addRulesForType(SerializationClass type, List<SerializationRule> currentList) {
		currentList = TypeAnnotationToRule.addRules(type, currentList);
		currentList = AttributeAnnotationToRuleOnType.addRules(type, true, currentList);
		return currentList;
	}
	
	protected List<SerializationRule> addRulesForAttribute(Attribute a, List<SerializationRule> currentList) {
		currentList = AttributeAnnotationToRuleOnAttribute.addRules(a, true, currentList);
		return currentList;
	}
	
	/** Serialize a value. */
	public IAsync<SerializationException> serializeValue(
		SerializationContext context, Object value, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		if (value == null)
			return serializeNullValue();
		
		for (SerializationRule rule : rules)
			try {
				value = rule.convertSerializationValue(value, typeDef, context);
			} catch (SerializationException e) {
				return new Async<>(e);
			}

		Class<?> type = value.getClass();
		
		if (type.isArray()) {
			if (value instanceof byte[])
				return serializeByteArrayValue(context, (byte[])value, path, rules);
			Class<?> elementType = type.getComponentType();
			CollectionContext ctx = new CollectionContext(context, value, typeDef, new TypeDefinition(elementType));
			return serializeCollectionValue(ctx, path, rules);
		}
				
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return serializeBooleanValue(((Boolean)value).booleanValue());
		
		if (byte.class.equals(type) ||
			short.class.equals(type) ||
			int.class.equals(type) ||
			long.class.equals(type) ||
			float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type))
			return serializeNumericValue((Number)value);
		
		if (char.class.equals(type) || Character.class.equals(type))
			return serializeCharacterValue(((Character)value).charValue());
		
		if (CharSequence.class.isAssignableFrom(type))
			return serializeStringValue((CharSequence)value);

		if (type.isEnum())
			return serializeStringValue(((Enum<?>)value).name());

		if (Collection.class.isAssignableFrom(type)) {
			TypeDefinition elementType;
			if (typeDef.getParameters().isEmpty())
				elementType = null;
			else
				elementType = typeDef.getParameters().get(0);
			CollectionContext ctx = new CollectionContext(context, value, typeDef, elementType);
			return serializeCollectionValue(ctx, path, rules);
		}

		if (Map.class.isAssignableFrom(type))
			return serializeMapValue(context, (Map<?,?>)value, typeDef, path, rules);
		
		if (InputStream.class.isAssignableFrom(type))
			return serializeInputStreamValue(context, (InputStream)value, path, rules);
		
		if (IO.Readable.class.isAssignableFrom(type))
			return serializeIOReadableValue(context, (IO.Readable)value, path, rules);

		return serializeObjectValue(context, value, typeDef, path, rules);
	}
	
	// *** null ***
	
	protected abstract IAsync<SerializationException> serializeNullValue();
	
	protected abstract IAsync<SerializationException> serializeNullAttribute(AttributeContext context, String path);

	// *** boolean ***
	
	protected abstract IAsync<SerializationException> serializeBooleanValue(boolean value);

	protected abstract IAsync<SerializationException> serializeBooleanAttribute(
		AttributeContext context, boolean value, String path);
	
	// *** numeric ***
	
	protected abstract IAsync<SerializationException> serializeNumericValue(Number value);

	protected abstract IAsync<SerializationException> serializeNumericAttribute(
		AttributeContext context, Number value, String path);

	// *** character ***
	
	protected abstract IAsync<SerializationException> serializeCharacterValue(char value);

	protected abstract IAsync<SerializationException> serializeCharacterAttribute(
		AttributeContext context, char value, String path);

	// *** string ***
	
	protected abstract IAsync<SerializationException> serializeStringValue(CharSequence value);

	protected abstract IAsync<SerializationException> serializeStringAttribute(
		AttributeContext context, CharSequence value, String path);
	
	// *** Object ***
	
	protected IAsync<SerializationException> serializeObjectValue(
		SerializationContext context, Object value, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		ObjectContext ctx;
		try {
			Class<?> type = value.getClass();
			SerializationClass sc = new SerializationClass(
				typeDef != null ? TypeDefinition.from(type, typeDef) : new TypeDefinition(type));
			ctx = new ObjectContext(context, value, sc, typeDef);
			rules = addRulesForType(sc, rules);
			sc.apply(rules, ctx, true);
		} catch (Exception e) {
			return new Async<>(new SerializationException("Error serializing object " + typeDef, e));
		}
		return serializeObjectValue(ctx, path, rules);
	}
	
	protected IAsync<SerializationException> serializeObjectValue(
		ObjectContext context, String path, List<SerializationRule> rules
	) {
		List<Attribute> attributes = sortAttributes(context.getSerializationClass().getAttributes());
		IAsync<SerializationException> start = startObjectValue(context, path, rules);
		Async<SerializationException> result = new Async<>();
		if (start.isDone()) {
			if (start.hasError()) return start;
			serializeAttribute(attributes, 0, context, path, rules, result);
		} else {
			start.thenStart(new SerializationTask(() -> serializeAttribute(attributes, 0, context, path, rules, result)), result);
		}
		return result;
	}
	
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		return attributes;
	}
	
	protected abstract IAsync<SerializationException> startObjectValue(
		ObjectContext context, String path, List<SerializationRule> rules);

	protected abstract IAsync<SerializationException> endObjectValue(
		ObjectContext context, String path, List<SerializationRule> rules);
	
	protected void serializeAttribute(
		List<Attribute> attributes, int attributeIndex, ObjectContext context, String containerPath,
		List<SerializationRule> rules, Async<SerializationException> result
	) {
		do {
			if (attributeIndex >= attributes.size()) {
				endObjectValue(context, containerPath, rules).onDone(result);
				return;
			}
			Attribute a = attributes.get(attributeIndex);
			if (a.ignore() || !a.canGet()) {
				attributeIndex++;
				continue;
			}
			AttributeContext ctx = new AttributeContext(context, a);
			List<SerializationRule> newRules = addRulesForAttribute(a, rules);
			IAsync<SerializationException> sp = serializeAttribute(
				ctx, containerPath + '.' + a.getOriginalName(), newRules);
			if (sp.isDone()) {
				if (sp.hasError()) result.error(sp.getError());
				else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
				else {
					attributeIndex++;
					continue;
				}
				return;
			}
			int nextIndex = attributeIndex + 1;
			sp.thenStart(new Task.Cpu<Void, NoException>("Serialization", priority) {
				@Override
				public Void run() {
					if (sp.hasError()) result.error(sp.getError());
					else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
					else serializeAttribute(attributes, nextIndex, context, containerPath, rules, result);
					return null;
				}
			}, true);
			return;
		} while (true);
	}
	
	protected IAsync<SerializationException> serializeAttribute(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		Object value;
		try { value = context.getAttribute().getValue(context.getParent().getInstance()); }
		catch (Exception e) {
			return new Async<>(
				new SerializationException("Unable to get value of attribute " + context.getAttribute().getOriginalName()
					+ " on " + context.getParent().getInstance().getClass().getName(), e));
		}
		
		if (value == null)
			return serializeNullAttribute(context, path);
		
		Class<?> type = value.getClass();
		
		if (type.isArray()) {
			if (byte[].class.equals(type))
				return serializeByteArrayAttribute(context, (byte[])value, path, rules);
			Class<?> elementType = type.getComponentType();
			CollectionContext ctx = new CollectionContext(
				context, value, context.getAttribute().getType(), new TypeDefinition(elementType));
			return serializeCollectionAttribute(ctx, path, rules);
		}
		
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return serializeBooleanAttribute(context, ((Boolean)value).booleanValue(), path);
		
		if (byte.class.equals(type) ||
			short.class.equals(type) ||
			int.class.equals(type) ||
			long.class.equals(type) ||
			float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type))
			return serializeNumericAttribute(context, (Number)value, path);
		
		if (char.class.equals(type) || Character.class.equals(type))
			return serializeCharacterAttribute(context, ((Character)value).charValue(), path);
		
		if (CharSequence.class.isAssignableFrom(type))
			return serializeStringAttribute(context, (CharSequence)value, path);

		if (type.isEnum())
			return serializeStringAttribute(context, ((Enum<?>)value).name(), path);

		if (Collection.class.isAssignableFrom(type)) {
			TypeDefinition elementType;
			if (context.getAttribute().getType().getParameters().isEmpty())
				elementType = null;
			else
				elementType = context.getAttribute().getType().getParameters().get(0);
			CollectionContext ctx = new CollectionContext(context, value, context.getAttribute().getType(), elementType);
			return serializeCollectionAttribute(ctx, path, rules);
		}

		if (Map.class.isAssignableFrom(type))
			return serializeMapAttribute(context, (Map<?,?>)value, context.getAttribute().getType(), path, rules);
		
		if (InputStream.class.isAssignableFrom(type))
			return serializeInputStreamAttribute(context, (InputStream)value, path, rules);
		
		if (IO.Readable.class.isAssignableFrom(type))
			return serializeIOReadableAttribute(context, (IO.Readable)value, path, rules);

		return serializeObjectAttribute(context, value, path, rules);
	}
	
	protected abstract IAsync<SerializationException> serializeObjectAttribute(
		AttributeContext context, Object value, String path, List<SerializationRule> rules);
	
	// *** Collection ***
	
	protected IAsync<SerializationException> serializeCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	) {
		IAsync<SerializationException> start = startCollectionValue(context, path, rules);
		Async<SerializationException> result = new Async<>();
		if (start.isDone()) {
			serializeCollectionElement(context, context.getIterator(), 0, path, rules, result);
			return result;
		}
		start.thenStart(new SerializationTask(() ->
			serializeCollectionElement(context, context.getIterator(), 0, path, rules, result)), result);
		return result;
	}
	
	protected abstract IAsync<SerializationException> startCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	);

	protected abstract IAsync<SerializationException> endCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	);

	protected abstract IAsync<SerializationException> startCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	);

	protected abstract IAsync<SerializationException> endCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	);
	
	@SuppressWarnings("squid:S3776") // complexity
	protected void serializeCollectionElement(
		CollectionContext context, Iterator<?> it, int elementIndex, String colPath,
		List<SerializationRule> rules, Async<SerializationException> result
	) {
		do {
			if (!it.hasNext()) {
				endCollectionValue(context, colPath, rules).onDone(result);
				return;
			}
			Object element = it.next();
			String elementPath = colPath + '[' + elementIndex + ']';
			
			IAsync<SerializationException> start =
				startCollectionValueElement(context, element, elementIndex, elementPath, rules);
			
			Async<SerializationException> value = new Async<>();
			if (start.isDone()) {
				if (start.hasError()) value.error(start.getError());
				else serializeValue(context, element, context.getElementType(), elementPath, rules).onDone(value);
			} else {
				start.thenStart(new SerializationTask(() ->
					serializeValue(context, element, context.getElementType(), elementPath, rules).onDone(value)), value);
			}
			
			Async<SerializationException> next = new Async<>();
			int currentIndex = elementIndex;
			if (value.isDone()) {
				if (value.hasError()) next.error(value.getError());
				else endCollectionValueElement(context, element, elementIndex, elementPath, rules).onDone(next);
			} else {
				value.thenStart(new SerializationTask(() ->
					endCollectionValueElement(context, element, currentIndex, elementPath, rules).onDone(next)), next);
			}
			
			if (next.isDone()) {
				if (next.forwardIfNotSuccessful(result))
					return;
				elementIndex++;
				continue;
			}
			next.thenStart(new SerializationTask(() -> {
				if (next.hasError()) result.error(next.getError());
				else serializeCollectionElement(context, it, currentIndex + 1, colPath, rules, result);
			}), result);
			return;
		} while (true);
	}

	protected abstract IAsync<SerializationException> serializeCollectionAttribute(
		CollectionContext context, String path, List<SerializationRule> rules);
	
	// *** Map ***
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected IAsync<SerializationException> serializeMapValue(
		SerializationContext context, Map<?,?> map, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, typeDef.getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		ArrayList<MapEntry> entries = new ArrayList<>(map.size());
		for (Map.Entry e : map.entrySet()) {
			MapEntry me = new MapEntry();
			me.key = e.getKey();
			me.value = e.getValue();
			entries.add(me);
		}
		return serializeValue(context, entries, type, path, rules);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected IAsync<SerializationException> serializeMapAttribute(
		AttributeContext context, Map<?,?> map, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, typeDef.getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		ArrayList<MapEntry> entries = new ArrayList<>(map.size());
		for (Map.Entry e : map.entrySet()) {
			MapEntry me = new MapEntry();
			me.key = e.getKey();
			me.value = e.getValue();
			entries.add(me);
		}
		CollectionContext ctx = new CollectionContext(context, entries, type, type.getParameters().get(0));
		return serializeCollectionAttribute(ctx, path, rules);
	}

	// *** InputStream ***

	protected IAsync<SerializationException> serializeInputStreamValue(
		SerializationContext context, InputStream in, String path, List<SerializationRule> rules) {
		return serializeIOReadableValue(context, new IOFromInputStream(in, in.toString(), Threading.getUnmanagedTaskManager(), priority),
			path, rules);
	}

	protected IAsync<SerializationException> serializeInputStreamAttribute(
		AttributeContext context, InputStream in, String path, List<SerializationRule> rules) {
		return serializeIOReadableAttribute(context, 
			new IOFromInputStream(in, in.toString(), Threading.getUnmanagedTaskManager(), priority),
			path, rules);
	}
	
	// *** IOReadable ***

	protected abstract IAsync<SerializationException> serializeIOReadableValue(
		SerializationContext context, IO.Readable io, String path, List<SerializationRule> rules);

	protected abstract IAsync<SerializationException> serializeIOReadableAttribute(
		AttributeContext context, IO.Readable io, String path, List<SerializationRule> rules);
	
	// *** byte[] by default using IO ***
	
	protected IAsync<SerializationException> serializeByteArrayValue(
		SerializationContext context, byte[] bytes, String path, List<SerializationRule> rules
	) {
		return serializeIOReadableValue(context, new ByteArrayIO(bytes, "serialization of byte[]"), path, rules);
	}
	
	protected IAsync<SerializationException> serializeByteArrayAttribute(
			AttributeContext context, byte[] bytes, String path, List<SerializationRule> rules
	) {
		return serializeIOReadableAttribute(context, new ByteArrayIO(bytes, "serialization of byte[]"), path, rules);
	}
}

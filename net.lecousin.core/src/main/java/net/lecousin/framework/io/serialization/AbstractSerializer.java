package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationUtil.MapEntry;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnAttribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public abstract class AbstractSerializer implements Serializer {

	protected byte priority;
	
	protected abstract ISynchronizationPoint<? extends Exception> initializeSerialization(IO.Writable output);
	
	protected abstract ISynchronizationPoint<? extends Exception> finalizeSerialization();
	
	protected class SerializationTask extends Task.Cpu<Void, NoException> {
		public SerializationTask(Runnable r) {
			super("Serialization", priority);
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() throws CancelException {
			try { r.run(); }
			catch (Throwable t) {
				throw new CancelException("Error thrown by serialization", t);
			}
			return null;
		}
	}
	
	@Override
	public ISynchronizationPoint<Exception> serialize(Object object, TypeDefinition typeDef, IO.Writable output, List<SerializationRule> rules) {
		priority = output.getPriority();
		ISynchronizationPoint<? extends Exception> init = initializeSerialization(output);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		init.listenAsyncSP(new SerializationTask(() -> {
			ISynchronizationPoint<? extends Exception> sp = serializeValue(null, object, typeDef, "", rules);
			sp.listenInlineSP(() -> {
				finalizeSerialization().listenInlineSP(result);
			}, result);
		}), result);
		return result;
	}
	
	
	protected List<SerializationRule> addRulesForType(SerializationClass type, List<SerializationRule> currentList) {
		currentList = TypeAnnotationToRule.addRules(type.getType().getBase(), currentList);
		currentList = AttributeAnnotationToRuleOnType.addRules(type, true, currentList);
		return currentList;
	}
	
	protected List<SerializationRule> addRulesForAttribute(Attribute a, List<SerializationRule> currentList) {
		return AttributeAnnotationToRuleOnAttribute.addRules(a, true, currentList);
	}
	
	public ISynchronizationPoint<? extends Exception> serializeValue(
		SerializationContext context, Object value, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		if (value == null)
			return serializeNullValue();
		
		for (SerializationRule rule : rules)
			value = rule.convertSerializationValue(value, typeDef, context);

		Class<?> type = value.getClass();
		
		if (type.isArray()) {
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
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeNullValue();
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeNullAttribute(AttributeContext context, String path);

	// *** boolean ***
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeBooleanValue(boolean value);

	protected abstract ISynchronizationPoint<? extends Exception> serializeBooleanAttribute(AttributeContext context, boolean value, String path);
	
	// *** numeric ***
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeNumericValue(Number value);

	protected abstract ISynchronizationPoint<? extends Exception> serializeNumericAttribute(AttributeContext context, Number value, String path);

	// *** character ***
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeCharacterValue(char value);

	protected abstract ISynchronizationPoint<? extends Exception> serializeCharacterAttribute(AttributeContext context, char value, String path);

	// *** string ***
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeStringValue(CharSequence value);

	protected abstract ISynchronizationPoint<? extends Exception> serializeStringAttribute(AttributeContext context, CharSequence value, String path);
	
	// *** Object ***
	
	protected ISynchronizationPoint<? extends Exception> serializeObjectValue(SerializationContext context, Object value, TypeDefinition typeDef, String path, List<SerializationRule> rules) {
		Class<?> type = value.getClass();
		SerializationClass sc = new SerializationClass(typeDef != null ? TypeDefinition.from(type, typeDef) : new TypeDefinition(type));
		ObjectContext ctx = new ObjectContext(context, value, sc, typeDef);
		rules = addRulesForType(sc, rules);
		sc.apply(rules, ctx);
		return serializeObjectValue(ctx, path, rules);
	}
	
	protected ISynchronizationPoint<? extends Exception> serializeObjectValue(
		ObjectContext context, String path, List<SerializationRule> rules
	) {
		List<Attribute> attributes = sortAttributes(context.getSerializationClass().getAttributes());
		ISynchronizationPoint<? extends Exception> start = startObjectValue(context, path, rules);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		if (start.isUnblocked()) {
			if (start.hasError()) return start;
			serializeAttribute(attributes, 0, context, path, rules, result);
		} else
			start.listenAsyncSP(new SerializationTask(() -> {
				serializeAttribute(attributes, 0, context, path, rules, result);
			}), result);
		return result;
	}
	
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		return attributes;
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> startObjectValue(ObjectContext context, String path, List<SerializationRule> rules);

	protected abstract ISynchronizationPoint<? extends Exception> endObjectValue(ObjectContext context, String path, List<SerializationRule> rules);
	
	protected void serializeAttribute(
		List<Attribute> attributes, int attributeIndex, ObjectContext context, String containerPath,
		List<SerializationRule> rules, SynchronizationPoint<Exception> result
	) {
		do {
			if (attributeIndex >= attributes.size()) {
				endObjectValue(context, containerPath, rules).listenInlineSP(result);
				return;
			}
			Attribute a = attributes.get(attributeIndex);
			if (a.ignore() || !a.canGet()) {
				attributeIndex++;
				continue;
			}
			AttributeContext ctx = new AttributeContext(context, a);
			List<SerializationRule> newRules = addRulesForAttribute(a, rules);
			ISynchronizationPoint<? extends Exception> sp = serializeAttribute(
				ctx, containerPath + '.' + a.getOriginalName(), newRules);
			if (sp.isUnblocked()) {
				if (sp.hasError()) result.error(sp.getError());
				else if (sp.isCancelled()) result.cancel(sp.getCancelEvent());
				else {
					attributeIndex++;
					continue;
				}
				return;
			}
			int nextIndex = attributeIndex + 1;
			sp.listenAsync(new Task.Cpu<Void, NoException>("Serialization", priority) {
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
	
	protected ISynchronizationPoint<? extends Exception> serializeAttribute(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		Object value;
		try { value = context.getAttribute().getValue(context.getParent().getInstance()); }
		catch (Exception e) {
			return new SynchronizationPoint<>(
				new Exception("Unable to get value of attribute " + context.getAttribute().getOriginalName()
					+ " on " + context.getParent().getInstance().getClass().getName(), e));
		}
		
		if (value == null)
			return serializeNullAttribute(context, path);
		
		Class<?> type = value.getClass();
		
		if (type.isArray()) {
			Class<?> elementType = type.getComponentType();
			CollectionContext ctx = new CollectionContext(context, value, context.getAttribute().getType(), new TypeDefinition(elementType));
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
	
	protected abstract ISynchronizationPoint<? extends Exception> serializeObjectAttribute(
		AttributeContext context, Object value, String path, List<SerializationRule> rules);
	
	// *** Collection ***
	
	protected ISynchronizationPoint<? extends Exception> serializeCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	) {
		ISynchronizationPoint<? extends Exception> start = startCollectionValue(context, path, rules);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		if (start.isUnblocked()) {
			serializeCollectionElement(context, context.getIterator(), 0, path, rules, result);
			return result;
		}
		start.listenAsyncSP(new SerializationTask(() -> {
			serializeCollectionElement(context, context.getIterator(), 0, path, rules, result);
		}), result);
		return result;
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> startCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	);

	protected abstract ISynchronizationPoint<? extends Exception> endCollectionValue(
		CollectionContext context, String path, List<SerializationRule> rules
	);

	protected abstract ISynchronizationPoint<? extends Exception> startCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	);

	protected abstract ISynchronizationPoint<? extends Exception> endCollectionValueElement(
		CollectionContext context, Object element, int elementIndex, String elementPath, List<SerializationRule> rules
	);
	
	protected void serializeCollectionElement(
		CollectionContext context, Iterator<?> it, int elementIndex, String colPath, List<SerializationRule> rules, SynchronizationPoint<Exception> result
	) {
		do {
			if (!it.hasNext()) {
				endCollectionValue(context, colPath, rules).listenInlineSP(result);
				return;
			}
			Object element = it.next();
			String elementPath = colPath + '[' + elementIndex + ']';
			
			ISynchronizationPoint<? extends Exception> start = startCollectionValueElement(context, element, elementIndex, elementPath, rules);
			
			SynchronizationPoint<Exception> value = new SynchronizationPoint<>();
			if (start.isUnblocked()) {
				if (start.hasError()) value.error(start.getError());
				else serializeValue(context, element, context.getElementType(), elementPath, rules).listenInlineSP(value);
			} else {
				start.listenAsyncSP(new SerializationTask(() -> {
					serializeValue(context, element, context.getElementType(), elementPath, rules).listenInlineSP(value);
				}), value);
			}
			
			SynchronizationPoint<Exception> next = new SynchronizationPoint<>();
			int currentIndex = elementIndex;
			if (value.isUnblocked()) {
				if (value.hasError()) next.error(value.getError());
				else endCollectionValueElement(context, element, elementIndex, elementPath, rules).listenInlineSP(next);
			} else {
				value.listenAsyncSP(new SerializationTask(() -> {
					endCollectionValueElement(context, element, currentIndex, elementPath, rules).listenInlineSP(next);
				}), next);
			}
			
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				elementIndex++;
				continue;
			}
			next.listenAsync(new SerializationTask(() -> {
				if (next.hasError()) result.error(next.getError());
				else serializeCollectionElement(context, it, currentIndex + 1, colPath, rules, result);
			}), result);
			return;
		} while (true);
	}

	protected abstract ISynchronizationPoint<? extends Exception> serializeCollectionAttribute(
		CollectionContext context, String path, List<SerializationRule> rules);
	
	// *** Map ***
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ISynchronizationPoint<? extends Exception> serializeMapValue(
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
	protected ISynchronizationPoint<? extends Exception> serializeMapAttribute(
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

	@SuppressWarnings("resource")
	protected ISynchronizationPoint<? extends Exception> serializeInputStreamValue(
		SerializationContext context, InputStream in, String path, List<SerializationRule> rules) {
		return serializeIOReadableValue(context, new IOFromInputStream(in, in.toString(), Threading.getUnmanagedTaskManager(), priority),
			path, rules);
	}

	@SuppressWarnings("resource")
	protected ISynchronizationPoint<? extends Exception> serializeInputStreamAttribute(
		AttributeContext context, InputStream in, String path, List<SerializationRule> rules) {
		return serializeIOReadableAttribute(context, 
			new IOFromInputStream(in, in.toString(), Threading.getUnmanagedTaskManager(), priority),
			path, rules);
	}
	
	// *** IOReadable ***

	protected abstract ISynchronizationPoint<? extends Exception> serializeIOReadableValue(
		SerializationContext context, IO.Readable io, String path, List<SerializationRule> rules);

	protected abstract ISynchronizationPoint<? extends Exception> serializeIOReadableAttribute(
		AttributeContext context, IO.Readable io, String path, List<SerializationRule> rules);
}

package net.lecousin.framework.io.serialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO.Writable;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/**
 * Base implementation of a serializer, decomposing the serialization process into overridable methods.
 * @param <Out> type of output it uses
 */
public abstract class AbstractSerializer<Out> implements Serializer {

	@Override
	public AsyncWork<Void, Exception> serialize(Object obj, List<SerializationRule> rules, Writable output, byte priority) {
		AsyncWork<Void,Exception> result = new AsyncWork<>();
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Serialization: " + getClass().getSimpleName(), priority) {
			@Override
			public Void run() {
				Out out = adaptOutput(output);
				Set<Object> alreadySerialized = new HashSet<Object>();
				try {
					startSerialization(obj, out, rules, alreadySerialized);
					serializeValue(obj, "", out, rules, alreadySerialized);
					ISynchronizationPoint<? extends Exception> flushing = endSerialization(obj, out, rules, alreadySerialized);
					if (flushing == null)
						result.unblockSuccess(null);
					else
						flushing.listenInline(new Runnable() {
							@Override
							public void run() {
								if (flushing.isCancelled())
									result.unblockCancel(flushing.getCancelEvent());
								else if (flushing.hasError())
									result.unblockError(flushing.getError());
								else
									result.unblockSuccess(null);
							}
						});
				} catch (Exception e) {
					result.unblockError(e);
				}
				return null;
			}
		};
		task.start();
		return result;
	}
	
	protected abstract Out adaptOutput(Writable output);
	
	protected abstract void startSerialization(
		Object rootObject, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	/**
	 * This method must flush any buffered output, to make sure everything has been written before to unblock the returned synchronization point.
	 */
	protected abstract ISynchronizationPoint<? extends Exception> endSerialization(
		Object rootObject, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected void serializeObject(
		Object obj, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		if (obj == null) {
			serializeNullValue(output);
			return;
		}
		
		if (alreadySerialized.contains(obj))
			throw new RecursiveSerializationException(obj, path);
		alreadySerialized.add(obj);
		
		Class<?> cl = obj.getClass();
		startObject(obj, cl, output);
		ArrayList<Attribute> attributes = SerializationUtil.getAttributes(cl);
		SerializationUtil.removeNonGettableAttributes(attributes);
		for (SerializationRule rule : rules)
			rule.apply(attributes);
		for (SerializationRule rule : SerializationUtil.processAnnotations(attributes, true, false))
			rule.apply(attributes);
		SerializationUtil.removeIgnoredAttributes(attributes);
		for (Attribute attribute : attributes)
			serializeAttribute(attribute, obj, path + '.' + attribute.getOriginalName(), output, rules, alreadySerialized);
		endObject(obj, cl, output);
	}
	
	protected abstract void startObject(Object obj, Class<?> cl, Out output) throws Exception;
	
	protected abstract void endObject(Object obj, Class<?> cl, Out output) throws Exception;
	
	protected void serializeAttribute(
		Attribute attr, Object obj, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		Object value = attr.getValue(obj);
		
		if (boolean.class.equals(attr.getType()) || Boolean.class.equals(attr.getType())) {
			serializeBooleanAttribute(attr, (Boolean)value, output);
			return;
		}
		if (byte.class.equals(attr.getType()) ||
			short.class.equals(attr.getType()) ||
			int.class.equals(attr.getType()) ||
			long.class.equals(attr.getType()) ||
			float.class.equals(attr.getType()) ||
			double.class.equals(attr.getType()) ||
			Number.class.isAssignableFrom(attr.getType())) {
			serializeNumericAttribute(attr, (Number)value, output);
			return;
		}
		if (char.class.equals(attr.getType()) || Character.class.equals(attr.getType())) {
			serializeCharacterAttribute(attr, (Character)value, output);
			return;
		}
		if (CharSequence.class.isAssignableFrom(attr.getType())) {
			serializeStringAttribute(attr, (CharSequence)value, output);
			return;
		}
		if (Collection.class.isAssignableFrom(attr.getType())) {
			serializeCollection(attr, (Collection<?>)value, path, output, rules, alreadySerialized);
			return;
		}
		if (attr.getType().isEnum()) {
			serializeStringAttribute(attr, value == null ? null : ((Enum<?>)value).name(), output);
			return;
		}
		// TODO map
		// TODO streams (IO, InputStream, File...)
		serializeObjectAttribute(attr, value, path, output, rules, alreadySerialized);
	}
	
	protected void serializeValue(
		Object value, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		if (value == null) {
			serializeNullValue(output);
			return;
		}
		if (alreadySerialized.contains(value))
			throw new RecursiveSerializationException(value, path);
		Class<?> type = value.getClass();

		if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			serializeBooleanValue((Boolean)value, output);
			return;
		}
		if (byte.class.equals(type) ||
			short.class.equals(type) ||
			int.class.equals(type) ||
			long.class.equals(type) ||
			float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type)) {
			serializeNumericValue((Number)value, output);
			return;
		}
		if (char.class.equals(type) || Character.class.equals(type)) {
			serializeCharacterValue((Character)value, output);
			return;
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			serializeStringValue((CharSequence)value, output);
			return;
		}
		if (Collection.class.isAssignableFrom(type)) {
			serializeCollectionValue((Collection<?>)value, path, output, rules, alreadySerialized);
			return;
		}
		if (type.isEnum()) {
			serializeStringValue(((Enum<?>)value).name(), output);
			return;
		}
		// TODO map
		// TODO streams (IO, InputStream, File...)
		serializeObjectValue(value, path, output, rules, alreadySerialized);
	}
	
	protected abstract void serializeNullValue(Out output) throws Exception;
	
	protected abstract void serializeBooleanAttribute(Attribute attr, Boolean value, Out output) throws Exception;
	
	protected abstract void serializeBooleanValue(Boolean value, Out output) throws Exception;
	
	protected abstract void serializeNumericAttribute(Attribute attr, Number value, Out output) throws Exception;
	
	protected abstract void serializeNumericValue(Number value, Out output) throws Exception;
	
	protected abstract void serializeCharacterAttribute(Attribute attr, Character value, Out output) throws Exception;
	
	protected abstract void serializeCharacterValue(Character value, Out output) throws Exception;
	
	protected abstract void serializeStringAttribute(Attribute attr, CharSequence value, Out output) throws Exception;
	
	protected abstract void serializeStringValue(CharSequence value, Out output) throws Exception;
	
	protected void serializeCollection(
		Attribute attr, Collection<?> collection, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		startCollection(attr, collection, output, rules, alreadySerialized);
		if (collection != null) {
			int index = 0;
			int size = collection.size();
			for (Object element : collection) {
				serializeCollectionElement(attr, element, index, size, path, output, rules, alreadySerialized);
				index++;
			}
		}
		endCollection(attr, collection, output, rules, alreadySerialized);
	}

	protected void serializeCollectionValue(
		Collection<?> collection, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		startCollectionValue(collection, output, rules, alreadySerialized);
		if (collection != null) {
			int index = 0;
			int size = collection.size();
			for (Object element : collection) {
				serializeCollectionValueElement(element, index, size, path, output, rules, alreadySerialized);
				index++;
			}
		}
		endCollectionValue(collection, output, rules, alreadySerialized);
	}
	
	protected abstract void startCollection(
		Attribute attr, Collection<?> collection, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected void serializeCollectionElement(
		Attribute attr, Object element, int index, int size, String collectionPath, Out output,
		List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		startCollectionElement(attr, element, index, size, output, rules, alreadySerialized);
		if (element == null) {
			serializeNullValue(output);
		} else {
			if (alreadySerialized.contains(element))
				throw new RecursiveSerializationException(element, collectionPath + '[' + index + ']');
			serializeValue(element, collectionPath + '[' + index + ']', output, rules, alreadySerialized);
		}
		endCollectionElement(attr, element, index, size, output, rules, alreadySerialized);
	}
	
	protected abstract void endCollection(
		Attribute attr, Collection<?> collection, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void startCollectionElement(
		Attribute attr, Object element, int index, int size, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void endCollectionElement(
		Attribute attr, Object element, int index, int size, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;

	protected abstract void startCollectionValue(
		Collection<?> collection, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected void serializeCollectionValueElement(
		Object element, int index, int size, String collectionPath, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception {
		startCollectionValueElement(element, index, size, output, rules, alreadySerialized);
		if (element == null) {
			serializeNullValue(output);
		} else {
			if (alreadySerialized.contains(element))
				throw new RecursiveSerializationException(element, collectionPath + '[' + index + ']');
			serializeValue(element, collectionPath + '[' + index + ']', output, rules, alreadySerialized);
		}
		endCollectionValueElement(element, index, size, output, rules, alreadySerialized);
	}
	
	protected abstract void endCollectionValue(
		Collection<?> collection, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void startCollectionValueElement(
		Object element, int index, int size, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void endCollectionValueElement(
		Object element, int index, int size, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void serializeObjectAttribute(
		Attribute attr, Object obj, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;
	
	protected abstract void serializeObjectValue(
		Object obj, String path, Out output, List<SerializationRule> rules, Set<Object> alreadySerialized
	) throws Exception;

}

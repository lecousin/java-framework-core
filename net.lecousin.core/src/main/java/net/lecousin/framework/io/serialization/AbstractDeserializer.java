package net.lecousin.framework.io.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationUtil.MapEntry;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnAttribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.math.IntegerUnit.Unit;
import net.lecousin.framework.math.IntegerUnit.UnitConversionException;
import net.lecousin.framework.util.Pair;

/** Implements most of the logic of a deserializer. */
public abstract class AbstractDeserializer implements Deserializer {
	
	protected byte priority;
	protected int maxTextSize = -1;
	
	protected abstract IAsync<SerializationException> initializeDeserialization(IO.Readable input);
	
	protected abstract IAsync<SerializationException> finalizeDeserialization();
	
	@Override
	public int getMaximumTextSize() {
		return maxTextSize;
	}
	
	@Override
	public void setMaximumTextSize(int max) {
		maxTextSize = max;
	}
	
	@Override
	public <T> AsyncSupplier<T, SerializationException> deserialize(TypeDefinition type, IO.Readable input, List<SerializationRule> rules) {
		priority = input.getPriority();
		IAsync<SerializationException> init = initializeDeserialization(input);
		AsyncSupplier<T, SerializationException> result = new AsyncSupplier<>();
		init.thenStart(new DeserializationTask(() -> {
			AsyncSupplier<T, SerializationException> sp = deserializeValue(null, type, "", rules);
			sp.onDone(obj -> finalizeDeserialization().onDone(() -> result.unblockSuccess(obj), result), result);
		}), result);
		return result;
	}
	
	protected List<SerializationRule> addRulesForType(SerializationClass type, List<SerializationRule> currentList) {
		currentList = TypeAnnotationToRule.addRules(type, currentList);
		currentList = AttributeAnnotationToRuleOnType.addRules(type, false, currentList);
		return currentList;
	}
	
	protected List<SerializationRule> addRulesForAttribute(Attribute a, List<SerializationRule> currentList) {
		return AttributeAnnotationToRuleOnAttribute.addRules(a, true, currentList);
	}
	
	protected final String taskDescription = "Deserialization using " + AbstractDeserializer.this.getClass().getName();
	
	/** Shortcut to easily create a Task. */
	protected class DeserializationTask extends Task.Cpu<Void, NoException> {
		public DeserializationTask(Runnable run) {
			super(taskDescription, priority);
			this.run = run;
		}
		
		private Runnable run;
		
		@Override
		public Void run() throws CancelException {
			try { run.run(); }
			catch (Exception t) {
				throw new CancelException("Error thrown in deserialization", t);
			}
			return null;
		}
	}
	
	/** Deserialize a value. */
	@SuppressWarnings("unchecked")
	public <T> AsyncSupplier<T, SerializationException> deserializeValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		rules = addRulesForType(new SerializationClass(type), rules);
		TypeDefinition newType = type;
		List<TypeDefinition> rulesTypes = new ArrayList<>(rules.size());
		for (SerializationRule rule : rules) {
			rulesTypes.add(newType);
			newType = rule.getDeserializationType(newType, context);
		}
		
		AsyncSupplier<?, SerializationException> value = deserializeValueType(context, newType, path, rules);
		AsyncSupplier<T, SerializationException> result = new AsyncSupplier<>();
		List<SerializationRule> rul = rules;
		value.thenDoOrStart(val -> {
			Object o = val;
			ListIterator<TypeDefinition> itType = rulesTypes.listIterator(rul.size());
			ListIterator<SerializationRule> itRule = rul.listIterator(rul.size());
			while (itRule.hasPrevious())
				try { o = itRule.previous().getDeserializationValue(o, itType.previous(), context); }
				catch (SerializationException e) {
					result.error(e);
					return;
				}
			result.unblockSuccess((T)o);
		}, taskDescription, priority, result);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "squid:S3776" })
	private <T> AsyncSupplier<T, SerializationException> deserializeValueType(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		Class<?> c = type.getBase();
		
		if (c.isArray()) {
			if (byte[].class.equals(c))
				return (AsyncSupplier<T, SerializationException>)deserializeByteArrayValue(context, rules);
			return (AsyncSupplier<T, SerializationException>)deserializeCollectionValue(context, type, path, rules);
		}
		
		if (boolean.class.equals(c))
			return (AsyncSupplier<T, SerializationException>)deserializeBooleanValue(false);
		if (Boolean.class.equals(c))
			return (AsyncSupplier<T, SerializationException>)deserializeBooleanValue(true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return (AsyncSupplier<T, SerializationException>)deserializeNumericValue(c, false, null);
		if (Number.class.isAssignableFrom(c))
			return (AsyncSupplier<T, SerializationException>)deserializeNumericValue(c, true, null);
			
		if (char.class.equals(c))
			return (AsyncSupplier<T, SerializationException>)deserializeCharacterValue(false);
		if (Character.class.equals(c))
			return (AsyncSupplier<T, SerializationException>)deserializeCharacterValue(true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncSupplier<? extends CharSequence, SerializationException> str = deserializeStringValue();
			AsyncSupplier<T, SerializationException> result = new AsyncSupplier<>();
			str.onDone(
				string -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					if (c.isAssignableFrom(string.getClass())) {
						result.unblockSuccess((T)string);
						return;
					}
					for (Constructor<?> ctor : c.getConstructors()) {
						Class<?>[] params = ctor.getParameterTypes();
						if (params.length != 1) continue;
						if (params[0].isAssignableFrom(string.getClass())) {
							try { result.unblockSuccess((T)ctor.newInstance(string)); }
							catch (Exception t) {
								result.error(SerializationException.instantiation(c.getName(), t));
							}
							return;
						}
						if (params[0].isAssignableFrom(String.class)) {
							try { result.unblockSuccess((T)ctor.newInstance(string.toString())); }
							catch (Exception t) {
								result.error(SerializationException.instantiation(c.getName(), t));
							}
							return;
						}
					}
					result.error(new SerializationException("Type " + c.getName()
						+ " does not have a compatible constructor with parameter type " + string.getClass()
						+ " or String"));
				},
				result
			);
			return result;
		}

		if (c.isEnum()) {
			AsyncSupplier<? extends CharSequence, SerializationException> str = deserializeStringValue();
			AsyncSupplier<T, SerializationException> result = new AsyncSupplier<>();
			str.onDone(
				string -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					try {
						result.unblockSuccess((T)Enum.valueOf((Class<? extends Enum>)c, string.toString()));
					} catch (IllegalArgumentException e) {
						result.error(new SerializationException("Unknown enum value '" + string + "' for " + c.getName()));
					}
				},
				result
			);
			return result;
		}

		if (Collection.class.isAssignableFrom(c))
			return (AsyncSupplier<T, SerializationException>)deserializeCollectionValue(context, type, path, rules);

		if (Map.class.isAssignableFrom(c))
			return (AsyncSupplier<T, SerializationException>)deserializeMapValue(context, type, path, rules);
		
		if (InputStream.class.isAssignableFrom(c))
			return (AsyncSupplier<T, SerializationException>)deserializeInputStreamValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return (AsyncSupplier<T, SerializationException>)deserializeIOReadableValue(context, rules);

		return deserializeObjectValue(context, type, path, rules);
	}
	
	// *** boolean ***
	
	protected abstract AsyncSupplier<Boolean, SerializationException> deserializeBooleanValue(boolean nullable);

	@SuppressWarnings("unused")
	protected AsyncSupplier<Boolean, SerializationException> deserializeBooleanAttributeValue(AttributeContext context, boolean nullable) {
		return deserializeBooleanValue(nullable);
	}
	
	// *** numeric ***

	protected abstract AsyncSupplier<? extends Number, SerializationException> deserializeNumericValue(
		Class<?> type, boolean nullable, Class<? extends IntegerUnit> targetUnit);

	protected AsyncSupplier<? extends Number, SerializationException> deserializeNumericAttributeValue(
		AttributeContext context, boolean nullable
	) {
		Unit unit = context.getAttribute().getAnnotation(false, Unit.class);
		Class<? extends IntegerUnit> target = unit != null ? unit.value() : null;
		return deserializeNumericValue(context.getAttribute().getType().getBase(), nullable, target);
	}
	
	/** Convert a BigDecimal into the specified type. */
	public static void convertBigDecimalValue(BigDecimal n, Class<?> type, AsyncSupplier<Number, SerializationException> result) {
		try {
			if (byte.class.equals(type) || Byte.class.equals(type))
				result.unblockSuccess(Byte.valueOf(n.byteValueExact()));
			else if (short.class.equals(type) || Short.class.equals(type))
				result.unblockSuccess(Short.valueOf(n.shortValueExact()));
			else if (int.class.equals(type) || Integer.class.equals(type))
				result.unblockSuccess(Integer.valueOf(n.intValueExact()));
			else if (long.class.equals(type) || Long.class.equals(type))
				result.unblockSuccess(Long.valueOf(n.longValueExact()));
			else if (float.class.equals(type) || Float.class.equals(type))
				result.unblockSuccess(Float.valueOf(n.floatValue()));
			else if (double.class.equals(type) || Double.class.equals(type))
				result.unblockSuccess(Double.valueOf(n.doubleValue()));
			else if (BigInteger.class.equals(type))
				result.unblockSuccess(n.toBigIntegerExact());
			else if (BigDecimal.class.equals(type))
				result.unblockSuccess(n);
			else
				throw new SerializationException("Unknown numeric value type " + type.getName());
		} catch (SerializationException e) {
			result.error(e);
		} catch (Exception e) {
			result.error(new SerializationException("Invalid numeric value", e));
		}
	}
	
	/** Convert a string into an integer, optionally doing convertion into the targetUnit if any. */
	public static Number convertStringToInteger(Class<?> type, String str, Class<? extends IntegerUnit> targetUnit)
	throws UnitConversionException, ParseException {
		int i = 0;
		int l = str.length();
		while (i < l) {
			char c = str.charAt(i);
			if (c >= '0' && c <= '9') {
				i++;
				continue;
			}
			break;
		}
		BigInteger value;
		if (i == l) value = new BigInteger(str);
		else {
			String unitStr = str.substring(i).trim();
			value = new BigInteger(str.substring(0, i));
			if (unitStr.length() > 0) {
				Class<? extends IntegerUnit> unit = IntegerUnit.ParserRegistry.get(unitStr);
				if (unit == null)
					throw new ParseException("Unknown integer unit: " + unitStr, i);
				long val = IntegerUnit.ConverterRegistry.convert(value.longValue(), unit, targetUnit);
				value = BigInteger.valueOf(val);
			}
		}
		if (byte.class.equals(type) || Byte.class.equals(type))
			return Byte.valueOf(value.byteValueExact());
		if (short.class.equals(type) || Short.class.equals(type))
			return Short.valueOf(value.shortValueExact());
		if (int.class.equals(type) || Integer.class.equals(type))
			return Integer.valueOf(value.intValueExact());
		if (long.class.equals(type) || Long.class.equals(type))
			return Long.valueOf(value.longValueExact());
		if (BigInteger.class.equals(type))
			return value;
		throw new ParseException("Unknown integer type " + type.getName(), 0);
	}
	
	// *** character ***
	
	/** By default, deserialize a string and get the first character. */
	protected AsyncSupplier<Character, SerializationException> deserializeCharacterValue(boolean nullable) {
		AsyncSupplier<? extends CharSequence, SerializationException> read = deserializeStringValue();
		return deserializeCharacter(read, nullable);
	}
	
	/** By default, deserialize a string and get the first character. */
	protected AsyncSupplier<Character, SerializationException> deserializeCharacterAttributeValue(AttributeContext context, boolean nullable) {
		Attribute fakeAttr = new Attribute(context.getAttribute());
		fakeAttr.setType(new TypeDefinition(String.class));
		AttributeContext fakeContext = new AttributeContext(context.getParent(), fakeAttr);
		AsyncSupplier<? extends CharSequence, SerializationException> read = deserializeStringAttributeValue(fakeContext);
		return deserializeCharacter(read, nullable);
	}
	
	private static AsyncSupplier<Character, SerializationException> deserializeCharacter(
		AsyncSupplier<? extends CharSequence, SerializationException> read, boolean nullable
	) {
		AsyncSupplier<Character, SerializationException> result = new AsyncSupplier<>();
		read.onDone(string -> {
			if (string == null || string.length() == 0) {
				if (nullable)
					result.unblockSuccess(null);
				else
					result.error(new SerializationException("Character value expected"));
				return;
			}
			if (string.length() > 1) {
				result.error(new SerializationException(
					"A single character value is expected, " + string.length() + " characters found"));
				return;
			}
			result.unblockSuccess(Character.valueOf(string.charAt(0)));
		}, result);
		return result;
	}
	
	// *** string ***
	
	protected abstract AsyncSupplier<? extends CharSequence, SerializationException> deserializeStringValue();
	
	protected AsyncSupplier<? extends CharSequence, SerializationException> deserializeStringAttributeValue(
		@SuppressWarnings({"unused","squid:S1172"}) AttributeContext context
	) {
		return deserializeStringValue();
	}
	
	// *** Collection ***
	
	@SuppressWarnings("rawtypes")
	protected AsyncSupplier<Object, SerializationException> deserializeCollectionValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		AsyncSupplier<Boolean, SerializationException> start = startCollectionValue();
		AsyncSupplier<Object, SerializationException> result = new AsyncSupplier<>();
		Collection<?> col;
		TypeDefinition elementType;
		if (type.getBase().isArray()) {
			col = new LinkedList();
			elementType = new TypeDefinition(type.getBase().getComponentType());
		} else {
			try { col = (Collection<?>)SerializationClass.instantiate(type, context, rules, false); }
			catch (SerializationException e) {
				return new AsyncSupplier<>(null, e);
			} catch (Exception e) {
				return new AsyncSupplier<>(null, new SerializationException("Error instantiating collection", e));
			}
			if (type.getParameters().isEmpty())
				return new AsyncSupplier<>(null, new SerializationException(
					"Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
		}
		CollectionContext ctx = new CollectionContext(context, col, type, elementType);
		start.thenDoOrStart(res -> {
			if (!res.booleanValue())
				result.unblockSuccess(null);
			else
				deserializeNextCollectionValueElement(ctx, 0, path, rules, result); 
		}, taskDescription, priority, result);
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected abstract AsyncSupplier<Boolean, SerializationException> startCollectionValue();
	
	protected void deserializeNextCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules,
		AsyncSupplier<Object, SerializationException> result
	) {
		deserializeCollectionValueElement(context, elementIndex, colPath, rules, false, result);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void deserializeCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules,
		boolean isAttribute,
		AsyncSupplier<Object, SerializationException> result
	) {
		do {
			AsyncSupplier<Pair<Object, Boolean>, SerializationException> next = isAttribute
				? deserializeCollectionAttributeValueElement(context, elementIndex, colPath, rules)
				: deserializeCollectionValueElement(context, elementIndex, colPath, rules);
			if (!next.isDone()) {
				int currentIndex = elementIndex;
				next.onDone(p -> {
					if (!p.getValue2().booleanValue()) {
						// end of collection
						if (Collection.class.isAssignableFrom(context.getCollectionType().getBase()))
							result.unblockSuccess(context.getCollection());
						else
							result.unblockSuccess(toArray(context));
						return;
					}
					new DeserializationTask(() -> {
						Object element = p.getValue1();
						if (element != null &&
							Collection.class.isAssignableFrom(context.getCollectionType().getBase()) &&
							!context.getElementType().getBase().isAssignableFrom(element.getClass())) {
							result.error(new SerializationException("Invalid collection element type "
									+ element.getClass().getName()
									+ ", expected is " + context.getElementType().getBase().getName()));
								return;
						}
						((Collection)context.getCollection()).add(element);
						if (isAttribute)
							deserializeNextCollectionAttributeValueElement(
								context, currentIndex + 1, colPath, rules, result);
						else
							deserializeNextCollectionValueElement(context, currentIndex + 1, colPath, rules, result);
					}).start();
				}, result);
				return;
			}
			if (next.hasError()) {
				result.error(next.getError());
				return;
			}
			Pair<Object, Boolean> p = next.getResult();
			if (!p.getValue2().booleanValue()) {
				// end of collection
				if (Collection.class.isAssignableFrom(context.getCollectionType().getBase()))
					result.unblockSuccess(context.getCollection());
				else
					result.unblockSuccess(toArray(context));
				return;
			}
			Object element = p.getValue1();
			if (element != null &&
				Collection.class.isAssignableFrom(context.getCollectionType().getBase()) &&
				!context.getElementType().getBase().isAssignableFrom(element.getClass())) {
				result.error(new SerializationException("Invalid collection element type "
						+ element.getClass().getName()
						+ ", expected is " + context.getElementType().getBase().getName()));
					return;
			}
			((Collection)context.getCollection()).add(element);
			elementIndex++;
			
		} while (true);
	}
	
	/** Return the element (possibly null) with true if an element is found, or null with false if the end of the collection has been reached. */
	protected abstract AsyncSupplier<Pair<Object, Boolean>, SerializationException> deserializeCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules);
	
	@SuppressWarnings("rawtypes")
	protected AsyncSupplier<Object, SerializationException> deserializeCollectionAttributeValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		AsyncSupplier<Boolean, SerializationException> start = startCollectionAttributeValue(context);
		Collection<?> col;
		TypeDefinition elementType;
		TypeDefinition colType = context.getAttribute().getType();
		if (colType.getBase().isArray()) {
			col = new LinkedList();
			Class<?> t = colType.getBase().getComponentType();
			if (t.equals(boolean.class)) t = Boolean.class;
			else if (t.equals(byte.class)) t = Byte.class;
			else if (t.equals(short.class)) t = Short.class;
			else if (t.equals(int.class)) t = Integer.class;
			else if (t.equals(long.class)) t = Long.class;
			else if (t.equals(float.class)) t = Float.class;
			else if (t.equals(double.class)) t = Double.class;
			else if (t.equals(char.class)) t = Character.class;
			elementType = new TypeDefinition(t);
		} else {
			try { col = (Collection<?>)SerializationClass.instantiate(colType, context, rules, false); }
			catch (SerializationException e) {
				return new AsyncSupplier<>(null, e);
			} catch (Exception e) {
				return new AsyncSupplier<>(null, new SerializationException("Error instantiating collection", e));
			}
			if (colType.getParameters().isEmpty())
				return new AsyncSupplier<>(null, new SerializationException(
					"Cannot deserialize collection without an element type specified"));
			elementType = colType.getParameters().get(0);
		}
		CollectionContext ctx = new CollectionContext(context, col, colType, elementType);
		AsyncSupplier<Object, SerializationException> result = new AsyncSupplier<>();
		start.thenDoOrStart(r -> {
			if (!r.booleanValue())
				result.unblockSuccess(null);
			else
				deserializeNextCollectionAttributeValueElement(ctx, 0, path, rules, result);
		}, taskDescription, priority, result);
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected AsyncSupplier<Boolean, SerializationException> startCollectionAttributeValue(
		@SuppressWarnings({"unused","squid:S1172"}) AttributeContext context
	) {
		return startCollectionValue();
	}

	protected void deserializeNextCollectionAttributeValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules,
		AsyncSupplier<Object, SerializationException> result
	) {
		deserializeCollectionValueElement(context, elementIndex, colPath, rules, true, result);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static Object toArray(CollectionContext context) {
		Class<?> t = context.getCollectionType().getBase().getComponentType();
		Collection col = (Collection)context.getCollection();
		if (t.equals(boolean.class)) {
			boolean[] a = new boolean[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Boolean)o).booleanValue();
			return a;
		}
		if (t.equals(byte.class)) {
			byte[] a = new byte[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Byte)o).byteValue();
			return a;
		}
		if (t.equals(short.class)) {
			short[] a = new short[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Short)o).shortValue();
			return a;
		}
		if (t.equals(int.class)) {
			int[] a = new int[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Integer)o).intValue();
			return a;
		}
		if (t.equals(long.class)) {
			long[] a = new long[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Long)o).longValue();
			return a;
		}
		if (t.equals(float.class)) {
			float[] a = new float[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Float)o).floatValue();
			return a;
		}
		if (t.equals(double.class)) {
			double[] a = new double[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Double)o).doubleValue();
			return a;
		}
		if (t.equals(char.class)) {
			char[] a = new char[col.size()];
			int i = 0;
			for (Object o : col) a[i++] = ((Character)o).charValue();
			return a;
		}
		return col.toArray((Object[])Array.newInstance(t, col.size()));
	}
	
	/** Return the element with true if an element is found, or null with false if the end of the collection has been reached. */
	protected AsyncSupplier<Pair<Object, Boolean>, SerializationException> deserializeCollectionAttributeValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules
	) {
		return deserializeCollectionValueElement(context, elementIndex, colPath, rules);
	}
	
	// *** Map ***
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AsyncSupplier<Map<?,?>, SerializationException> deserializeMapValue(
		SerializationContext context, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, typeDef.getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		AsyncSupplier<Object, SerializationException> value = deserializeValueType(context, type, path, rules);
		AsyncSupplier<Map<?,?>, SerializationException> result = new AsyncSupplier<>();
		value.thenDoOrStart(val -> {
			try {
				result.unblockSuccess(getMap((ArrayList<MapEntry>)val, typeDef, context, rules));
			} catch (SerializationException e) {
				result.error(e);
			}
		}, taskDescription, priority, result);
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AsyncSupplier<Map<?,?>, SerializationException> deserializeMapAttributeValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, context.getAttribute().getType().getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		Attribute fakeAttribute = new Attribute(context.getAttribute());
		fakeAttribute.setType(type);
		AttributeContext ctx = new AttributeContext(context.getParent(), fakeAttribute);
		AsyncSupplier<Object, SerializationException> value = deserializeCollectionAttributeValue(ctx, path, rules);
		AsyncSupplier<Map<?,?>, SerializationException> result = new AsyncSupplier<>();
		value.thenDoOrStart(val -> {
			try {
				result.unblockSuccess(getMap((ArrayList<MapEntry>)val, context.getAttribute().getType(), context, rules));
			} catch (SerializationException e) {
				result.error(e);
			}
		}, taskDescription, priority, result);
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static Map<?,?> getMap(
		ArrayList<MapEntry> entries, TypeDefinition type, SerializationContext context, List<SerializationRule> rules
	) throws SerializationException {
		try {
			Map map = (Map)SerializationClass.instantiate(type, context, rules, false);
			for (MapEntry entry : entries)
				map.put(entry.key, entry.value);
			return map;
		} catch (SerializationException e) {
			throw e;
		} catch (Exception e) {
			throw SerializationException.instantiation(type.toString(), e);
		}
	}
	
	// *** InputStream ***

	protected AsyncSupplier<InputStream, SerializationException> deserializeInputStreamValue(
		SerializationContext context, List<SerializationRule> rules
	) {
		AsyncSupplier<IO.Readable, SerializationException> io = deserializeIOReadableValue(context, rules);
		AsyncSupplier<InputStream, SerializationException> result = new AsyncSupplier<>();
		io.thenDoOrStart(ior -> result.unblockSuccess(ior != null ? IOAsInputStream.get(ior, false) : null),
			taskDescription, priority, result);
		return result;
	}
	
	protected AsyncSupplier<InputStream, SerializationException> deserializeInputStreamAttributeValue(
		AttributeContext context, List<SerializationRule> rules
	) {
		AsyncSupplier<IO.Readable, SerializationException> io = deserializeIOReadableAttributeValue(context, rules);
		AsyncSupplier<InputStream, SerializationException> result = new AsyncSupplier<>();
		io.thenDoOrStart(ior -> result.unblockSuccess(ior != null ? IOAsInputStream.get(ior, false) : null),
			taskDescription, priority, result);
		return result;
	}
	
	// *** IO.Readable ***
	
	protected List<StreamReferenceHandler> streamReferenceHandlers = new LinkedList<>();

	@Override
	public void addStreamReferenceHandler(StreamReferenceHandler handler) {
		streamReferenceHandlers.add(handler);
	}
	
	protected abstract AsyncSupplier<IO.Readable, SerializationException> deserializeIOReadableValue(
		SerializationContext context, List<SerializationRule> rules);
	
	protected abstract AsyncSupplier<IO.Readable, SerializationException> deserializeIOReadableAttributeValue(
		AttributeContext context, List<SerializationRule> rules);
	
	// *** byte[] by default using IO ***
	
	protected AsyncSupplier<byte[], SerializationException> deserializeByteArrayValue(
		SerializationContext context, List<SerializationRule> rules
	) {
		AsyncSupplier<IO.Readable, SerializationException> io = deserializeIOReadableValue(context, rules);
		return deserializeByteArray(io);
	}
	
	protected AsyncSupplier<byte[], SerializationException> deserializeByteArrayAttributeValue(
		AttributeContext context, List<SerializationRule> rules
	) {
		AsyncSupplier<IO.Readable, SerializationException> io = deserializeIOReadableAttributeValue(context, rules);
		return deserializeByteArray(io);
	}
	
	private AsyncSupplier<byte[], SerializationException> deserializeByteArray(AsyncSupplier<IO.Readable, SerializationException> io) {
		AsyncSupplier<byte[], IOException> res = new AsyncSupplier<>();
		AsyncSupplier<byte[], SerializationException> result = new AsyncSupplier<>();
		res.forward(result, ioe -> new SerializationException("Error deserializing byte array", ioe));
		io.thenDoOrStart(ior -> {
			if (ior == null)
				result.unblockSuccess(null);
			else
				IOUtil.readFully(ior, res);
		}, taskDescription, priority, result);
		return result;
	}
	
	// *** Object ***

	@SuppressWarnings("unchecked")
	protected <T> AsyncSupplier<T, SerializationException> deserializeObjectValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		AsyncSupplier<Object, SerializationException> start = startObjectValue(context, type, rules);
		AsyncSupplier<Object, SerializationException> result = new AsyncSupplier<>();
		start.thenDoOrStart(instance -> {
			if (instance == null)
				result.unblockSuccess(null);
			else
				deserializeObjectAttributes(context, instance, type, path, rules, result);
		}, taskDescription, priority, result);
		return (AsyncSupplier<T, SerializationException>)result;
	}

	/** Instantiate the type if the start of an object has been found, null if null has been found, or an error.
	 * The context should be used to instantiate the object.
	 * The deserializer should handle the case a specific type to instantiate is specified,
	 * and may need to read a first attribute to get this type.
	 */
	protected abstract AsyncSupplier<Object, SerializationException> startObjectValue(
		SerializationContext context, TypeDefinition type, List<SerializationRule> rules);
	
	protected void deserializeObjectAttributes(
		SerializationContext parentContext, Object instance, TypeDefinition typeDef,
		String path, List<SerializationRule> rules, AsyncSupplier<Object, SerializationException> result
	) {
		ObjectContext ctx;
		try { 
			SerializationClass sc = new SerializationClass(
				typeDef != null
				? TypeDefinition.from(instance.getClass(), typeDef)
				: new TypeDefinition(instance.getClass()));
			ctx = new ObjectContext(parentContext, instance, sc, typeDef);
			rules = addRulesForType(sc, rules);
			sc.apply(rules, ctx, false);
		} catch (SerializationException e) {
			result.error(e);
			return;
		}
		deserializeNextObjectAttribute(ctx, path, rules, result);
	}
	
	private static Attribute checkNextAttributeName(
		String n, ObjectContext context, String path, AsyncSupplier<Object, SerializationException> result
	) {
		if (n == null) {
			result.unblockSuccess(context.getInstance());
			return null;
		}
		Attribute a = context.getSerializationClass().getAttributeByName(n);
		if (a == null) {
			result.error(new SerializationException("Unknown attribute " + n + " for type "
				+ context.getInstance().getClass().getName() + " in " + path));
			return null;
		}
		if (!a.canSet()) {
			result.error(new SerializationException("Attribute " + n + " cannot be set on type "
				+ context.getInstance().getClass().getName()));
			return null;
		}
		return a;
	}
	
	protected void deserializeNextObjectAttribute(
		ObjectContext context, String path, List<SerializationRule> rules, AsyncSupplier<Object, SerializationException> result
	) {
		do {
			AsyncSupplier<String, SerializationException> name = deserializeObjectAttributeName(context);
			if (name.isDone()) {
				if (name.hasError()) {
					result.error(name.getError());
					return;
				}
				
				String n = name.getResult();
				Attribute a = checkNextAttributeName(n, context, path, result);
				if (a == null) return;
				IAsync<SerializationException> val =
					deserializeObjectAttributeValue(context, a, path + '.' + n, rules);
				if (val.isDone()) {
					if (val.forwardIfNotSuccessful(result))
						return;
					continue;
				}
				val.thenStart(new DeserializationTask(() -> deserializeNextObjectAttribute(context, path, rules, result)), result);
				return;
			}
			name.onDone(
				n -> {
					if (n == null) {
						result.unblockSuccess(context.getInstance());
						return;
					}
					new DeserializationTask(() -> {
						Attribute a = checkNextAttributeName(n, context, path, result);
						if (a == null) return;
						IAsync<SerializationException> val =
							deserializeObjectAttributeValue(context, a, path + '.' + n, rules);
						if (val.isDone()) {
							if (val.forwardIfNotSuccessful(result))
								return;
							deserializeNextObjectAttribute(context, path, rules, result);
							return;
						}
						val.thenStart(new DeserializationTask(() ->
							deserializeNextObjectAttribute(context, path, rules, result)
						), result);
					}).start();
				}, result
			);
			return;
		} while (true);
	}
	
	/** Return the name of the attribute read, or null if the object is closed. */
	protected abstract AsyncSupplier<String, SerializationException> deserializeObjectAttributeName(ObjectContext context);
	
	protected IAsync<SerializationException> deserializeObjectAttributeValue(
		ObjectContext context, Attribute a, String path, List<SerializationRule> rules
	) {
		AttributeContext ctx = new AttributeContext(context, a);
		AsyncSupplier<?, SerializationException> value = deserializeObjectAttributeValue(ctx, path, rules);
		Async<SerializationException> sp = new Async<>();
		value.thenDoOrStart(val -> {
			if (!a.ignore())
				try { a.setValue(context.getInstance(), val); }
				catch (SerializationException e) {
					sp.error(e);
					return;
				}
			sp.unblock();
		}, taskDescription, priority, sp);
		return sp;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AsyncSupplier<?, SerializationException> deserializeObjectAttributeValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		Attribute a = context.getAttribute();
		TypeDefinition type = a.getType();
		Class<?> c = type.getBase();
		rules = addRulesForAttribute(a, rules);
		
		if (c.isArray()) {
			if (byte[].class.equals(c))
				return deserializeByteArrayAttributeValue(context, rules);
			return deserializeCollectionAttributeValue(context, path, rules);
		}
		
		if (boolean.class.equals(c))
			return deserializeBooleanAttributeValue(context, false);
		if (Boolean.class.equals(c))
			return deserializeBooleanAttributeValue(context, true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return deserializeNumericAttributeValue(context, false);
		if (Number.class.isAssignableFrom(c))
			return deserializeNumericAttributeValue(context, true);
			
		if (char.class.equals(c))
			return deserializeCharacterAttributeValue(context, false);
		if (Character.class.equals(c))
			return deserializeCharacterAttributeValue(context, true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncSupplier<? extends CharSequence, SerializationException> str = deserializeStringAttributeValue(context);
			return convertFromStringToCharSequence(str, c);
		}

		if (c.isEnum()) {
			AsyncSupplier<? extends CharSequence, SerializationException> str = deserializeStringAttributeValue(context);
			AsyncSupplier<Object, SerializationException> result = new AsyncSupplier<>();
			str.onDone(
				string -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					try {
						result.unblockSuccess(Enum.valueOf((Class<? extends Enum>)c, string.toString()));
					} catch (IllegalArgumentException e) {
						result.error(new SerializationException("Unknown enum value '" + string + "' for " + c.getName()));
					}
				},
				result
			);
			return result;
		}

		if (Collection.class.isAssignableFrom(c))
			return deserializeCollectionAttributeValue(context, path, rules);

		if (Map.class.isAssignableFrom(c))
			return deserializeMapAttributeValue(context, path, rules);
		
		if (InputStream.class.isAssignableFrom(c))
			return deserializeInputStreamAttributeValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return deserializeIOReadableAttributeValue(context, rules);

		return deserializeObjectAttributeObjectValue(context, path, rules);
	}
	
	protected AsyncSupplier<Object, SerializationException> convertFromStringToCharSequence(
		AsyncSupplier<? extends CharSequence, SerializationException> str, Class<?> target
	) {
		AsyncSupplier<Object, SerializationException> result = new AsyncSupplier<>();
		str.onDone(
			string -> {
				if (string == null) {
					result.unblockSuccess(null);
					return;
				}
				if (target.isAssignableFrom(string.getClass())) {
					result.unblockSuccess(string);
					return;
				}
				for (Constructor<?> ctor : target.getConstructors()) {
					Class<?>[] params = ctor.getParameterTypes();
					if (params.length != 1) continue;
					if (params[0].isAssignableFrom(string.getClass())) {
						try { result.unblockSuccess(ctor.newInstance(string)); }
						catch (Exception t) {
							result.error(SerializationException.instantiation(target.getName(), t));
						}
						return;
					}
					if (params[0].isAssignableFrom(String.class)) {
						try { result.unblockSuccess(ctor.newInstance(string.toString())); }
						catch (Exception t) {
							result.error(SerializationException.instantiation(target.getName(), t));
						}
						return;
					}
				}
				result.error(new SerializationException("Type " + target.getName()
					+ " does not have a compatible constructor with parameter type " + string.getClass()
					+ " or String"));
			},
			result
		);
		return result;
	}

	protected AsyncSupplier<Object, SerializationException> deserializeObjectAttributeObjectValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		return deserializeObjectValue(context, context.getAttribute().getType(), path, rules);
	}
	
}

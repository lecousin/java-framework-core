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

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
	
	protected abstract ISynchronizationPoint<SerializationException> initializeDeserialization(IO.Readable input);
	
	protected abstract ISynchronizationPoint<SerializationException> finalizeDeserialization();
	
	@Override
	public int getMaximumTextSize() {
		return maxTextSize;
	}
	
	@Override
	public void setMaximumTextSize(int max) {
		maxTextSize = max;
	}
	
	@Override
	public <T> AsyncWork<T, SerializationException> deserialize(TypeDefinition type, IO.Readable input, List<SerializationRule> rules) {
		priority = input.getPriority();
		ISynchronizationPoint<SerializationException> init = initializeDeserialization(input);
		AsyncWork<T, SerializationException> result = new AsyncWork<>();
		init.listenAsync(new DeserializationTask(() -> {
			AsyncWork<T, SerializationException> sp = deserializeValue(null, type, "", rules);
			sp.listenInline(obj -> finalizeDeserialization().listenInline(() -> result.unblockSuccess(obj), result), result);
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
	
	/** Shortcut to easily create a Task. */
	protected class DeserializationTask extends Task.Cpu<Void, NoException> {
		public DeserializationTask(Runnable run) {
			super("Deserialization using " + AbstractDeserializer.this.getClass().getName(), priority);
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
	public <T> AsyncWork<T, SerializationException> deserializeValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		rules = addRulesForType(new SerializationClass(type), rules);
		TypeDefinition newType = type;
		List<TypeDefinition> rulesTypes = new ArrayList<>(rules.size());
		for (SerializationRule rule : rules) {
			rulesTypes.add(newType);
			newType = rule.getDeserializationType(newType, context);
		}
		
		AsyncWork<?, SerializationException> value = deserializeValueType(context, newType, path, rules);
		AsyncWork<T, SerializationException> result = new AsyncWork<>();
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else {
				Object o = value.getResult();
				ListIterator<TypeDefinition> itType = rulesTypes.listIterator(rules.size());
				ListIterator<SerializationRule> itRule = rules.listIterator(rules.size());
				while (itRule.hasPrevious())
					try { o = itRule.previous().getDeserializationValue(o, itType.previous(), context); }
					catch (SerializationException e) {
						result.error(e);
						return result;
					}
				result.unblockSuccess((T)o);
			}
			return result;
		}
		List<SerializationRule> rul = rules;
		value.listenAsync(new DeserializationTask(() -> {
			Object o = value.getResult();
			ListIterator<TypeDefinition> itType = rulesTypes.listIterator(rul.size());
			ListIterator<SerializationRule> itRule = rul.listIterator(rul.size());
			while (itRule.hasPrevious())
				try { o = itRule.previous().getDeserializationValue(o, itType.previous(), context); }
				catch (SerializationException e) {
					result.error(e);
					return;
				}
			result.unblockSuccess((T)o);
		}), result);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "squid:S3776" })
	private <T> AsyncWork<T, SerializationException> deserializeValueType(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		Class<?> c = type.getBase();
		
		if (c.isArray()) {
			if (byte[].class.equals(c))
				return (AsyncWork<T, SerializationException>)deserializeByteArrayValue(context, rules);
			return (AsyncWork<T, SerializationException>)deserializeCollectionValue(context, type, path, rules);
		}
		
		if (boolean.class.equals(c))
			return (AsyncWork<T, SerializationException>)deserializeBooleanValue(false);
		if (Boolean.class.equals(c))
			return (AsyncWork<T, SerializationException>)deserializeBooleanValue(true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return (AsyncWork<T, SerializationException>)deserializeNumericValue(c, false, null);
		if (Number.class.isAssignableFrom(c))
			return (AsyncWork<T, SerializationException>)deserializeNumericValue(c, true, null);
			
		if (char.class.equals(c))
			return (AsyncWork<T, SerializationException>)deserializeCharacterValue(false);
		if (Character.class.equals(c))
			return (AsyncWork<T, SerializationException>)deserializeCharacterValue(true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncWork<? extends CharSequence, SerializationException> str = deserializeStringValue();
			AsyncWork<T, SerializationException> result = new AsyncWork<>();
			str.listenInline(
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
								result.error(
									new SerializationException("Error instantiating type " + c.getName(), t));
							}
							return;
						}
						if (params[0].isAssignableFrom(String.class)) {
							try { result.unblockSuccess((T)ctor.newInstance(string.toString())); }
							catch (Exception t) {
								result.error(
									new SerializationException("Error instantiating type " + c.getName(), t));
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
			AsyncWork<? extends CharSequence, SerializationException> str = deserializeStringValue();
			AsyncWork<T, SerializationException> result = new AsyncWork<>();
			str.listenInline(
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
			return (AsyncWork<T, SerializationException>)deserializeCollectionValue(context, type, path, rules);

		if (Map.class.isAssignableFrom(c))
			return (AsyncWork<T, SerializationException>)deserializeMapValue(context, type, path, rules);
		
		if (InputStream.class.isAssignableFrom(c))
			return (AsyncWork<T, SerializationException>)deserializeInputStreamValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return (AsyncWork<T, SerializationException>)deserializeIOReadableValue(context, rules);

		return deserializeObjectValue(context, type, path, rules);
	}
	
	// *** boolean ***
	
	protected abstract AsyncWork<Boolean, SerializationException> deserializeBooleanValue(boolean nullable);

	@SuppressWarnings("unused")
	protected AsyncWork<Boolean, SerializationException> deserializeBooleanAttributeValue(AttributeContext context, boolean nullable) {
		return deserializeBooleanValue(nullable);
	}
	
	// *** numeric ***

	protected abstract AsyncWork<? extends Number, SerializationException> deserializeNumericValue(
		Class<?> type, boolean nullable, Class<? extends IntegerUnit> targetUnit);

	protected AsyncWork<? extends Number, SerializationException> deserializeNumericAttributeValue(AttributeContext context, boolean nullable) {
		Unit unit = context.getAttribute().getAnnotation(false, Unit.class);
		Class<? extends IntegerUnit> target = unit != null ? unit.value() : null;
		return deserializeNumericValue(context.getAttribute().getType().getBase(), nullable, target);
	}
	
	/** Convert a BigDecimal into the specified type. */
	public static void convertBigDecimalValue(BigDecimal n, Class<?> type, AsyncWork<Number, SerializationException> result) {
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
	protected AsyncWork<Character, SerializationException> deserializeCharacterValue(boolean nullable) {
		AsyncWork<? extends CharSequence, SerializationException> read = deserializeStringValue();
		AsyncWork<Character, SerializationException> result = new AsyncWork<>();
		read.listenInline(string -> {
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
	
	/** By default, deserialize a string and get the first character. */
	protected AsyncWork<Character, SerializationException> deserializeCharacterAttributeValue(AttributeContext context, boolean nullable) {
		Attribute fakeAttr = new Attribute(context.getAttribute());
		fakeAttr.setType(new TypeDefinition(String.class));
		AttributeContext fakeContext = new AttributeContext(context.getParent(), fakeAttr);
		AsyncWork<? extends CharSequence, SerializationException> read = deserializeStringAttributeValue(fakeContext);
		AsyncWork<Character, SerializationException> result = new AsyncWork<>();
		read.listenInline(string -> {
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
	
	protected abstract AsyncWork<? extends CharSequence, SerializationException> deserializeStringValue();
	
	protected AsyncWork<? extends CharSequence, SerializationException> deserializeStringAttributeValue(
		@SuppressWarnings({"unused","squid:S1172"}) AttributeContext context
	) {
		return deserializeStringValue();
	}
	
	// *** Collection ***
	
	@SuppressWarnings("rawtypes")
	protected AsyncWork<Object, SerializationException> deserializeCollectionValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		AsyncWork<Boolean, SerializationException> start = startCollectionValue();
		AsyncWork<Object, SerializationException> result = new AsyncWork<>();
		Collection<?> col;
		TypeDefinition elementType;
		if (type.getBase().isArray()) {
			col = new LinkedList();
			elementType = new TypeDefinition(type.getBase().getComponentType());
		} else {
			try { col = (Collection<?>)SerializationClass.instantiate(type, context, rules, false); }
			catch (SerializationException e) {
				return new AsyncWork<>(null, e);
			} catch (Exception e) {
				return new AsyncWork<>(null, new SerializationException("Error instantiating collection", e));
			}
			if (type.getParameters().isEmpty())
				return new AsyncWork<>(null, new SerializationException(
					"Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
		}
		CollectionContext ctx = new CollectionContext(context, col, type, elementType);
		if (start.isUnblocked()) {
			if (start.hasError()) return new AsyncWork<>(null, start.getError());
			deserializeNextCollectionValueElement(ctx, 0, path, rules, result);
		} else {
			start.listenAsync(new DeserializationTask(() -> {
				try {
					deserializeNextCollectionValueElement(ctx, 0, path, rules, result);
				} catch (Exception e) {
					result.error(new SerializationException("Error deserializing collection", e));
				}
			}), result);
		}
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected abstract AsyncWork<Boolean, SerializationException> startCollectionValue();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void deserializeNextCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules,
		AsyncWork<Object, SerializationException> result
	) {
		do {
			AsyncWork<Pair<Object, Boolean>, SerializationException> next =
				deserializeCollectionValueElement(context, elementIndex, colPath, rules);
			if (next.isUnblocked()) {
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
				if (element != null) {
					if (Collection.class.isAssignableFrom(context.getCollectionType().getBase()) &&
						!context.getElementType().getBase().isAssignableFrom(element.getClass())) {
						result.error(new SerializationException("Invalid collection element type "
								+ element.getClass().getName()
								+ ", expected is " + context.getElementType().getBase().getName()));
							return;
					}
				}
				((Collection)context.getCollection()).add(element);
				elementIndex++;
				continue;
			}
			int currentIndex = elementIndex;
			next.listenInline(() -> {
				Pair<Object, Boolean> p = next.getResult();
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
					if (element != null) {
						if (Collection.class.isAssignableFrom(context.getCollectionType().getBase()) &&
							!context.getElementType().getBase().isAssignableFrom(element.getClass())) {
							result.error(new SerializationException("Invalid collection element type "
									+ element.getClass().getName()
									+ ", expected is " + context.getElementType().getBase().getName()));
								return;
						}
					}
					((Collection)context.getCollection()).add(element);
					deserializeNextCollectionValueElement(context, currentIndex + 1, colPath, rules, result);
				}).start();
			}, result);
			return;
		} while (true);
	}
	
	/** Return the element (possibly null) with true if an element is found, or null with false if the end of the collection has been reached. */
	protected abstract AsyncWork<Pair<Object, Boolean>, SerializationException> deserializeCollectionValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules);
	
	@SuppressWarnings("rawtypes")
	protected AsyncWork<Object, SerializationException> deserializeCollectionAttributeValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		AsyncWork<Boolean, SerializationException> start = startCollectionAttributeValue(context);
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
				return new AsyncWork<>(null, e);
			} catch (Exception e) {
				return new AsyncWork<>(null, new SerializationException("Error instantiating collection", e));
			}
			if (colType.getParameters().isEmpty())
				return new AsyncWork<>(null, new SerializationException(
					"Cannot deserialize collection without an element type specified"));
			elementType = colType.getParameters().get(0);
		}
		CollectionContext ctx = new CollectionContext(context, col, colType, elementType);
		AsyncWork<Object, SerializationException> result = new AsyncWork<>();
		if (start.isUnblocked()) {
			if (start.hasError()) return new AsyncWork<>(null, start.getError());
			deserializeNextCollectionAttributeValueElement(ctx, 0, path, rules, result);
		} else {
			start.listenAsync(new DeserializationTask(() -> {
				try {
					deserializeNextCollectionAttributeValueElement(ctx, 0, path, rules, result);
				} catch (Exception e) {
					result.error(new SerializationException("Error deserializing collection", e));
				}
			}), result);
		}
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected AsyncWork<Boolean, SerializationException> startCollectionAttributeValue(@SuppressWarnings("unused") AttributeContext context) {
		return startCollectionValue();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void deserializeNextCollectionAttributeValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules,
		AsyncWork<Object, SerializationException> result
	) {
		do {
			AsyncWork<Pair<Object, Boolean>, SerializationException> next =
				deserializeCollectionAttributeValueElement(context, elementIndex, colPath, rules);
			if (next.isUnblocked()) {
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
				continue;
			}
			int currentIndex = elementIndex;
			next.listenInline(() -> {
				Pair<Object, Boolean> p = next.getResult();
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
					if (element != null) {
						if (Collection.class.isAssignableFrom(context.getCollectionType().getBase()) &&
							!context.getElementType().getBase().isAssignableFrom(element.getClass())) {
							result.error(new SerializationException("Invalid collection element type "
									+ element.getClass().getName()
									+ ", expected is " + context.getElementType().getBase().getName()));
								return;
						}
					}
					((Collection)context.getCollection()).add(element);
					deserializeNextCollectionAttributeValueElement(context, currentIndex + 1, colPath, rules, result);
				}).start();
			}, result);
			return;
		} while (true);
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
	protected AsyncWork<Pair<Object, Boolean>, SerializationException> deserializeCollectionAttributeValueElement(
		CollectionContext context, int elementIndex, String colPath, List<SerializationRule> rules
	) {
		return deserializeCollectionValueElement(context, elementIndex, colPath, rules);
	}
	
	// *** Map ***
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AsyncWork<Map<?,?>, SerializationException> deserializeMapValue(
		SerializationContext context, TypeDefinition typeDef, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, typeDef.getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		AsyncWork<Object, SerializationException> value = deserializeValueType(context, type, path, rules);
		AsyncWork<Map<?,?>, SerializationException> result = new AsyncWork<>();
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else
				try {
					result.unblockSuccess(getMap((ArrayList<MapEntry>)value.getResult(), typeDef, context, rules));
				} catch (SerializationException e) {
					result.error(e);
				}
			return result;
		}
		value.listenAsync(new DeserializationTask(() -> {
			try {
				result.unblockSuccess(getMap((ArrayList<MapEntry>)value.getResult(), typeDef, context, rules));
			} catch (SerializationException e) {
				result.error(e);
			}
		}), result);
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AsyncWork<Map<?,?>, SerializationException> deserializeMapAttributeValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		TypeDefinition type = new TypeDefinition(MapEntry.class, context.getAttribute().getType().getParameters());
		type = new TypeDefinition(ArrayList.class, type);
		Attribute fakeAttribute = new Attribute(context.getAttribute());
		fakeAttribute.setType(type);
		AttributeContext ctx = new AttributeContext(context.getParent(), fakeAttribute);
		AsyncWork<Object, SerializationException> value = deserializeCollectionAttributeValue(ctx, path, rules);
		AsyncWork<Map<?,?>, SerializationException> result = new AsyncWork<>();
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else 
				try {
					result.unblockSuccess(
						getMap((ArrayList<MapEntry>)value.getResult(), context.getAttribute().getType(), context, rules));
				} catch (SerializationException e) {
					result.error(e);
				}
			return result;
		}
		value.listenAsync(new DeserializationTask(() -> {
			try {
				result.unblockSuccess(
					getMap((ArrayList<MapEntry>)value.getResult(), context.getAttribute().getType(), context, rules));
			} catch (SerializationException e) {
				result.error(e);
			}
		}), result);
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
			throw new SerializationException("Error instantiating type " + type, e);
		}
	}
	
	// *** InputStream ***

	protected AsyncWork<InputStream, SerializationException> deserializeInputStreamValue(
		SerializationContext context, List<SerializationRule> rules
	) {
		AsyncWork<IO.Readable, SerializationException> io = deserializeIOReadableValue(context, rules);
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			return new AsyncWork<>(IOAsInputStream.get(io.getResult(), false), null);
		}
		AsyncWork<InputStream, SerializationException> result = new AsyncWork<>();
		io.listenInline(
			inputStream -> {
				if (inputStream == null)
					result.unblockSuccess(null);
				else
					result.unblockSuccess(IOAsInputStream.get(inputStream, false));
			},
			result
		);
		return result;
	}
	
	protected AsyncWork<InputStream, SerializationException> deserializeInputStreamAttributeValue(
		AttributeContext context, List<SerializationRule> rules
	) {
		AsyncWork<IO.Readable, SerializationException> io = deserializeIOReadableAttributeValue(context, rules);
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			return new AsyncWork<>(IOAsInputStream.get(io.getResult(), false), null);
		}
		AsyncWork<InputStream, SerializationException> result = new AsyncWork<>();
		io.listenInline(
			inputStream -> {
				if (inputStream == null)
					result.unblockSuccess(null);
				else
					result.unblockSuccess(IOAsInputStream.get(inputStream, false));
			},
			result
		);
		return result;
	}
	
	// *** IO.Readable ***
	
	protected List<StreamReferenceHandler> streamReferenceHandlers = new LinkedList<>();

	@Override
	public void addStreamReferenceHandler(StreamReferenceHandler handler) {
		streamReferenceHandlers.add(handler);
	}
	
	protected abstract AsyncWork<IO.Readable, SerializationException> deserializeIOReadableValue(
		SerializationContext context, List<SerializationRule> rules);
	
	protected abstract AsyncWork<IO.Readable, SerializationException> deserializeIOReadableAttributeValue(
		AttributeContext context, List<SerializationRule> rules);
	
	// *** byte[] by default using IO ***
	
	protected AsyncWork<byte[], SerializationException> deserializeByteArrayValue(SerializationContext context, List<SerializationRule> rules) {
		AsyncWork<IO.Readable, SerializationException> io = deserializeIOReadableValue(context, rules);
		AsyncWork<byte[], IOException> res = new AsyncWork<>();
		AsyncWork<byte[], SerializationException> result = new AsyncWork<>();
		res.listenInline(result, ioe -> new SerializationException("Error deserializing byte array", ioe));
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			IOUtil.readFully(io.getResult(), res);
			return result;
		}
		io.listenInline(
			r -> {
				if (r == null)
					result.unblockSuccess(null);
				else
					IOUtil.readFully(r, res);
			},
			result
		);
		return result;
	}
	
	protected AsyncWork<byte[], SerializationException> deserializeByteArrayAttributeValue(
		AttributeContext context, List<SerializationRule> rules
	) {
		AsyncWork<IO.Readable, SerializationException> io = deserializeIOReadableAttributeValue(context, rules);
		AsyncWork<byte[], IOException> res = new AsyncWork<>();
		AsyncWork<byte[], SerializationException> result = new AsyncWork<>();
		res.listenInline(result, ioe -> new SerializationException("Error deserializing byte array", ioe));
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			IOUtil.readFully(io.getResult(), res);
			return result;
		}
		io.listenInline(
			r -> {
				if (r == null)
					result.unblockSuccess(null);
				else
					IOUtil.readFully(r, res);
			},
			result
		);
		return result;
	}
	
	// *** Object ***

	@SuppressWarnings("unchecked")
	protected <T> AsyncWork<T, SerializationException> deserializeObjectValue(
		SerializationContext context, TypeDefinition type, String path, List<SerializationRule> rules
	) {
		AsyncWork<Object, SerializationException> start = startObjectValue(context, type, rules);
		if (start.isUnblocked()) {
			if (start.hasError()) return (AsyncWork<T, SerializationException>)start;
			Object instance = start.getResult();
			if (instance == null) return (AsyncWork<T, SerializationException>)start;
			AsyncWork<Object, SerializationException> result = new AsyncWork<>();
			deserializeObjectAttributes(context, instance, type, path, rules, result);
			return (AsyncWork<T, SerializationException>)result;
		}
		AsyncWork<Object, SerializationException> result = new AsyncWork<>();
		start.listenInline(
			instance -> {
				if (instance == null) result.unblockSuccess(null);
				else new DeserializationTask(() ->
					deserializeObjectAttributes(context, instance, type, path, rules, result)
				).start();
			}, result
		);
		return (AsyncWork<T, SerializationException>)result;
	}

	/** Instantiate the type if the start of an object has been found, null if null has been found, or an error.
	 * The context should be used to instantiate the object.
	 * The deserializer should handle the case a specific type to instantiate is specified,
	 * and may need to read a first attribute to get this type.
	 */
	protected abstract AsyncWork<Object, SerializationException> startObjectValue(
		SerializationContext context, TypeDefinition type, List<SerializationRule> rules);
	
	protected void deserializeObjectAttributes(
		SerializationContext parentContext, Object instance, TypeDefinition typeDef,
		String path, List<SerializationRule> rules, AsyncWork<Object, SerializationException> result
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
	
	protected void deserializeNextObjectAttribute(
		ObjectContext context, String path, List<SerializationRule> rules, AsyncWork<Object, SerializationException> result
	) {
		do {
			AsyncWork<String, SerializationException> name = deserializeObjectAttributeName(context);
			if (name.isUnblocked()) {
				if (name.hasError()) {
					result.error(name.getError());
					return;
				}
				String n = name.getResult();
				if (n == null) {
					result.unblockSuccess(context.getInstance());
					return;
				}
				Attribute a = context.getSerializationClass().getAttributeByName(n);
				if (a == null) {
					result.error(new SerializationException("Unknown attribute " + n + " for type "
						+ context.getInstance().getClass().getName() + " in " + path));
					return;
				}
				if (!a.canSet()) {
					result.error(new SerializationException("Attribute " + n + " cannot be set on type "
						+ context.getInstance().getClass().getName()));
					return;
				}
				ISynchronizationPoint<SerializationException> val =
					deserializeObjectAttributeValue(context, a, path + '.' + n, rules);
				if (val.isUnblocked()) {
					if (val.hasError()) {
						result.error(val.getError());
						return;
					}
					continue;
				}
				val.listenAsync(new DeserializationTask(() -> deserializeNextObjectAttribute(context, path, rules, result)), result);
				return;
			}
			name.listenInline(
				n -> {
					if (n == null) {
						result.unblockSuccess(context.getInstance());
						return;
					}
					Attribute a = context.getSerializationClass().getAttributeByName(n);
					if (a == null) {
						result.error(new SerializationException("Unknown attribute " + n + " for type "
							+ context.getInstance().getClass().getName()));
						return;
					}
					if (!a.canSet()) {
						result.error(new SerializationException("Attribute " + n + " cannot be set on type "
							+ context.getInstance().getClass().getName()));
						return;
					}
					new DeserializationTask(() -> {
						ISynchronizationPoint<SerializationException> val =
							deserializeObjectAttributeValue(context, a, path + '.' + n, rules);
						if (val.isUnblocked()) {
							if (val.hasError()) {
								result.error(val.getError());
								return;
							}
							deserializeNextObjectAttribute(context, path, rules, result);
							return;
						}
						val.listenAsync(new DeserializationTask(() ->
							deserializeNextObjectAttribute(context, path, rules, result)
						), result);
					}).start();
				}, result
			);
			return;
		} while (true);
	}
	
	/** Return the name of the attribute read, or null if the object is closed. */
	protected abstract AsyncWork<String, SerializationException> deserializeObjectAttributeName(ObjectContext context);
	
	protected ISynchronizationPoint<SerializationException> deserializeObjectAttributeValue(
		ObjectContext context, Attribute a, String path, List<SerializationRule> rules
	) {
		AttributeContext ctx = new AttributeContext(context, a);
		AsyncWork<?, SerializationException> value = deserializeObjectAttributeValue(ctx, path, rules);
		if (value.isUnblocked()) {
			if (value.hasError())
				return value;
			Object val = value.getResult();
			if (!a.ignore())
				try { a.setValue(context.getInstance(), val); }
				catch (SerializationException e) {
					return new SynchronizationPoint<>(e);
				}
			return value;
		}
		SynchronizationPoint<SerializationException> sp = new SynchronizationPoint<>();
		value.listenInline(
			val -> new DeserializationTask(() -> {
				if (!a.ignore())
					try { a.setValue(context.getInstance(), val); }
					catch (SerializationException e) {
						sp.error(e);
						return;
					}
				sp.unblock();
			}).start(), sp
		);
		return sp;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AsyncWork<?, SerializationException> deserializeObjectAttributeValue(
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
			AsyncWork<? extends CharSequence, SerializationException> str = deserializeStringAttributeValue(context);
			AsyncWork<Object, SerializationException> result = new AsyncWork<>();
			str.listenInline(
				string -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					if (c.isAssignableFrom(string.getClass())) {
						result.unblockSuccess(string);
						return;
					}
					for (Constructor<?> ctor : c.getConstructors()) {
						Class<?>[] params = ctor.getParameterTypes();
						if (params.length != 1) continue;
						if (params[0].isAssignableFrom(string.getClass())) {
							try { result.unblockSuccess(ctor.newInstance(string)); }
							catch (Exception t) {
								result.error(new SerializationException(
									"Error instantiating type " + c.getName(), t));
							}
							return;
						}
						if (params[0].isAssignableFrom(String.class)) {
							try { result.unblockSuccess(ctor.newInstance(string.toString())); }
							catch (Exception t) {
								result.error(new SerializationException(
									"Error instantiating type " + c.getName(), t));
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
			AsyncWork<? extends CharSequence, SerializationException> str = deserializeStringAttributeValue(context);
			AsyncWork<Object, SerializationException> result = new AsyncWork<>();
			str.listenInline(
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

	protected AsyncWork<Object, SerializationException> deserializeObjectAttributeObjectValue(
		AttributeContext context, String path, List<SerializationRule> rules
	) {
		return deserializeObjectValue(context, context.getAttribute().getType(), path, rules);
	}
	
}

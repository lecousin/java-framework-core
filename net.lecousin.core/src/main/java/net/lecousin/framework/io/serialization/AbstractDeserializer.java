package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnAttribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.util.Pair;

public abstract class AbstractDeserializer implements Deserializer {
	
	protected byte priority;
	
	protected abstract ISynchronizationPoint<Exception> initializeDeserialization(IO.Readable input);
	
	protected abstract ISynchronizationPoint<Exception> finalizeDeserialization();
	
	@Override
	public <T> AsyncWork<T, Exception> deserialize(TypeDefinition type, IO.Readable input, List<SerializationRule> rules) {
		priority = input.getPriority();
		ISynchronizationPoint<Exception> init = initializeDeserialization(input);
		AsyncWork<T, Exception> result = new AsyncWork<>();
		init.listenAsync(new DeserializationTask(() -> {
			AsyncWork<T, Exception> sp = deserializeValue(null, type, rules);
			sp.listenInline((obj) -> {
				finalizeDeserialization().listenInline(() -> { result.unblockSuccess(obj); }, result);
			}, result);
		}), result);
		return result;
	}
	
	protected List<SerializationRule> addRulesForType(SerializationClass type, List<SerializationRule> currentList) {
		currentList = TypeAnnotationToRule.addRules(type.getType().getBase(), currentList);
		currentList = AttributeAnnotationToRuleOnType.addRules(type, false, currentList);
		return currentList;
	}
	
	protected List<SerializationRule> addRulesForAttribute(Attribute a, List<SerializationRule> currentList) {
		return AttributeAnnotationToRuleOnAttribute.addRules(a, true, currentList);
	}
	
	protected class DeserializationTask extends Task.Cpu<Void, NoException> {
		public DeserializationTask(Runnable run) {
			super("Deserialization using " + AbstractDeserializer.this.getClass().getName(), priority);
			this.run = run;
		}
		
		private Runnable run;
		
		@Override
		public Void run() {
			run.run();
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> AsyncWork<T, Exception> deserializeValue(SerializationContext context, TypeDefinition type, List<SerializationRule> rules) {
		TypeDefinition newType = type;
		List<TypeDefinition> rulesTypes = new ArrayList<>(rules.size());
		for (SerializationRule rule : rules) {
			rulesTypes.add(newType);
			newType = rule.getDeserializationType(newType, context);
		}
		
		AsyncWork<?, Exception> value = deserializeValueType(context, newType, rules);
		AsyncWork<T, Exception> result = new AsyncWork<>();
		if (value.isUnblocked()) {
			if (value.hasError()) result.error(value.getError());
			else {
				Object o = value.getResult();
				ListIterator<TypeDefinition> itType = rulesTypes.listIterator(rules.size());
				ListIterator<SerializationRule> itRule = rules.listIterator(rules.size());
				while (itRule.hasPrevious())
					try { o = itRule.previous().getDeserializationValue(o, itType.previous(), context); }
					catch (Exception e) {
						result.error(e);
						return result;
					}
				result.unblockSuccess((T)o);
			}
			return result;
		}
		value.listenAsync(new DeserializationTask(() -> {
			Object o = value.getResult();
			ListIterator<TypeDefinition> itType = rulesTypes.listIterator(rules.size());
			ListIterator<SerializationRule> itRule = rules.listIterator(rules.size());
			while (itRule.hasPrevious())
				try { o = itRule.next().getDeserializationValue(o, itType.next(), context); }
				catch (Exception e) {
					result.error(e);
					return;
				}
			result.unblockSuccess((T)o);
		}), result);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> AsyncWork<T, Exception> deserializeValueType(SerializationContext context, TypeDefinition type, List<SerializationRule> rules) {
		Class<?> c = type.getBase();
		if (void.class.equals(c) || Void.class.equals(c))
			return new AsyncWork<>(null, null);
		
		if (boolean.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeBooleanValue(false);
		if (Boolean.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeBooleanValue(true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeNumericValue(c, false);
		if (Number.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeNumericValue(c, true);
			
		if (char.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeCharacterValue(false);
		if (Character.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeCharacterValue(true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringValue();
			AsyncWork<T, Exception> result = new AsyncWork<>();
			str.listenInline(
				(string) -> {
					if (string != null && c.isAssignableFrom(string.getClass()))
						result.unblockSuccess((T)string);
					else {
						for (Constructor<?> ctor : c.getConstructors()) {
							Class<?>[] params = ctor.getParameterTypes();
							if (params.length != 1) continue;
							if (string == null) {
								try {
									result.unblockSuccess((T)ctor.newInstance(new Object[] { null }));
									return;
								} catch (Throwable t) {
									continue;
								}
							}
							if (params[0].isAssignableFrom(string.getClass())) {
								try { result.unblockSuccess((T)ctor.newInstance(string)); }
								catch (Throwable t) {
									result.error(new Exception("Error instantiating type " + c.getName(), t));
								}
								return;
							}
							if (params[0].isAssignableFrom(String.class)) {
								try { result.unblockSuccess((T)ctor.newInstance(string.toString())); }
								catch (Throwable t) {
									result.error(new Exception("Error instantiating type " + c.getName(), t));
								}
								return;
							}
						}
						result.error(new Exception("Type " + c.getName()
							+ " does not have a compatible constructor with parameter type " + string.getClass()
							+ " or String"));
					}
				},
				result
			);
			return result;
		}

		if (c.isEnum()) {
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringValue();
			AsyncWork<T, Exception> result = new AsyncWork<>();
			str.listenInline(
				(string) -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					try {
						result.unblockSuccess((T)Enum.valueOf((Class<? extends Enum>)c, string.toString()));
					} catch (IllegalArgumentException e) {
						result.error(new Exception("Unknown enum value '" + string + "' for " + c.getName()));
					}
				},
				result
			);
			return result;
		}

		if (Collection.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeCollectionValue(context, type, rules);

		/*
		if (Map.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeMapValue(context, type, rules);
		*/
		
		if (InputStream.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeInputStreamValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeIOReadableValue(context, rules);

		return (AsyncWork<T, Exception>)deserializeObjectValue(context, type, rules);
	}
	
	// *** boolean ***
	
	protected abstract AsyncWork<Boolean, Exception> deserializeBooleanValue(boolean nullable);

	@SuppressWarnings("unused")
	protected AsyncWork<Boolean, Exception> deserializeBooleanAttributeValue(AttributeContext context, boolean nullable) {
		return deserializeBooleanValue(nullable);
	}
	
	// *** numeric ***

	protected abstract AsyncWork<? extends Number, Exception> deserializeNumericValue(Class<?> type, boolean nullable);

	protected AsyncWork<? extends Number, Exception> deserializeNumericAttributeValue(AttributeContext context, boolean nullable) {
		return deserializeNumericValue(context.getAttribute().getType().getBase(), nullable);
	}
	
	/** Convert a BigDecimal into the specified type. */
	public static void convertBigDecimalValue(BigDecimal n, Class<?> type, AsyncWork<Number, Exception> result) {
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
				throw new Exception("Unknown numeric value type " + type.getName());
		} catch (Exception e) {
			result.error(e);
		}
	}
	
	public static Number convertStringToInteger(Class<?> type, String str, Class<? extends IntegerUnit> targetUnit) throws Exception {
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
					throw new Exception("Unknown integer unit: " + unitStr);
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
		throw new Exception("Unknown integer type " + type.getName());
	}
	
	// *** character ***
	
	/** By default, deserialize a string and get the first character. */
	protected AsyncWork<Character, Exception> deserializeCharacterValue(boolean nullable) {
		AsyncWork<? extends CharSequence, Exception> read = deserializeStringValue();
		AsyncWork<Character, Exception> result = new AsyncWork<>();
		read.listenInline((string) -> {
			if (string == null || string.length() == 0) {
				if (nullable)
					result.unblockSuccess(null);
				else
					result.error(new Exception("Character value expected"));
				return;
			}
			if (string.length() > 1) {
				result.error(new Exception("A single character value is expected, " + string.length() + " characters found"));
				return;
			}
			result.unblockSuccess(Character.valueOf(string.charAt(0)));
		}, result);
		return result;
	}
	
	/** By default, deserialize a string and get the first character. */
	protected AsyncWork<Character, Exception> deserializeCharacterAttributeValue(AttributeContext context, boolean nullable) {
		Attribute fakeAttr = new Attribute(context.getAttribute());
		fakeAttr.setType(new TypeDefinition(String.class));
		AttributeContext fakeContext = new AttributeContext(context.getParent(), fakeAttr);
		AsyncWork<? extends CharSequence, Exception> read = deserializeStringAttributeValue(fakeContext);
		AsyncWork<Character, Exception> result = new AsyncWork<>();
		read.listenInline((string) -> {
			if (string == null || string.length() == 0) {
				if (nullable)
					result.unblockSuccess(null);
				else
					result.error(new Exception("Character value expected"));
				return;
			}
			if (string.length() > 1) {
				result.error(new Exception("A single character value is expected, " + string.length() + " characters found"));
				return;
			}
			result.unblockSuccess(Character.valueOf(string.charAt(0)));
		}, result);
		return result;
	}
	
	// *** string ***
	
	protected abstract AsyncWork<? extends CharSequence, Exception> deserializeStringValue();
	
	protected AsyncWork<? extends CharSequence, Exception> deserializeStringAttributeValue(@SuppressWarnings("unused") AttributeContext context) {
		return deserializeStringValue();
	}
	
	// *** Collection ***
	
	protected AsyncWork<Collection<?>, Exception> deserializeCollectionValue(SerializationContext context, TypeDefinition type, List<SerializationRule> rules) {
		AsyncWork<Boolean, Exception> start = startCollectionValue();
		AsyncWork<Collection<?>, Exception> result = new AsyncWork<>();
		if (start.isUnblocked()) {
			if (start.hasError()) return new AsyncWork<>(null, start.getError());
			Collection<?> col;
			try { col = (Collection<?>)SerializationClass.instantiate(type, context, rules); }
			catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
			TypeDefinition elementType;
			if (type.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
			CollectionContext ctx = new CollectionContext(context, col, elementType);
			deserializeNextCollectionValueElement(ctx, 0, rules, result);
		} else {
			TypeDefinition elementType;
			if (type.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
			start.listenAsync(new DeserializationTask(() -> {
				try {
					Collection<?> col = (Collection<?>)SerializationClass.instantiate(type, context, rules);
					CollectionContext ctx = new CollectionContext(context, col, elementType);
					deserializeNextCollectionValueElement(ctx, 0, rules, result);
				} catch (Exception e) {
					result.error(e);
				}
			}), result);
		}
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected abstract AsyncWork<Boolean, Exception> startCollectionValue();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void deserializeNextCollectionValueElement(CollectionContext context, int elementIndex, List<SerializationRule> rules, AsyncWork<Collection<?>, Exception> result) {
		do {
			AsyncWork<Pair<Object, Boolean>, Exception> next = deserializeCollectionValueElement(context, elementIndex, rules);
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				Pair<Object, Boolean> p = next.getResult();
				if (!p.getValue2().booleanValue()) {
					// end of collection
					result.unblockSuccess(context.getCollection());
					return;
				}
				Object element = p.getValue1();
				if (element != null && !context.getElementType().getBase().isAssignableFrom(element.getClass())) {
					result.error(new Exception("Invalid collection element type " + element.getClass().getName() + ", expected is " + context.getElementType().getBase().getName()));
					return;
				}
				((Collection)context.getCollection()).add(element);
				elementIndex++;
				continue;
			}
			int currentIndex = elementIndex;
			next.listenInline(() -> {
				if (next.hasError()) {
					if (next.hasError()) {
						result.error(next.getError());
						return;
					}
					Pair<Object, Boolean> p = next.getResult();
					if (!p.getValue2().booleanValue()) {
						// end of collection
						result.unblockSuccess(context.getCollection());
						return;
					}
					new DeserializationTask(() -> {
						Object element = p.getValue1();
						if (element != null && !context.getElementType().getBase().isAssignableFrom(element.getClass())) {
							result.error(new Exception("Invalid collection element type " + element.getClass().getName() + ", expected is " + context.getElementType().getBase().getName()));
							return;
						}
						((Collection)context.getCollection()).add(element);
						deserializeNextCollectionValueElement(context, currentIndex + 1, rules, result);
					}).start();
				}
			});
			return;
		} while (true);
	}
	
	/** Return the element (possibly null) with true if an element is found, or null with false if the end of the collection has been reached. */
	protected abstract AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionValueElement(CollectionContext context, int elementIndex, List<SerializationRule> rules);
	
	protected AsyncWork<Collection<?>, Exception> deserializeCollectionAttributeValue(AttributeContext context, List<SerializationRule> rules) {
		AsyncWork<Boolean, Exception> start = startCollectionAttributeValue(context);
		AsyncWork<Collection<?>, Exception> result = new AsyncWork<>();
		if (start.isUnblocked()) {
			if (start.hasError()) return new AsyncWork<>(null, start.getError());
			TypeDefinition colType = context.getAttribute().getType();
			Collection<?> col;
			try { col = (Collection<?>)SerializationClass.instantiate(colType, context, rules); }
			catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
			TypeDefinition elementType;
			if (colType.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = colType.getParameters().get(0);
			CollectionContext ctx = new CollectionContext(context, col, elementType);
			deserializeNextCollectionAttributeValueElement(ctx, 0, rules, result);
		} else {
			TypeDefinition colType = context.getAttribute().getType();
			TypeDefinition elementType;
			if (colType.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = colType.getParameters().get(0);
			start.listenAsync(new DeserializationTask(() -> {
				try {
					Collection<?> col = (Collection<?>)SerializationClass.instantiate(colType, context, rules);
					CollectionContext ctx = new CollectionContext(context, col, elementType);
					deserializeNextCollectionAttributeValueElement(ctx, 0, rules, result);
				} catch (Exception e) {
					result.error(e);
				}
			}), result);
		}
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected AsyncWork<Boolean, Exception> startCollectionAttributeValue(@SuppressWarnings("unused") AttributeContext context) {
		return startCollectionValue();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void deserializeNextCollectionAttributeValueElement(CollectionContext context, int elementIndex, List<SerializationRule> rules, AsyncWork<Collection<?>, Exception> result) {
		do {
			AsyncWork<Pair<Object, Boolean>, Exception> next = deserializeCollectionAttributeValueElement(context, elementIndex, rules);
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				Pair<Object, Boolean> p = next.getResult();
				if (!p.getValue2().booleanValue()) {
					// end of collection
					result.unblockSuccess(context.getCollection());
					return;
				}
				Object element = p.getValue1();
				if (element != null && !context.getElementType().getBase().isAssignableFrom(element.getClass())) {
					result.error(new Exception("Invalid collection element type " + element.getClass().getName() + ", expected is " + context.getElementType().getBase().getName()));
					return;
				}
				((Collection)context.getCollection()).add(element);
				elementIndex++;
				continue;
			}
			int currentIndex = elementIndex;
			next.listenInline(() -> {
				if (next.hasError()) {
					if (next.hasError()) {
						result.error(next.getError());
						return;
					}
					Pair<Object, Boolean> p = next.getResult();
					if (!p.getValue2().booleanValue()) {
						// end of collection
						result.unblockSuccess(context.getCollection());
						return;
					}
					new DeserializationTask(() -> {
						Object element = p.getValue1();
						if (element != null && !context.getElementType().getBase().isAssignableFrom(element.getClass())) {
							result.error(new Exception("Invalid collection element type " + element.getClass().getName() + ", expected is " + context.getElementType().getBase().getName()));
							return;
						}
						((Collection)context.getCollection()).add(element);
						deserializeNextCollectionAttributeValueElement(context, currentIndex + 1, rules, result);
					}).start();
				}
			});
			return;
		} while (true);
	}
	
	/** Return the element with true if an element is found, or null with false if the end of the collection has been reached. */
	protected AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionAttributeValueElement(CollectionContext context, int elementIndex, List<SerializationRule> rules) {
		return deserializeCollectionValueElement(context, elementIndex, rules);
	}
	
	// *** Map ***
	
	/*
	protected abstract AsyncWork<Map<?,?>, Exception> deserializeMapValue(SerializationContext context, TypeDefinition type, List<SerializationRule> rules);
	
	protected abstract AsyncWork<Map<?,?>, Exception> deserializeMapAttributeValue(AttributeContext context, List<SerializationRule> rules);
	*/
	
	// *** InputStream ***

	protected AsyncWork<InputStream, Exception> deserializeInputStreamValue(SerializationContext context, List<SerializationRule> rules) {
		AsyncWork<IO.Readable, Exception> io = deserializeIOReadableValue(context, rules);
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			return new AsyncWork<>(IOAsInputStream.get(io.getResult()), null);
		}
		AsyncWork<InputStream, Exception> result = new AsyncWork<>();
		io.listenInline(
			(inputStream) -> {
				result.unblockSuccess(IOAsInputStream.get(inputStream));
			},
			result
		);
		return result;
	}
	
	protected AsyncWork<InputStream, Exception> deserializeInputStreamAttributeValue(AttributeContext context, List<SerializationRule> rules) {
		AsyncWork<IO.Readable, Exception> io = deserializeIOReadableAttributeValue(context, rules);
		if (io.isUnblocked()) {
			if (io.hasError()) return new AsyncWork<>(null, io.getError());
			if (io.getResult() == null) return new AsyncWork<>(null, null);
			return new AsyncWork<>(IOAsInputStream.get(io.getResult()), null);
		}
		AsyncWork<InputStream, Exception> result = new AsyncWork<>();
		io.listenInline(
			(inputStream) -> {
				result.unblockSuccess(IOAsInputStream.get(inputStream));
			},
			result
		);
		return result;
	}
	
	// *** IO.Readable ***
	
	protected abstract AsyncWork<IO.Readable, Exception> deserializeIOReadableValue(SerializationContext context, List<SerializationRule> rules);
	
	protected abstract AsyncWork<IO.Readable, Exception> deserializeIOReadableAttributeValue(AttributeContext context, List<SerializationRule> rules);
	
	// *** Object ***

	@SuppressWarnings("unchecked")
	protected <T> AsyncWork<T, Exception> deserializeObjectValue(SerializationContext context, TypeDefinition type, List<SerializationRule> rules) {
		AsyncWork<Object, Exception> start = startObjectValue(context, type, rules);
		if (start.isUnblocked()) {
			if (start.hasError()) return (AsyncWork<T, Exception>)start;
			Object instance = start.getResult();
			if (instance == null) return (AsyncWork<T, Exception>)start;
			AsyncWork<Object, Exception> result = new AsyncWork<>();
			deserializeObjectAttributes(context, instance, rules, result);
			return (AsyncWork<T, Exception>)result;
		}
		AsyncWork<Object, Exception> result = new AsyncWork<>();
		start.listenInline(
			(instance) -> {
				if (instance == null) result.unblockSuccess(null);
				else new DeserializationTask(() -> {
					deserializeObjectAttributes(context, instance, rules, result);
				}).start();
			}, result
		);
		return (AsyncWork<T, Exception>)result;
	}

	/** Instantiate the type if the start of an object has been found, null if null has been found, or an error.
	 * The context should be used to instantiate the object.
	 * The deserializer should handle the case a specific type to instantiate is specified,
	 * and may need to read a first attribute to get this type.
	 */
	protected abstract AsyncWork<Object, Exception> startObjectValue(SerializationContext context, TypeDefinition type, List<SerializationRule> rules);
	
	protected void deserializeObjectAttributes(SerializationContext parentContext, Object instance, List<SerializationRule> rules, AsyncWork<Object, Exception> result) {
		SerializationClass sc = new SerializationClass(new TypeDefinition(instance.getClass()));
		ObjectContext ctx = new ObjectContext(parentContext, instance, sc);
		rules = addRulesForType(sc, rules);
		sc.apply(rules, ctx);
		deserializeNextObjectAttribute(ctx, rules, result);
	}
	
	protected void deserializeNextObjectAttribute(ObjectContext context, List<SerializationRule> rules, AsyncWork<Object, Exception> result) {
		do {
			AsyncWork<String, Exception> name = deserializeObjectAttributeName();
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
					result.error(new Exception("Unknown attribute " + n + " for type " + context.getInstance().getClass().getName()));
					return;
				}
				if (!a.canSet()) {
					result.error(new Exception("Attribute " + n + " cannot be set on type " + context.getInstance().getClass().getName()));
					return;
				}
				ISynchronizationPoint<Exception> val = deserializeObjectAttributeValue(context, a, rules);
				if (val.isUnblocked()) {
					if (val.hasError()) {
						result.error(val.getError());
						return;
					}
					continue;
				}
				val.listenAsync(new DeserializationTask(() -> {
					deserializeNextObjectAttribute(context, rules, result);
				}), result);
				return;
			}
			name.listenInline(
				(n) -> {
					if (n == null) {
						result.unblockSuccess(context.getInstance());
						return;
					}
					Attribute a = context.getSerializationClass().getAttributeByName(n);
					if (a == null) {
						result.error(new Exception("Unknown attribute " + n + " for type " + context.getInstance().getClass().getName()));
						return;
					}
					if (!a.canSet()) {
						result.error(new Exception("Attribute " + n + " cannot be set on type " + context.getInstance().getClass().getName()));
						return;
					}
					new DeserializationTask(() -> {
						ISynchronizationPoint<Exception> val = deserializeObjectAttributeValue(context, a, rules);
						if (val.isUnblocked()) {
							if (val.hasError()) {
								result.error(val.getError());
								return;
							}
							deserializeNextObjectAttribute(context, rules, result);
							return;
						}
						val.listenAsync(new DeserializationTask(() -> {
							deserializeNextObjectAttribute(context, rules, result);
						}), result);
					}).start();
				}, result
			);
			return;
		} while (true);
	}
	
	/** Return the name of the attribute read, or null if the object is closed. */
	protected abstract AsyncWork<String, Exception> deserializeObjectAttributeName();
	
	protected ISynchronizationPoint<Exception> deserializeObjectAttributeValue(ObjectContext context, Attribute a, List<SerializationRule> rules) {
		AttributeContext ctx = new AttributeContext(context, a);
		AsyncWork<?, Exception> value = deserializeObjectAttributeValue(ctx, rules);
		if (value.isUnblocked()) {
			if (value.hasError())
				return value;
			Object val = value.getResult();
			if (!a.ignore())
				try { a.setValue(context.getInstance(), val); }
				catch (Exception e) {
					return new SynchronizationPoint<>(e);
				}
			return value;
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		value.listenInline(
			(val) -> {
				new DeserializationTask(() -> {
					if (!a.ignore())
						try { a.setValue(context.getInstance(), val); }
						catch (Exception e) {
							sp.error(e);
							return;
						}
					sp.unblock();
				}).start();
			}, sp
		);
		return sp;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AsyncWork<?, Exception> deserializeObjectAttributeValue(AttributeContext context, List<SerializationRule> rules) {
		Attribute a = context.getAttribute();
		TypeDefinition type = a.getType();
		Class<?> c = type.getBase();
		rules = addRulesForAttribute(a, rules);
		
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
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringAttributeValue(context);
			AsyncWork<Object, Exception> result = new AsyncWork<>();
			str.listenInline(
				(string) -> {
					if (string != null && c.isAssignableFrom(string.getClass()))
						result.unblockSuccess(string);
					else {
						for (Constructor<?> ctor : c.getConstructors()) {
							Class<?>[] params = ctor.getParameterTypes();
							if (params.length != 1) continue;
							if (string == null) {
								try {
									result.unblockSuccess(ctor.newInstance(new Object[] { null }));
									return;
								} catch (Throwable t) {
									continue;
								}
							}
							if (params[0].isAssignableFrom(string.getClass())) {
								try { result.unblockSuccess(ctor.newInstance(string)); }
								catch (Throwable t) {
									result.error(new Exception("Error instantiating type " + c.getName(), t));
								}
								return;
							}
							if (params[0].isAssignableFrom(String.class)) {
								try { result.unblockSuccess(ctor.newInstance(string.toString())); }
								catch (Throwable t) {
									result.error(new Exception("Error instantiating type " + c.getName(), t));
								}
								return;
							}
						}
						result.error(new Exception("Type " + c.getName()
							+ " does not have a compatible constructor with parameter type " + string.getClass()
							+ " or String"));
					}
				},
				result
			);
			return result;
		}

		if (c.isEnum()) {
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringAttributeValue(context);
			AsyncWork<Object, Exception> result = new AsyncWork<>();
			str.listenInline(
				(string) -> {
					if (string == null) {
						result.unblockSuccess(null);
						return;
					}
					try {
						result.unblockSuccess(Enum.valueOf((Class<? extends Enum>)c, string.toString()));
					} catch (IllegalArgumentException e) {
						result.error(new Exception("Unknown enum value '" + string + "' for " + c.getName()));
					}
				},
				result
			);
			return result;
		}

		if (Collection.class.isAssignableFrom(c))
			return deserializeCollectionAttributeValue(context, rules);

		/*
		if (Map.class.isAssignableFrom(c))
			return deserializeMapAttributeValue(context, rules);
		*/
		
		if (InputStream.class.isAssignableFrom(c))
			return deserializeInputStreamAttributeValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return deserializeIOReadableAttributeValue(context, rules);

		return deserializeObjectAttributeObjectValue(context, rules);
	}

	protected AsyncWork<Object, Exception> deserializeObjectAttributeObjectValue(AttributeContext context, List<SerializationRule> rules) {
		return deserializeObjectValue(context, context.getAttribute().getType(), rules);
	}
	
}

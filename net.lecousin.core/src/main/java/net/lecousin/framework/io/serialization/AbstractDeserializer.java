package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.util.Pair;

public abstract class AbstractDeserializer<Input> implements Deserializer {

	protected byte priority;
	
	protected abstract AsyncWork<Input, Exception> initializeInput(IO.Readable input);
	
	protected abstract ISynchronizationPoint<Exception> finalizeInput(Input in);
	
	@Override
	public <T> AsyncWork<T, Exception> deserialize(TypeDefinition type, IO.Readable input, List<SerializationRule> rules) {
		priority = input.getPriority();
		AsyncWork<Input, Exception> init = initializeInput(input);
		AsyncWork<T, Exception> result = new AsyncWork<>();
		init.listenAsynch(
			new Task.Cpu<Void, NoException>("Serialization", priority) {
				@Override
				public Void run() {
					Input in = init.getResult();
					AsyncWork<T, Exception> sp = deserializeValue(type, in, rules);
					sp.listenInline((obj) -> {
						finalizeInput(in).listenInline(() -> { result.unblockSuccess(obj); }, result);
					}, result);
					return null;
				}
			},
			result
		);
		return result;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> AsyncWork<T, Exception> deserializeValue(TypeDefinition type, Input input, List<SerializationRule> rules) {
		Class<?> c = type.getBase();
		if (void.class.equals(c) || Void.class.equals(c))
			return new AsyncWork<>(null, null);
		
		if (boolean.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeBooleanValue(input, false);
		if (Boolean.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeBooleanValue(input, true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeNumericValue(c, input, false);
		if (Number.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeNumericValue(c, input, true);
			
		if (char.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeCharacterValue(input, false);
		if (Character.class.equals(c))
			return (AsyncWork<T, Exception>)deserializeCharacterValue(input, true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringValue(input);
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
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringValue(input);
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
			return (AsyncWork<T, Exception>)deserializeCollectionValue(type, input, rules);

		if (Map.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeMapValue(type, input, rules);
		
		if (InputStream.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeInputStreamValue(input, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return (AsyncWork<T, Exception>)deserializeIOReadableValue(input, rules);

		return (AsyncWork<T, Exception>)deserializeObjectValue(null, type, input, rules);
	}
	
	// *** boolean ***
	
	protected abstract AsyncWork<Boolean, Exception> deserializeBooleanValue(Input input, boolean nullable);

	protected abstract AsyncWork<Boolean, Exception> deserializeBooleanAttributeValue(Object containerInstance, Attribute a, Input input, boolean nullable);
	
	// *** numeric ***

	protected abstract AsyncWork<? extends Number, Exception> deserializeNumericValue(Class<?> type, Input input, boolean nullable);

	protected abstract AsyncWork<? extends Number, Exception> deserializeNumericAttributeValue(Object containerInstance, Attribute a, Input input, boolean nullable);
	
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
	protected AsyncWork<Character, Exception> deserializeCharacterValue(Input input, boolean nullable) {
		AsyncWork<? extends CharSequence, Exception> read = deserializeStringValue(input);
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
	protected AsyncWork<Character, Exception> deserializeCharacterAttributeValue(Object containerInstance, Attribute a, Input input, boolean nullable) {
		Attribute fakeAttr = new Attribute(a);
		fakeAttr.setType(new TypeDefinition(String.class));
		AsyncWork<? extends CharSequence, Exception> read = deserializeStringAttributeValue(containerInstance, fakeAttr, input);
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
	
	protected abstract AsyncWork<? extends CharSequence, Exception> deserializeStringValue(Input input);
	
	protected abstract AsyncWork<? extends CharSequence, Exception> deserializeStringAttributeValue(Object containerInstance, Attribute a, Input input);
	
	// *** Collection ***
	
	protected AsyncWork<Collection<?>, Exception> deserializeCollectionValue(TypeDefinition type, Input input, List<SerializationRule> rules) {
		AsyncWork<Boolean, Exception> start = startCollectionValue(input);
		AsyncWork<Collection<?>, Exception> result = new AsyncWork<>();
		if (start.isUnblocked()) {
			if (start.hasError()) return new AsyncWork<>(null, start.getError());
			Collection<?> col;
			try { col = (Collection<?>)SerializationClass.instantiate(type.getBase(), rules); }
			catch (Exception e) {
				return new AsyncWork<>(null, e);
			}
			TypeDefinition elementType;
			if (type.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
			deserializeNextCollectionValueElement(col, elementType, input, rules, result);
		} else {
			TypeDefinition elementType;
			if (type.getParameters().isEmpty())
				return new AsyncWork<>(null, new Exception("Cannot deserialize collection without an element type specified"));
			elementType = type.getParameters().get(0);
			start.listenAsynch(
				new Task.Cpu<Void, NoException>("Deserialization", priority) {
					@Override
					public Void run() {
						Collection<?> col;
						try { col = (Collection<?>)SerializationClass.instantiate(type.getBase(), rules); }
						catch (Exception e) {
							result.error(e);
							return null;
						}
						deserializeNextCollectionValueElement(col, elementType, input, rules, result);
						return null;
					}
				}, result
			);
		}
		return result;
	}
	
	/** Return true if the start has been found, false if null has been found, or an error. */
	protected abstract AsyncWork<Boolean, Exception> startCollectionValue(Input input);
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void deserializeNextCollectionValueElement(Collection<?> col, TypeDefinition elementType, Input input, List<SerializationRule> rules, AsyncWork<Collection<?>, Exception> result) {
		AsyncWork<Pair<Object, Boolean>, Exception> next = deserializeCollectionValueElement(elementType, input, rules);
		if (next.isUnblocked()) {
			if (next.hasError()) {
				result.error(next.getError());
				return;
			}
			Pair<Object, Boolean> p = next.getResult();
			if (!p.getValue2().booleanValue()) {
				// end of collection
				result.unblockSuccess(col);
				return;
			}
			((Collection)col).add(p.getValue1());
			deserializeNextCollectionValueElement(col, elementType, input, rules, result);
			return;
		}
		next.listenInline(() -> {
			if (next.hasError()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				Pair<Object, Boolean> p = next.getResult();
				if (!p.getValue2().booleanValue()) {
					// end of collection
					result.unblockSuccess(col);
					return;
				}
				new Task.Cpu<Void, NoException>("Deserialization", priority) {
					@Override
					public Void run() {
						((Collection)col).add(p.getValue1());
						deserializeNextCollectionValueElement(col, elementType, input, rules, result);
						return null;
					}
				}.start();
			}
		});
	}
	
	/** Return the element with true if an element is found, or null with false if the end of the collection has been reached. */
	protected abstract AsyncWork<Pair<Object, Boolean>, Exception> deserializeCollectionValueElement(TypeDefinition elementType, Input input, List<SerializationRule> rules);
	
	protected abstract AsyncWork<Map<?,?>, Exception> deserializeCollectionAttributeValue(Object containerInstance, Attribute a, Input input, List<SerializationRule> rules);
	
	// *** Map ***
	
	protected abstract AsyncWork<Map<?,?>, Exception> deserializeMapValue(TypeDefinition type, Input input, List<SerializationRule> rules);
	
	protected abstract AsyncWork<Map<?,?>, Exception> deserializeMapAttributeValue(Object containerInstance, Attribute a, Input input, List<SerializationRule> rules);
	
	// *** InputStream ***

	protected AsyncWork<InputStream, Exception> deserializeInputStreamValue(Input input, List<SerializationRule> rules) {
		AsyncWork<IO.Readable, Exception> io = deserializeIOReadableValue(input, rules);
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
	
	protected AsyncWork<InputStream, Exception> deserializeInputStreamAttributeValue(Object containerInstance, Attribute a, Input input, List<SerializationRule> rules) {
		AsyncWork<IO.Readable, Exception> io = deserializeIOReadableAttributeValue(containerInstance, a, input, rules);
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
	
	protected abstract AsyncWork<IO.Readable, Exception> deserializeIOReadableValue(Input input, List<SerializationRule> rules);
	
	protected abstract AsyncWork<IO.Readable, Exception> deserializeIOReadableAttributeValue(Object containerInstance, Attribute a, Input input, List<SerializationRule> rules);
	
	// *** Object ***

	@SuppressWarnings("unchecked")
	protected <T> AsyncWork<T, Exception> deserializeObjectValue(Object containerInstance, TypeDefinition type, Input input, List<SerializationRule> rules) {
		AsyncWork<Object, Exception> start = startObjectValue(containerInstance, type, input, rules);
		if (start.isUnblocked()) {
			if (start.hasError()) return (AsyncWork<T, Exception>)start;
			Object instance = start.getResult();
			if (instance == null) return (AsyncWork<T, Exception>)start;
			AsyncWork<Object, Exception> result = new AsyncWork<>();
			deserializeObjectAttributes(instance, type, input, rules, result);
			return (AsyncWork<T, Exception>)result;
		}
		AsyncWork<Object, Exception> result = new AsyncWork<>();
		start.listenInline(
			(instance) -> {
				if (instance == null) result.unblockSuccess(null);
				else new Task.Cpu<Void, NoException>("Deserialization", priority) {
					@Override
					public Void run() {
						deserializeObjectAttributes(instance, type, input, rules, result);
						return null;
					}
				}.start();
			}, result
		);
		return (AsyncWork<T, Exception>)result;
	}

	/** Instantiate the type if the start of an object has been found, null if null has been found, or an error.
	 * The container instance may be null in case it is the root element or inside a collection.
	 */
	protected abstract AsyncWork<Object, Exception> startObjectValue(Object containerInstance, TypeDefinition type, Input input, List<SerializationRule> rules);
	
	protected void deserializeObjectAttributes(Object instance, TypeDefinition type, Input input, List<SerializationRule> rules, AsyncWork<Object, Exception> result) {
		SerializationClass sc = new SerializationClass(type);
		rules = TypeAnnotationToRule.addRules(instance.getClass(), rules);
		sc.apply(rules, instance);
		deserializeNextObjectAttribute(instance, sc, input, rules, result);
	}
	
	protected void deserializeNextObjectAttribute(Object instance, SerializationClass sc, Input input, List<SerializationRule> rules, AsyncWork<Object, Exception> result) {
		AsyncWork<String, Exception> name = deserializeObjectAttributeName(input);
		if (name.isUnblocked()) {
			if (name.hasError()) {
				result.error(name.getError());
				return;
			}
			String n = name.getResult();
			if (n == null) {
				result.unblockSuccess(instance);
				return;
			}
			Attribute a = sc.getAttributeByName(n);
			if (a == null) {
				result.error(new Exception("Unknown attribute " + n + " for type " + instance.getClass().getName()));
				return;
			}
			if (!a.canSet()) {
				result.error(new Exception("Attribute " + n + " cannot be set on type " + instance.getClass().getName()));
				return;
			}
			deserializeObjectAttributeValue(instance, sc, a, input, rules, result);
			return;
		}
		name.listenInline(
			(n) -> {
				if (n == null) {
					result.unblockSuccess(instance);
					return;
				}
				Attribute a = sc.getAttributeByName(n);
				if (a == null) {
					result.error(new Exception("Unknown attribute " + n + " for type " + instance.getClass().getName()));
					return;
				}
				if (!a.canSet()) {
					result.error(new Exception("Attribute " + n + " cannot be set on type " + instance.getClass().getName()));
					return;
				}
				new Task.Cpu<Void, NoException>("Deserialization", priority) {
					@Override
					public Void run() {
						deserializeObjectAttributeValue(instance, sc, a, input, rules, result);
						return null;
					}
				}.start();
			}, result
		);
	}
	
	/** Return the name of the attribute read, or null if the object is closed. */
	protected abstract AsyncWork<String, Exception> deserializeObjectAttributeName(Input input);
	
	protected void deserializeObjectAttributeValue(Object instance, SerializationClass sc, Attribute a, Input input, List<SerializationRule> rules, AsyncWork<Object, Exception> result) {
		AsyncWork<?, Exception> value = deserializeObjectAttributeValue(instance, a, input, rules);
		if (value.isUnblocked()) {
			if (value.hasError()) {
				result.error(value.getError());
				return;
			}
			Object val = value.getResult();
			try { a.setValue(instance, val); }
			catch (Exception e) {
				result.error(e);
				return;
			}
			deserializeNextObjectAttribute(instance, sc, input, rules, result);
			return;
		}
		value.listenInline(
			(val) -> {
				new Task.Cpu<Void, NoException>("Deserialization", priority) {
					@Override
					public Void run() {
						try { a.setValue(instance, val); }
						catch (Exception e) {
							result.error(e);
							return null;
						}
						deserializeNextObjectAttribute(instance, sc, input, rules, result);
						return null;
					}
				}.start();
			}, result
		);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AsyncWork<?, Exception> deserializeObjectAttributeValue(Object instance, Attribute a, Input input, List<SerializationRule> rules) {
		TypeDefinition type = a.getType();
		Class<?> c = type.getBase();
		
		if (boolean.class.equals(c))
			return deserializeBooleanAttributeValue(instance, a, input, false);
		if (Boolean.class.equals(c))
			return deserializeBooleanAttributeValue(instance, a, input, true);
		
		if (byte.class.equals(c) ||
			short.class.equals(c) ||
			int.class.equals(c) ||
			long.class.equals(c) ||
			float.class.equals(c) ||
			double.class.equals(c))
			return deserializeNumericAttributeValue(instance, a, input, false);
		if (Number.class.isAssignableFrom(c))
			return deserializeNumericAttributeValue(instance, a, input, true);
			
		if (char.class.equals(c))
			return deserializeCharacterAttributeValue(instance, a, input, false);
		if (Character.class.equals(c))
			return deserializeCharacterAttributeValue(instance, a, input, true);
		
		if (CharSequence.class.isAssignableFrom(c)) {
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringAttributeValue(instance, a, input);
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
			AsyncWork<? extends CharSequence, Exception> str = deserializeStringAttributeValue(instance, a, input);
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
			return deserializeCollectionAttributeValue(instance, a, input, rules);

		if (Map.class.isAssignableFrom(c))
			return deserializeMapAttributeValue(instance, a, input, rules);
		
		if (InputStream.class.isAssignableFrom(c))
			return deserializeInputStreamAttributeValue(instance, a, input, rules);
		
		if (IO.Readable.class.isAssignableFrom(c))
			return deserializeIOReadableAttributeValue(instance, a, input, rules);

		return deserializeObjectAttributeObjectValue(instance, a, input, rules);
	}

	protected AsyncWork<Object, Exception> deserializeObjectAttributeObjectValue(Object containerInstance, Attribute a, Input input, List<SerializationRule> rules) {
		return deserializeObjectValue(containerInstance, a.getType(), input, rules);
	}
	
}

package net.lecousin.framework.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import net.lecousin.framework.collections.ListOfArrays;

/**
 * Utility methods for reflexivity.
 */
public final class ClassUtil {
	
	private ClassUtil() { /* no instance */ }

	/** Get all fields from the given class and all its super classes. */
	public static Iterable<Field> getAllFields(Class<?> cl) {
		Field[] fields = cl.getDeclaredFields();
		ListOfArrays<Field> list = new ListOfArrays<>();
		if (fields.length > 0) list.add(fields);
		while ((cl = cl.getSuperclass()) != null) {
			fields = cl.getDeclaredFields();
			if (fields.length > 0) list.add(fields);
		}
		return list;
	}
	
	/** Get all fields from the given class and all its super classes, with the ones from the root class first. */
	public static ArrayList<Field> getAllFieldsInheritedFirst(Class<?> cl) {
		ArrayList<Field[]> all = new ArrayList<>();
		Field[] fields = cl.getDeclaredFields();
		int total = fields.length;
		if (fields.length > 0) all.add(fields);
		while ((cl = cl.getSuperclass()) != null) {
			fields = cl.getDeclaredFields();
			total += fields.length;
			if (fields.length > 0) all.add(fields);
		}
		ArrayList<Field> result = new ArrayList<Field>(total);
		for (int i = all.size() - 1; i >= 0; --i)
			for (Field f : all.get(i))
				result.add(f);
		return result;
	}
	
	/** Search for a specific field from the given class and its super classes. */
	public static Field getField(Class<?> cl, String name) {
		do {
			Field[] fields = cl.getDeclaredFields();
			for (Field f : fields)
				if (f.getName().equals(name))
					return f;
		} while ((cl = cl.getSuperclass()) != null);
		return null;
	}
	
	/** Search for a setter method. */
	public static Method getSetter(Class<?> cl, String name) {
		name = name.substring(0,1).toUpperCase() + name.substring(1);
		for (Method m : cl.getMethods()) {
			if (!m.getName().equals("set" + name)) continue;
			if (m.getParameterTypes().length != 1) continue;
			if ((m.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
				return m;
		}
		return null;
	}
	
	/** Search for a getter method. */
	public static Method getGetter(Class<?> cl, String name) {
		name = name.substring(0,1).toUpperCase() + name.substring(1);
		for (Method m : cl.getMethods()) {
			if (!m.getName().equals("get" + name)) continue;
			if (m.getParameterTypes().length != 0) continue;
			if ((m.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) != Modifier.PUBLIC) continue;
			return m;
		}
		Field f = getField(cl, name);
		if (f == null)
			return null;
		if (boolean.class.equals(f.getType()) || Boolean.class.equals(f.getType())) {
			for (Method m : cl.getMethods()) {
				if (!m.getName().equals("is" + name)) continue;
				if (m.getParameterTypes().length != 0) continue;
				if ((m.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) != Modifier.PUBLIC) continue;
				if (!boolean.class.equals(m.getReturnType()) && ! Boolean.class.equals(m.getReturnType())) continue;
				return m;
			}
		}
		return null;
	}
	
	/** Return the package of the given class. */
	public static String getPackageName(Class<?> cl) {
		String n = cl.getName();
		int i = n.lastIndexOf('.');
		if (i < 0) return "";
		return n.substring(0, i);
	}

	/** Search a method compatible with the given parameters. */
	public static <T> Method getMethodFor(Class<T> cl, String name, Object[] params) {
		Method[] methods = cl.getMethods();
		for (Method m : methods) {
			if (!m.getName().equals(name))
				continue;
			Class<?>[] types = m.getParameterTypes();
			if (types.length != params.length)
				continue;
			boolean ok = true;
			for (int i = 0; i < params.length; ++i) {
				if (params[i] == null) {
					if (types[i].isPrimitive()) {
						ok = false;
						break;
					}
					continue;
				}
				if (types[i].isAssignableFrom(params[i].getClass())) continue;
				if (types[i].isPrimitive()) {
					if (types[i].equals(boolean.class) && params[i] instanceof Boolean) continue;
					if (types[i].equals(byte.class) && params[i] instanceof Byte) continue;
					if (types[i].equals(short.class) && params[i] instanceof Short) continue;
					if (types[i].equals(int.class) && params[i] instanceof Integer) continue;
					if (types[i].equals(long.class) && params[i] instanceof Long) continue;
				}
				ok = false;
				break;
			}
			if (!ok)
				continue;
			return m;
		}
		return null;
	}
	
	/** Search for a method. */
	public static Method getMethod(Class<?> cl, String name) {
		for (Method m : cl.getMethods())
			if (m.getName().equals(name))
				return m;
		return null;
	}

	/** Search for a method with a name and a specific number of parameters. */
	public static Method getMethod(Class<?> cl, String name, int nbParameters) {
		for (Method m : cl.getMethods())
			if (m.getName().equals(name) && m.getParameterTypes().length == nbParameters)
				return m;
		return null;
	}
	
	/** Set a value to a field, using a path. The path is a list of fields separated by dot. */
	public static boolean setFieldFromPath(Object object, String path, Object value) {
		String[] names = path.split("\\.");
		for (int i = 0; i < names.length; ++i) {
			try {
				try {
					Field f = object.getClass().getField(names[i]);
					if ((f.getModifiers() & Modifier.PUBLIC) != 0) {
						if (i < names.length - 1) {
							object = f.get(object);
							continue;
						}
						f.set(object, value);
						return true;
					}
				} catch (NoSuchFieldException e) { /* ignore */ }
				if (i < names.length - 1) {
					Method getter = getGetter(object.getClass(), names[i]);
					if (getter == null)
						return false;
					object = getter.invoke(object);
				} else {
					Method setter = getSetter(object.getClass(), names[i]);
					if (setter == null)
						return false;
					setter.invoke(object, value);
				}
			} catch (Throwable t) {
				return false;
			}
		}
		return false;
	}
	
	/** Search for a field using a path. The path is a list of fields separated by dot. */
	public static Object getFieldFromPath(Object object, String path)
	throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
		String[] names = path.split("\\.");
		for (int i = 0; i < names.length; ++i) {
			try {
				Field f = object.getClass().getField(names[i]);
				if ((f.getModifiers() & Modifier.PUBLIC) != 0) {
					Object obj = f.get(object);
					if (i < names.length - 1) {
						if (obj == null)
							throw new NoSuchFieldException("Cannot get field " + path + " because field " + names[i]
								+ " is null in class " + object.getClass().getName());
						object = obj;
						continue;
					}
					return obj;
				}
			} catch (NoSuchFieldException e) { /* ignore */ }
			Method getter = getGetter(object.getClass(), names[i]);
			if (getter == null)
				throw new NoSuchFieldException("Unknown field '" + names[i] + "' in class " + object.getClass().getName());
			Object obj = getter.invoke(object);
			if (i == names.length - 1)
				return obj;
			if (obj == null)
				throw new NoSuchFieldException("Cannot get field " + path + " because field " + names[i]
					+ " is null in class " + object.getClass().getName());
			object = obj;
		}
		throw new NoSuchFieldException("Unknown field path '" + path + "'");
	}
	
}

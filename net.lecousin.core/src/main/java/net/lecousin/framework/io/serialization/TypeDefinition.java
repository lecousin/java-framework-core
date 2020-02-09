package net.lecousin.framework.io.serialization;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.lecousin.framework.collections.CollectionsUtil;

/** Specify a type, with generic parameters. */
public class TypeDefinition {
	
	/** Create from the given type which is the type of an attribute or a method, knowing its container type. */
	@SuppressWarnings({ "rawtypes" })
	public TypeDefinition(TypeDefinition containerType, Type type) {
		if (type instanceof Class) {
			base = (Class<?>)type;
			parameters = new ArrayList<>(0);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)type;
			base = (Class<?>)pt.getRawType();
			Type[] params = pt.getActualTypeArguments();
			parameters = new ArrayList<>(params.length);
			for (Type p : params)
				parameters.add(new TypeDefinition(containerType, p));
		} else if ((type instanceof TypeVariable) && containerType != null) {
			String name = ((TypeVariable)type).getName();
			TypeVariable[] params = containerType.getBase().getTypeParameters();
			if (containerType.parameters.size() != params.length)
				throw new IllegalArgumentException("Cannot resolve type variable " + name
					+ " using parameters of " + containerType.base.getName());
			for (int i = 0; i < params.length; ++i)
				if (params[i].getName().equals(name)) {
					base = containerType.parameters.get(i).base;
					parameters = new ArrayList<>(containerType.parameters.get(i).parameters);
					return;
				}
			throw new IllegalArgumentException("Unexpected type " + type.getClass() + ": " + type.toString());
		} else if ((type instanceof WildcardType) || (type instanceof GenericArrayType)) {
			base = Object.class;
			parameters = new ArrayList<>(0);
		} else {
			throw new IllegalArgumentException("Unexpected type " + type.getClass() + ": " + type.toString());
		}
	}
	
	/** Create a type of the given class and generic parameters. */
	public TypeDefinition(Class<?> base, TypeDefinition... parameters) {
		this.base = base;
		this.parameters = Arrays.asList(parameters);
	}

	/** Create a type of the given class and generic parameters. */
	public TypeDefinition(Class<?> base, List<TypeDefinition> parameters) {
		this.base = base;
		this.parameters = new ArrayList<>(parameters);
	}
	
	/** Create a type, with the given definition of a field or method, and knowing the instance type.
	 * This will resolve the generic type variables as much as possible.
	 */
	@SuppressWarnings("rawtypes")
	public static TypeDefinition from(Class<?> instanceType, TypeDefinition definition) {
		if (instanceType.equals(definition.getBase()))
			return definition;
		TypeVariable[] types = instanceType.getTypeParameters();
		if (types.length == 0)
			return new TypeDefinition(instanceType);
		TypeDefinition[] params = new TypeDefinition[types.length];
		Type superClass = instanceType.getGenericSuperclass();
		if (superClass != null && getTypeParameters(superClass, definition, types, params))
			return new TypeDefinition(instanceType, params);
		for (Type superInt : instanceType.getGenericInterfaces())
			if (getTypeParameters(superInt, definition, types, params))
				return new TypeDefinition(instanceType, params);
		return new TypeDefinition(instanceType);
	}
	
	/** Fill the generic parameters. */
	@SuppressWarnings("rawtypes")
	public static boolean getTypeParameters(Type superType, TypeDefinition finalType, TypeVariable[] vars, TypeDefinition[] params) {
		if (superType instanceof ParameterizedType)
			return getTypeParameters((ParameterizedType)superType, finalType, vars, params);
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static boolean getTypeParameters(
		ParameterizedType superType, TypeDefinition finalType, TypeVariable[] vars, TypeDefinition[] params
	) {
		Type[] args = superType.getActualTypeArguments();
		Type raw = superType.getRawType();
		if (!(raw instanceof Class)) throw new IllegalArgumentException("Unexpected raw type " + raw);
		Class<?> superClass = (Class<?>)raw;
		if (superClass.equals(finalType.getBase())) {
			fillParameters(params, vars, args, finalType.getParameters());
			return true;
		}
		Type superSuperClass = superClass.getGenericSuperclass();
		if (superSuperClass != null) {
			TypeDefinition[] superParams = new TypeDefinition[args.length];
			if (getTypeParameters(superSuperClass, finalType, (TypeVariable[])args, superParams)) {
				fillParameters(params, vars, args, Arrays.asList(superParams));
				return true;
			}
		}
		for (Type superInt : superClass.getGenericInterfaces()) {
			TypeDefinition[] superParams = new TypeDefinition[args.length];
			if (getTypeParameters(superInt, finalType, (TypeVariable[])args, superParams)) {
				fillParameters(params, vars, args, Arrays.asList(superParams));
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	private static void fillParameters(TypeDefinition[] params, TypeVariable[] vars, Type[] args, List<TypeDefinition> fromParams) {
		for (int i = 0; i < vars.length; ++i) {
			for (int j = 0; j < args.length; ++j) {
				if (args[j] instanceof TypeVariable && ((TypeVariable)args[j]).getName().equals(vars[i].getName())) {
					params[i] = fromParams.get(j);
					break;
				}
			}
		}
	}
	
	private Class<?> base;
	private List<TypeDefinition> parameters;
	
	public Class<?> getBase() {
		return base;
	}
	
	/** Return the generic parameters. */
	public List<TypeDefinition> getParameters() {
		return parameters;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeDefinition)) return false;
		TypeDefinition o = (TypeDefinition)obj;
		if (!o.base.equals(base))
			return false;
		return CollectionsUtil.equals(parameters, o.parameters);
	}
	
	@Override
	public int hashCode() {
		return base.hashCode();
	}
	
}

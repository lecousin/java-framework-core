package net.lecousin.framework.io.serialization;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.lecousin.framework.collections.CollectionsUtil;

public class TypeDefinition {
	
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
		} else if (type instanceof WildcardType) {
			base = Object.class;
			parameters = new ArrayList<>(0);
		} else
			throw new IllegalArgumentException("Unexpected type " + type.getClass() + ": " + type.toString());
	}
	
	public TypeDefinition(Class<?> base, TypeDefinition... parameters) {
		this.base = base;
		this.parameters = Arrays.asList(parameters);
	}

	public TypeDefinition(Class<?> base, List<TypeDefinition> parameters) {
		this.base = base;
		this.parameters = new ArrayList<>(parameters);
	}
	
	@SuppressWarnings("rawtypes")
	public static TypeDefinition from(Class<?> instanceType, TypeDefinition definition) {
		if (instanceType.equals(definition.getBase()))
			return definition;
		TypeVariable[] types = instanceType.getTypeParameters();
		if (types.length == 0)
			return new TypeDefinition(instanceType);
		TypeDefinition[] params = new TypeDefinition[types.length];
		Type superClass = instanceType.getGenericSuperclass();
		if (superClass != null)
			if (getParameters(superClass, definition, types, params))
				return new TypeDefinition(instanceType, params);
		for (Type superInt : instanceType.getGenericInterfaces())
			if (getParameters(superInt, definition, types, params))
				return new TypeDefinition(instanceType, params);
		return new TypeDefinition(instanceType);
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean getParameters(Type superType, TypeDefinition finalType, TypeVariable[] vars, TypeDefinition[] params) {
		if (superType instanceof ParameterizedType) {
			Type[] args = ((ParameterizedType)superType).getActualTypeArguments();
			Type raw = ((ParameterizedType)superType).getRawType();
			if (!(raw instanceof Class)) throw new IllegalArgumentException("Unexpected raw type " + raw);
			Class<?> superClass = (Class<?>)raw;
			if (superClass.equals(finalType.getBase())) {
				for (int i = 0; i < vars.length; ++i) {
					for (int j = 0; j < args.length; ++j) {
						if (args[j] instanceof TypeVariable && ((TypeVariable)args[j]).getName().equals(vars[i].getName())) {
							params[i] = finalType.getParameters().get(j);
							break;
						}
					}
				}
				return true;
			}
			Type superSuperClass = superClass.getGenericSuperclass();
			if (superSuperClass != null) {
				TypeDefinition[] superParams = new TypeDefinition[args.length];
				if (getParameters(superSuperClass, finalType, (TypeVariable[])args, superParams)) {
					for (int i = 0; i < vars.length; ++i) {
						for (int j = 0; j < args.length; ++j) {
							if (args[j] instanceof TypeVariable && ((TypeVariable)args[j]).getName().equals(vars[i].getName())) {
								params[i] = superParams[j];
								break;
							}
						}
					}
					return true;
				}
			}
			for (Type superInt : superClass.getGenericInterfaces()) {
				TypeDefinition[] superParams = new TypeDefinition[args.length];
				if (getParameters(superInt, finalType, (TypeVariable[])args, superParams)) {
					for (int i = 0; i < vars.length; ++i) {
						for (int j = 0; j < args.length; ++j) {
							if (args[j] instanceof TypeVariable && ((TypeVariable)args[j]).getName().equals(vars[i].getName())) {
								params[i] = superParams[j];
								break;
							}
						}
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private Class<?> base;
	private List<TypeDefinition> parameters;
	
	public Class<?> getBase() {
		return base;
	}
	
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

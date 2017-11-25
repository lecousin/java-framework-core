package net.lecousin.framework.io.serialization;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.collections.CollectionsUtil;

public class TypeDefinition {
	
	public TypeDefinition(Type type) {
		if (type instanceof Class) {
			base = (Class<?>)type;
			parameters = new ArrayList<>(0);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)type;
			base = (Class<?>)pt.getRawType();
			Type[] params = pt.getActualTypeArguments();
			parameters = new ArrayList<>(params.length);
			for (Type p : params)
				parameters.add(new TypeDefinition(p));
		} else
			throw new IllegalArgumentException("Unexpected type " + type.getClass() + ": " + type.toString());
	}

	private Class<?> base;
	private List<TypeDefinition> parameters;
	
	public Class<?> getBase() {
		return base;
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

package net.lecousin.framework.io.serialization.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.util.ClassUtil;

public class MergeTypeAttributes implements SerializationRule {

	public MergeTypeAttributes(Class<?> type, String targetAttributeName) {
		this.type = type;
		this.targetAttributeName = targetAttributeName;
	}
	
	private Class<?> type;
	private String targetAttributeName;
	
	@Override
	public void apply(SerializationClass type, SerializationContext context, boolean serializing) throws Exception {
		if (context instanceof ObjectContext) {
			ObjectContext octx = (ObjectContext)context;
			if (octx.getParent() instanceof AttributeContext) {
				AttributeContext actx = (AttributeContext)octx.getParent();
				if (actx.getAttribute() instanceof MergeTargetAttribute) {
					// we are on the target attribute
					MergeTargetAttribute ma = (MergeTargetAttribute)actx.getAttribute();
					if (serializing) {
						// we need to change the instance
						Object containerObject = octx.getInstance();
						Object targetInstance;
						Method getter = ClassUtil.getGetter(containerObject.getClass(), targetAttributeName);
						if (getter != null)
							targetInstance = getter.invoke(containerObject);
						else {
							Field f = ClassUtil.getField(containerObject.getClass(), targetAttributeName);
							if (f != null)
								targetInstance = f.get(containerObject);
							else
								throw new Exception("Cannot find attribute " + targetAttributeName + " on class " + containerObject.getClass().getName());
						}
						octx.setInstance(targetInstance);
						// we need to change the type
						SerializationClass containerClass = octx.getSerializationClass();
						SerializationClass sc = new SerializationClass(TypeDefinition.from(targetInstance.getClass(), ma.targetType));
						octx.setSerializationClass(sc);
						octx.setOriginalType(ma.targetType);
						// we need to add other attributes
						for (Attribute a : containerClass.getAttributes()) {
							if (a.getOriginalName().equals(targetAttributeName)) continue;
							sc.getAttributes().add(new MergedAttribute(a, sc, containerObject));
						}
					} else {
						// we need to instantiate the target attribute
						Object mergedInstance = SerializationClass.instantiate(ma.targetType.getBase());
						// we can set the targetAttribute
						Method setter = ClassUtil.getSetter(this.type, targetAttributeName);
						if (setter != null)
							setter.invoke(octx.getInstance(), mergedInstance);
						else {
							Field f = ClassUtil.getField(this.type, targetAttributeName);
							if (f != null)
								f.set(octx.getInstance(), mergedInstance);
							else
								throw new Exception("Cannot find attribute " + targetAttributeName + " on class " + this.type.getName());
						}
						// we skip the targetAttribute
						for (Iterator<Attribute> it = octx.getSerializationClass().getAttributes().iterator(); it.hasNext(); ) {
							if (it.next().getOriginalName().equals(targetAttributeName)) {
								it.remove();
								break;
							}
						}
						// we need to add other attributes
						SerializationClass sc = new SerializationClass(ma.targetType);
						for (Attribute a : sc.getAttributes()) {
							octx.getSerializationClass().getAttributes().add(new MergedAttribute(a, octx.getSerializationClass(), mergedInstance));
						}
					}
				}
			}
		}

		// search for attributes that need to be merged
		for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
			Attribute a = it.next();
			if (!a.getOriginalType().getBase().equals(this.type)) continue;
			TypeDefinition t = SerializationClass.searchAttributeType(a.getOriginalType(), targetAttributeName);
			if (t == null) continue;
			it.set(new MergeTargetAttribute(a, t));
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof MergeTypeAttributes)) return false;
		MergeTypeAttributes r = (MergeTypeAttributes)rule;
		return r.type.equals(type) && r.targetAttributeName.equals(targetAttributeName);
	}
	
	private class MergeTargetAttribute extends Attribute {
		public MergeTargetAttribute(Attribute originalAttribute, TypeDefinition targetType) {
			super(originalAttribute);
			this.targetType = targetType;
		}
		private TypeDefinition targetType;
	}
	
	private static class MergedAttribute extends Attribute {
		public MergedAttribute(Attribute original, SerializationClass newParent, Object containerInstance) {
			super(original);
			this.parent = newParent;
			this.containerInstance = containerInstance;
		}
		private Object containerInstance;
		
		@Override
		public Object getValue(Object instance) throws Exception {
			return super.getValue(containerInstance);
		}
		
		@Override
		public void setValue(Object instance, Object value) throws Exception {
			super.setValue(containerInstance, value);
		}
	}
	
}

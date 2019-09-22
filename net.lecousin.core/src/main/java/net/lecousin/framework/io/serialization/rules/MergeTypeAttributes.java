package net.lecousin.framework.io.serialization.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationContextPattern;
import net.lecousin.framework.io.serialization.SerializationException;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.util.ClassUtil;

/** Merge attributes of a class into the class of a specified attribute. */
public class MergeTypeAttributes implements SerializationRule {

	/** Constructor. */
	public MergeTypeAttributes(SerializationContextPattern contextPattern, Class<?> type, String targetAttributeName) {
		this.contextPattern = contextPattern;
		this.type = type;
		this.targetAttributeName = targetAttributeName;
	}
	
	private SerializationContextPattern contextPattern;
	private Class<?> type;
	private String targetAttributeName;
	
	@Override
	public boolean apply(
		SerializationClass type, SerializationContext context, List<SerializationRule> rules, boolean serializing
	) throws SerializationException {
		try {
			if (context instanceof ObjectContext) {
				ObjectContext octx = (ObjectContext)context;
				if (octx.getParent() instanceof AttributeContext) {
					AttributeContext actx = (AttributeContext)octx.getParent();
					applyOnAttributeContext(octx, actx, serializing);
				} else if (this.type.isAssignableFrom(octx.getSerializationClass().getType().getBase())) {
					if (octx.getParent() instanceof CollectionContext) {
						CollectionContext cctx = (CollectionContext)octx.getParent();
						while (cctx.getParent() instanceof CollectionContext)
							cctx = (CollectionContext)cctx.getParent();
						if (cctx.getParent() instanceof AttributeContext) {
							AttributeContext actx = (AttributeContext)cctx.getParent();
							if (contextPattern.matches(actx)) {
								// eligible to merge
								applyOnCollectionContextInAttribute(octx, serializing);
							}
						}
					}
				} else if (!serializing && this.type.isAssignableFrom(octx.getOriginalType().getBase())) {
					// we may deserialize an abstract target type which has been instantiated
					TypeDefinition targetType =
						SerializationClass.searchAttributeType(octx.getOriginalType(), targetAttributeName);
					if (targetType != null) {
						Object mergedInstance = octx.getInstance();
						// we need to instantiate the original type
						Object containerInstance = SerializationClass.instantiate(octx.getOriginalType().getBase());
						// we can set the targetAttribute
						Method setter = ClassUtil.getSetter(this.type, targetAttributeName);
						if (setter != null)
							setter.invoke(containerInstance, mergedInstance);
						else {
							Field f = ClassUtil.getField(this.type, targetAttributeName);
							if (f != null)
								f.set(containerInstance, mergedInstance);
							else
								throw new SerializationException("Cannot find attribute " + targetAttributeName
									+ " on class " + this.type.getName());
						}
						// we need to change the instance
						octx.setInstance(containerInstance);
						// we change the class
						SerializationClass containerClass = new SerializationClass(octx.getOriginalType());
						SerializationClass targetClass = octx.getSerializationClass();
						octx.setSerializationClass(containerClass);
						// we skip the targetAttribute
						for (Iterator<Attribute> it = containerClass.getAttributes().iterator(); it.hasNext(); ) {
							if (it.next().getOriginalName().equals(targetAttributeName)) {
								it.remove();
								break;
							}
						}
						// we need to apply other rules to have final list of attributes
						List<SerializationRule> subRules = rules.subList(rules.indexOf(this) + 1, rules.size());
						targetClass.apply(subRules, context, serializing);
						// we need to add other attributes
						for (Attribute a : targetClass.getAttributes()) {
							containerClass.getAttributes().add(new MergedAttribute(a, containerClass, mergedInstance));
						}
						// we need to apply rules on the new type
						List<SerializationRule> newRules =
							TypeAnnotationToRule.addRules(containerClass.getType().getBase(), rules);
						newRules = AttributeAnnotationToRuleOnType.addRules(containerClass, false, newRules);
						newRules.remove(this);
						containerClass.apply(newRules, context, serializing);
						return true;
					}
				}
			}
	
			if (!contextPattern.matches(type, context))
				return false;
			// search for attributes that need to be merged
			for (ListIterator<Attribute> it = type.getAttributes().listIterator(); it.hasNext(); ) {
				Attribute a = it.next();
				if (!a.getOriginalType().getBase().equals(this.type)) continue;
				TypeDefinition t = SerializationClass.searchAttributeType(a.getOriginalType(), targetAttributeName);
				if (t == null) continue;
				it.set(new MergeTargetAttribute(a, t));
			}
			return false;
		} catch (SerializationException e) {
			throw e;
		} catch (Exception e) {
			throw new SerializationException("Error merging attributes", e);
		}
	}
	
	private void applyOnAttributeContext(ObjectContext octx, AttributeContext actx, boolean serializing)
	throws SerializationException, ReflectiveOperationException {
		if (actx.getAttribute() instanceof MergeTargetAttribute) {
			// we are on the target attribute
			MergeTargetAttribute ma = (MergeTargetAttribute)actx.getAttribute();
			if (serializing) {
				// we need to change the instance
				Object containerObject = octx.getInstance();
				TypeDefinition targetType;
				if (containerObject != null) {
					Object targetInstance;
					Method getter = ClassUtil.getGetter(containerObject.getClass(), targetAttributeName);
					if (getter != null)
						targetInstance = getter.invoke(containerObject);
					else {
						Field f = ClassUtil.getField(containerObject.getClass(), targetAttributeName);
						if (f != null)
							targetInstance = f.get(containerObject);
						else
							throw new SerializationException("Cannot find attribute " + targetAttributeName
								+ " on class " + containerObject.getClass().getName());
					}
					octx.setInstance(targetInstance);
					targetType = TypeDefinition.from(targetInstance.getClass(), ma.targetType);
				} else {
					Attribute a = octx.getSerializationClass().getAttributeByOriginalName(targetAttributeName);
					targetType = a.getOriginalType();
				}
				// we need to change the type
				SerializationClass containerClass = octx.getSerializationClass();
				SerializationClass sc = new SerializationClass(targetType);
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
						throw new SerializationException("Cannot find attribute " + targetAttributeName
							+ " on class " + this.type.getName());
				}
				// we skip the targetAttribute
				for (Iterator<Attribute> it = octx.getSerializationClass().getAttributes().iterator();
					it.hasNext(); ) {
					if (it.next().getOriginalName().equals(targetAttributeName)) {
						it.remove();
						break;
					}
				}
				// we need to add other attributes
				SerializationClass sc = new SerializationClass(ma.targetType);
				for (Attribute a : sc.getAttributes()) {
					octx.getSerializationClass().getAttributes().add(
						new MergedAttribute(a, octx.getSerializationClass(), mergedInstance));
				}
			}
		}
	}
	
	private void applyOnCollectionContextInAttribute(ObjectContext octx, boolean serializing)
	throws SerializationException, ReflectiveOperationException {
		TypeDefinition targetType =
			SerializationClass.searchAttributeType(octx.getOriginalType(), targetAttributeName);
		if (targetType != null) {
			if (serializing) {
				// we need to change the instance
				Object containerObject = octx.getInstance();
				TypeDefinition newType;
				if (containerObject != null) {
					Object targetInstance;
					Method getter =
					ClassUtil.getGetter(containerObject.getClass(), targetAttributeName);
					if (getter != null)
						targetInstance = getter.invoke(containerObject);
					else {
						Field f = ClassUtil.getField(
							containerObject.getClass(), targetAttributeName);
						if (f != null)
							targetInstance = f.get(containerObject);
						else
							throw new SerializationException("Cannot find attribute "
								+ targetAttributeName
								+ " on class "
								+ containerObject.getClass().getName());
					}
					octx.setInstance(targetInstance);
					newType = TypeDefinition.from(targetInstance.getClass(), targetType);
				} else {
					Attribute a = octx.getSerializationClass()
						.getAttributeByOriginalName(targetAttributeName);
					newType = a.getOriginalType();
				}
				// we need to change the type
				SerializationClass containerClass = octx.getSerializationClass();
				SerializationClass sc = new SerializationClass(newType);
				octx.setSerializationClass(sc);
				octx.setOriginalType(targetType);
				// we need to add other attributes
				for (Attribute a : containerClass.getAttributes()) {
					if (a.getOriginalName().equals(targetAttributeName)) continue;
					sc.getAttributes().add(new MergedAttribute(a, sc, containerObject));
				}
			} else {
				// we need to instantiate the target attribute
				Object mergedInstance = SerializationClass.instantiate(targetType.getBase());
				// we can set the targetAttribute
				Method setter = ClassUtil.getSetter(this.type, targetAttributeName);
				if (setter != null)
					setter.invoke(octx.getInstance(), mergedInstance);
				else {
					Field f = ClassUtil.getField(this.type, targetAttributeName);
					if (f != null)
						f.set(octx.getInstance(), mergedInstance);
					else
						throw new SerializationException("Cannot find attribute "
							+ targetAttributeName
							+ " on class " + this.type.getName());
				}
				// we skip the targetAttribute
				for (Iterator<Attribute> it =
					octx.getSerializationClass().getAttributes().iterator();
					it.hasNext(); ) {
					if (it.next().getOriginalName().equals(targetAttributeName)) {
						it.remove();
						break;
					}
				}
				// we need to add other attributes
				SerializationClass sc = new SerializationClass(targetType);
				for (Attribute a : sc.getAttributes()) {
					octx.getSerializationClass().getAttributes().add(
						new MergedAttribute(a, octx.getSerializationClass(),
							mergedInstance));
				}
			}
		}
	}
	
	@Override
	public boolean isEquivalent(SerializationRule rule) {
		if (!(rule instanceof MergeTypeAttributes)) return false;
		MergeTypeAttributes r = (MergeTypeAttributes)rule;
		return r.type.equals(type) && r.targetAttributeName.equals(targetAttributeName);
	}
	
	private static class MergeTargetAttribute extends Attribute {
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
		public Object getValue(Object instance) throws SerializationException {
			return super.getValue(containerInstance);
		}
		
		@Override
		public void setValue(Object instance, Object value) throws SerializationException {
			super.setValue(containerInstance, value);
		}
	}
	
}

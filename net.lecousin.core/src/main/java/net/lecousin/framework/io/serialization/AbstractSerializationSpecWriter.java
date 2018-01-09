package net.lecousin.framework.io.serialization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.CollectionContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
import net.lecousin.framework.io.serialization.SerializationUtil.MapEntry;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnAttribute;
import net.lecousin.framework.io.serialization.annotations.AttributeAnnotationToRuleOnType;
import net.lecousin.framework.io.serialization.annotations.TypeAnnotationToRule;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Generate serialization specification. */
public abstract class AbstractSerializationSpecWriter implements SerializationSpecWriter {

	protected byte priority;
	
	protected abstract ISynchronizationPoint<? extends Exception> initializeSpecWriter(IO.Writable output);
	
	protected abstract ISynchronizationPoint<? extends Exception> finalizeSpecWriter();
	
	/** Utility to create tasks. */
	protected class SpecTask extends Task.Cpu<Void, NoException> {
		public SpecTask(Runnable r) {
			super("Write Specification", priority);
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() {
			r.run();
			return null;
		}
	}
	
	@Override
	public ISynchronizationPoint<Exception> writeSpecification(Class<?> type, IO.Writable output, List<SerializationRule> rules) {
		priority = output.getPriority();
		ISynchronizationPoint<? extends Exception> init = initializeSpecWriter(output);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		init.listenAsyncSP(new SpecTask(() -> {
			ISynchronizationPoint<? extends Exception> sp;
			if (type != null)
				sp = specifyValue(null, new TypeDefinition(type), rules);
			else
				sp = specifyAnyValue(null);
			sp.listenInlineSP(() -> {
				finalizeSpecWriter().listenInlineSP(result);
			}, result);
		}), result);
		return result;
	}
	
	protected List<SerializationRule> addRulesForType(SerializationClass type, List<SerializationRule> currentList) {
		currentList = TypeAnnotationToRule.addRules(type.getType().getBase(), currentList);
		currentList = AttributeAnnotationToRuleOnType.addRules(type, true, currentList);
		return currentList;
	}
	
	protected List<SerializationRule> addRulesForAttribute(Attribute a, List<SerializationRule> currentList) {
		return AttributeAnnotationToRuleOnAttribute.addRules(a, true, currentList);
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyAnyValue(SerializationContext context);
	
	public ISynchronizationPoint<? extends Exception> specifyValue(
		SerializationContext context, TypeDefinition typeDef, List<SerializationRule> rules
	) {
		for (SerializationRule rule : rules)
			typeDef = rule.getDeserializationType(typeDef, context);
		
		Class<?> type = typeDef.getBase();
		
		if (type.isArray()) {
			Class<?> elementType = type.getComponentType();
			CollectionContext ctx = new CollectionContext(context, null, typeDef, new TypeDefinition(elementType));
			return specifyCollectionValue(ctx, rules);
		}
		
		if (boolean.class.equals(type))
			return specifyBooleanValue(context, false);
		if (Boolean.class.equals(type))
			return specifyBooleanValue(context, true);
		
		if (byte.class.equals(type))
			return specifyNumericValue(context, Byte.class, false, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (Byte.class.equals(type))
			return specifyNumericValue(context, Byte.class, true, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (short.class.equals(type))
			return specifyNumericValue(context, Short.class, false, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (Short.class.equals(type))
			return specifyNumericValue(context, Short.class, true, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (int.class.equals(type))
			return specifyNumericValue(context, Integer.class, false,
				Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (Integer.class.equals(type))
			return specifyNumericValue(context, Integer.class, true,
				Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (long.class.equals(type))
			return specifyNumericValue(context, Long.class, false, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));
		if (Long.class.equals(type))
			return specifyNumericValue(context, Long.class, true, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));
		if (float.class.equals(type) ||
			double.class.equals(type) ||
			Number.class.isAssignableFrom(type))
			return specifyNumericValue(context, type, !type.isPrimitive(), null, null);

		if (char.class.equals(type))
			return specifyCharacterValue(context, false);
		if (Character.class.equals(type))
			return specifyCharacterValue(context, true);
		
		if (CharSequence.class.isAssignableFrom(type))
			return specifyStringValue(context, typeDef);
		
		if (type.isEnum())
			return specifyEnumValue(context, typeDef);

		if (Collection.class.isAssignableFrom(type)) {
			TypeDefinition elementType;
			if (typeDef.getParameters().isEmpty())
				elementType = null;
			else
				elementType = typeDef.getParameters().get(0);
			CollectionContext ctx = new CollectionContext(context, null, typeDef, elementType);
			return specifyCollectionValue(ctx, rules);
		}

		if (Map.class.isAssignableFrom(type))
			return specifyMapValue(context, typeDef, rules);
		
		if (InputStream.class.isAssignableFrom(type))
			return specifyInputStreamValue(context, rules);
		
		if (IO.Readable.class.isAssignableFrom(type))
			return specifyIOReadableValue(context, rules);
		
		return specifyObjectValue(context, typeDef, rules);
	}
	
	// *** boolean ***
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyBooleanValue(SerializationContext context, boolean nullable);

	// *** number ***

	protected abstract ISynchronizationPoint<? extends Exception> specifyNumericValue(
		SerializationContext context, Class<?> type, boolean nullable, Number min, Number max);

	// *** character ***
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyCharacterValue(SerializationContext context, boolean nullable);
	
	// *** string ***
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyStringValue(SerializationContext context, TypeDefinition type);

	// *** enum ***
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyEnumValue(SerializationContext context, TypeDefinition type);

	// *** collection ***
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyCollectionValue(
		CollectionContext context, List<SerializationRule> rules);
	
	// *** map ***

	protected ISynchronizationPoint<? extends Exception> specifyMapValue(
		SerializationContext context, TypeDefinition type, List<SerializationRule> rules
	) {
		TypeDefinition elementType = new TypeDefinition(MapEntry.class, type.getParameters());
		TypeDefinition colType = new TypeDefinition(ArrayList.class, elementType);
		CollectionContext ctx = new CollectionContext(context, null, colType, elementType);
		return specifyCollectionValue(ctx, rules);
	}
	
	// *** InputStream ***

	protected ISynchronizationPoint<? extends Exception> specifyInputStreamValue(
		SerializationContext context, List<SerializationRule> rules
	) {
		return specifyIOReadableValue(context, rules);
	}
	
	// *** IO.Readable ***

	protected abstract ISynchronizationPoint<? extends Exception> specifyIOReadableValue(
		SerializationContext context, List<SerializationRule> rules);
	
	// *** object ***
	
	protected ISynchronizationPoint<? extends Exception> specifyObjectValue(
		SerializationContext context, TypeDefinition typeDef, List<SerializationRule> rules
	) {
		ObjectContext ctx;
		try {
			Class<?> type = typeDef.getBase();
			SerializationClass sc = new SerializationClass(TypeDefinition.from(type, typeDef));
			ctx = new ObjectContext(context, null, sc, typeDef);
			rules = addRulesForType(sc, rules);
			sc.apply(rules, ctx, true);
		} catch (Exception e) {
			return new SynchronizationPoint<>(e);
		}
		return specifyTypedValue(ctx, rules);
	}

	protected abstract ISynchronizationPoint<? extends Exception> specifyTypedValue(ObjectContext context, List<SerializationRule> rules);
	
	protected ISynchronizationPoint<? extends Exception> specifyTypeContent(ObjectContext context, List<SerializationRule> rules) {
		List<Attribute> attributes = sortAttributes(context.getSerializationClass().getAttributes());
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		specifyTypeAttribute(context, attributes, 0, rules, sp);
		return sp;
	}
	
	protected List<Attribute> sortAttributes(List<Attribute> attributes) {
		return attributes;
	}
	
	protected void specifyTypeAttribute(
		ObjectContext context, List<Attribute> attributes, int index, List<SerializationRule> rules, SynchronizationPoint<Exception> sp
	) {
		if (index == attributes.size()) {
			sp.unblock();
			return;
		}
		Attribute attr = attributes.get(index);
		AttributeContext ctx = new AttributeContext(context, attr);
		ISynchronizationPoint<? extends Exception> s = specifyTypeAttribute(ctx, rules);
		if (s.isUnblocked()) {
			if (s.hasError()) sp.error(s.getError());
			else specifyTypeAttribute(context, attributes, index + 1, rules, sp);
			return;
		}
		s.listenAsyncSP(new SpecTask(() -> {
			specifyTypeAttribute(context, attributes, index + 1, rules, sp);
		}), sp);
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyTypeAttribute(AttributeContext context, List<SerializationRule> rules);
	
}

package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.SerializationContext.ObjectContext;
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
	
	protected ISynchronizationPoint<? extends Exception> specifyValue(
		SerializationContext context, TypeDefinition typeDef, List<SerializationRule> rules
	) {
		Class<?> type = typeDef.getBase();
		
		if (boolean.class.equals(type))
			return specifyBooleanValue(false);
		if (Boolean.class.equals(type))
			return specifyBooleanValue(true);
		
		if (byte.class.equals(type))
			return specifyNumericValue(Byte.class, false, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (Byte.class.equals(type))
			return specifyNumericValue(Byte.class, true, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (short.class.equals(type))
			return specifyNumericValue(Short.class, false, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (Short.class.equals(type))
			return specifyNumericValue(Short.class, true, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (int.class.equals(type))
			return specifyNumericValue(Integer.class, false, Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (Integer.class.equals(type))
			return specifyNumericValue(Integer.class, true, Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (long.class.equals(type))
			return specifyNumericValue(Long.class, false, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));
		if (Long.class.equals(type))
			return specifyNumericValue(Long.class, true, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));
		
		if (CharSequence.class.isAssignableFrom(type))
			return specifyStringValue(context, typeDef);

		// TODO
		
		return specifyObjectValue(context, typeDef, rules);
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyBooleanValue(boolean nullable);

	protected abstract ISynchronizationPoint<? extends Exception> specifyNumericValue(Class<?> type, boolean nullable, Number min, Number max);
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyStringValue(SerializationContext context, TypeDefinition type);
	
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

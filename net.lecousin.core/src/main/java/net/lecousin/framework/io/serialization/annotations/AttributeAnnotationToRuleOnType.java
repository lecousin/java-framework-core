package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.Pair;

/** Convert an annotation into a SerializationRule.
 * @param <TAnnotation> type of annotation
 */
public interface AttributeAnnotationToRuleOnType<TAnnotation extends Annotation> {

	/** Create a rule from an annotation. */
	SerializationRule createRule(TAnnotation annotation, Attribute attribute);
	
	/** Search for annotations on the given type, and try to convert them into
	 * serialization rules.
	 */
	@SuppressWarnings({ "rawtypes" })
	static List<SerializationRule> addRules(SerializationClass type, boolean onGet, List<SerializationRule> rules) {
		List<SerializationRule> newRules = new LinkedList<>();
		for (Attribute attr : type.getAttributes()) {
			if (onGet && !attr.canGet()) continue;
			if (!onGet && !attr.canSet()) continue;
			for (Annotation a : ClassUtil.expandRepeatableAnnotations(attr.getAnnotations(onGet))) {
				for (AttributeAnnotationToRuleOnType toRule : getAnnotationToRules(a))
					addRuleFromAttribute(attr, a, toRule, rules, newRules);
			}
		}
		if (newRules.isEmpty())
			return rules;
		ArrayList<SerializationRule> newList = new ArrayList<>(rules.size() + newRules.size());
		newList.addAll(rules);
		newList.addAll(newRules);
		return newList;
	}
	
	/** Create a rule from annotation and add it to the new rules. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void addRuleFromAttribute(
		Attribute attr, Annotation a, AttributeAnnotationToRuleOnType toRule, List<SerializationRule> rules, List<SerializationRule> newRules
	) {
		SerializationRule rule;
		try { rule = toRule.createRule(a, attr); }
		catch (Exception t) {
			LCCore.getApplication().getDefaultLogger().error(
				"Error creating rule from annotation " + a.annotationType().getName()
				+ " on attribute " + attr.getOriginalName() + " of type "
				+ attr.getOriginalType().getBase().getName(), t);
			return;
		}
		SerializationRule.addRuleIfNoEquivalent(rule, newRules, rules);
	}
	
	/** Search for implementations to convert the given annotation into a rule.
	 * It looks first on the annotation class if there is an inner class implementing AttributeAnnotationToRuleOnAttribute.
	 * If none is found, it looks into the registry.
	 */
	static List<AttributeAnnotationToRuleOnType<?>> getAnnotationToRules(Annotation a) {
		LinkedList<AttributeAnnotationToRuleOnType<?>> list = new LinkedList<>();
		for (Class<?> c : a.annotationType().getDeclaredClasses()) {
			if (!AttributeAnnotationToRuleOnType.class.isAssignableFrom(c)) continue;
			try { list.add((AttributeAnnotationToRuleOnType<?>)c.newInstance()); }
			catch (Exception t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating AttributeAnnotationToRule " + a.annotationType().getName(), t);
			}
		}
		for (Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnType<?>> p : Registry.registry)
			if (p.getValue1().equals(a.annotationType()))
				list.add(p.getValue2());
		return list;
	}
	
	/** Registry of converters between annotations and serialization rules. */
	public static final class Registry {
		
		private Registry() {
			// no instance
		}

		private static List<Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnType<?>>> registry = new ArrayList<>();

		/** Register a converter. */
		public static <T extends Annotation> void register(Class<T> annotationType, AttributeAnnotationToRuleOnType<T> toRule) {
			registry.add(new Pair<>(annotationType, toRule));
		}
	}
	
}

package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Pair;

/** Convert an annotation on an attribute into a SerializationRule on this attribute.
 * @param <TAnnotation> type of annotation
 */
public interface AttributeAnnotationToRuleOnAttribute<TAnnotation extends Annotation> {

	/** Create a rule from an annotation. */
	SerializationRule createRule(TAnnotation annotation, Attribute attribute);
	
	/** Search for annotations on the given attributes, and try to convert them into
	 * serialization rules.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<SerializationRule> addRules(Attribute attr, boolean onGet, List<SerializationRule> rules) {
		List<SerializationRule> newRules = new LinkedList<>();
		for (Annotation a : attr.getAnnotations(onGet)) {
			for (AttributeAnnotationToRuleOnAttribute toRule : getAnnotationToRules(a)) {
				SerializationRule rule;
				try { rule = toRule.createRule(a, attr); }
				catch (Throwable t) {
					LCCore.getApplication().getDefaultLogger().error(
						"Error creating rule from annotation " + a.annotationType().getName()
						+ " on attribute " + attr.getOriginalName() + " of type "
						+ attr.getOriginalType().getBase().getName(), t);
					continue;
				}
				if (rule != null) {
					boolean found = false;
					for (SerializationRule r : rules)
						if (r.isEquivalent(rule)) {
							found = true;
							break;
						}
					if (!found)
						for (SerializationRule r : newRules)
							if (r.isEquivalent(rule)) {
								found = true;
								break;
							}
					if (!found)
						newRules.add(rule);
				}
			}
		}
		if (newRules.isEmpty())
			return rules;
		ArrayList<SerializationRule> newList = new ArrayList<>(rules.size() + newRules.size());
		newList.addAll(rules);
		newList.addAll(newRules);
		return newList;
	}
	
	/** Search for implementations to convert the given annotation into a rule.
	 * It looks first on the annotation class if there is an inner class implementing AttributeAnnotationToRuleOnAttribute.
	 * If none is found, it looks into the registry.
	 */
	public static List<AttributeAnnotationToRuleOnAttribute<?>> getAnnotationToRules(Annotation a) {
		LinkedList<AttributeAnnotationToRuleOnAttribute<?>> list = new LinkedList<>();
		for (Class<?> c : a.annotationType().getDeclaredClasses()) {
			if (!AttributeAnnotationToRuleOnAttribute.class.isAssignableFrom(c)) continue;
			try { list.add((AttributeAnnotationToRuleOnAttribute<?>)c.newInstance()); }
			catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating AttributeAnnotationToRule " + a.annotationType().getName(), t);
				continue;
			}
		}
		for (Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnAttribute<?>> p : Registry.registry)
			if (p.getValue1().equals(a.annotationType()))
				list.add(p.getValue2());
		return list;
	}
	
	/** Registry of converters between annotations and serialization rules. */
	public static class Registry {

		private static List<Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnAttribute<?>>> registry = new ArrayList<>();

		/** Register a converter. */
		public static <T extends Annotation> void register(Class<T> annotationType, AttributeAnnotationToRuleOnAttribute<T> toRule) {
			registry.add(new Pair<>(annotationType, toRule));
		}
	}
	
}

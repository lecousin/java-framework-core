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
public interface TypeAnnotationToRule<TAnnotation extends Annotation> {

	/** Create a rule from an annotation. */
	SerializationRule createRule(TAnnotation annotation, Class<?> type);

	/** Search for annotations on the given type, and try to convert them into
	 * serialization rules.
	 */
	public static List<SerializationRule> addRules(SerializationClass type, List<SerializationRule> rules) {
		rules = addRules(type.getType().getBase(), rules);
		for (Attribute a : type.getAttributes())
			rules = addRules(a.getType().getBase(), rules);
		return rules;
	}
	
	/** Search for annotations on the given type, and try to convert them into
	 * serialization rules.
	 */
	public static List<SerializationRule> addRules(Class<?> clazz, List<SerializationRule> rules) {
		List<SerializationRule> newRules = new LinkedList<>();
		processAnnotations(clazz, newRules, rules);
		if (newRules.isEmpty())
			return rules;
		ArrayList<SerializationRule> newList = new ArrayList<>(rules.size() + newRules.size());
		newList.addAll(rules);
		newList.addAll(newRules);
		return newList;
	}
	
	/** Convert annotations into rules. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void processAnnotations(Class<?> clazz, List<SerializationRule> newRules, List<SerializationRule> rules) {
		for (Annotation a : ClassUtil.expandRepeatableAnnotations(clazz.getDeclaredAnnotations())) {
			for (TypeAnnotationToRule toRule : getAnnotationToRules(a)) {
				SerializationRule rule;
				try { rule = toRule.createRule(a, clazz); }
				catch (Throwable t) {
					LCCore.getApplication().getDefaultLogger().error(
						"Error creating rule from annotation " + a.annotationType().getName()
						+ " using " + toRule.getClass().getName(), t);
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
		if (clazz.getSuperclass() != null)
			processAnnotations(clazz.getSuperclass(), newRules, rules);
		for (Class<?> i : clazz.getInterfaces())
			processAnnotations(i, newRules, rules);
	}
	
	/** Search for implementations to convert the given annotation into a rule.
	 * It looks first on the annotation class if there is an inner class implementing AttributeAnnotationToRuleOnAttribute.
	 * If none is found, it looks into the registry.
	 */
	public static List<TypeAnnotationToRule<?>> getAnnotationToRules(Annotation a) {
		LinkedList<TypeAnnotationToRule<?>> list = new LinkedList<>();
		for (Class<?> c : a.annotationType().getDeclaredClasses()) {
			if (!TypeAnnotationToRule.class.isAssignableFrom(c)) continue;
			try { list.add((TypeAnnotationToRule<?>)c.newInstance()); }
			catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating TypeAnnotationToRule " + a.annotationType().getName(), t);
				continue;
			}
		}
		for (Pair<Class<? extends Annotation>, TypeAnnotationToRule<?>> p : Registry.registry)
			if (p.getValue1().equals(a.annotationType()))
				list.add(p.getValue2());
		return list;
	}
	
	/** Registry of converters between annotations and serialization rules. */
	public static class Registry {

		private static List<Pair<Class<? extends Annotation>, TypeAnnotationToRule<?>>> registry = new ArrayList<>();

		/** Register a converter. */
		public static <T extends Annotation> void register(Class<T> annotationType, TypeAnnotationToRule<T> toRule) {
			registry.add(new Pair<>(annotationType, toRule));
		}
	}
	
}

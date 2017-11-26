package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Pair;

public interface TypeAnnotationToRule<TAnnotation extends Annotation> {

	SerializationRule createRule(TAnnotation annotation, Class<?> type);
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<SerializationRule> addRules(Class<?> clazz, List<SerializationRule> rules) {
		List<SerializationRule> newRules = new LinkedList<>();
		for (Annotation a : clazz.getAnnotations()) {
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
		if (newRules.isEmpty())
			return rules;
		ArrayList<SerializationRule> newList = new ArrayList<>(rules.size() + newRules.size());
		newList.addAll(rules);
		newList.addAll(newRules);
		return newList;
	}
	
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
	
	public static class Registry {

		private static List<Pair<Class<? extends Annotation>, TypeAnnotationToRule<?>>> registry = new ArrayList<>();

		public static <T extends Annotation> void register(Class<T> annotationType, TypeAnnotationToRule<T> toRule) {
			registry.add(new Pair<>(annotationType, toRule));
		}
	}
	
}

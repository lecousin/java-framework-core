package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Pair;

public interface AttributeAnnotationToRuleOnType<TAnnotation extends Annotation> {

	SerializationRule createRule(TAnnotation annotation, Attribute attribute);
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<SerializationRule> addRules(SerializationClass type, boolean onGet, List<SerializationRule> rules) {
		List<SerializationRule> newRules = new LinkedList<>();
		for (Attribute attr : type.getAttributes()) {
			if (onGet && !attr.canGet()) continue;
			if (!onGet && !attr.canSet()) continue;
			for (Annotation a : attr.getAnnotations(onGet)) {
				for (AttributeAnnotationToRuleOnType toRule : getAnnotationToRules(a)) {
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
		}
		if (newRules.isEmpty())
			return rules;
		ArrayList<SerializationRule> newList = new ArrayList<>(rules.size() + newRules.size());
		newList.addAll(rules);
		newList.addAll(newRules);
		return newList;
	}
	
	public static List<AttributeAnnotationToRuleOnType<?>> getAnnotationToRules(Annotation a) {
		LinkedList<AttributeAnnotationToRuleOnType<?>> list = new LinkedList<>();
		for (Class<?> c : a.annotationType().getDeclaredClasses()) {
			if (!AttributeAnnotationToRuleOnType.class.isAssignableFrom(c)) continue;
			try { list.add((AttributeAnnotationToRuleOnType<?>)c.newInstance()); }
			catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating AttributeAnnotationToRule " + a.annotationType().getName(), t);
				continue;
			}
		}
		for (Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnType<?>> p : Registry.registry)
			if (p.getValue1().equals(a.annotationType()))
				list.add(p.getValue2());
		return list;
	}
	
	public static class Registry {

		private static List<Pair<Class<? extends Annotation>, AttributeAnnotationToRuleOnType<?>>> registry = new ArrayList<>();

		public static <T extends Annotation> void register(Class<T> annotationType, AttributeAnnotationToRuleOnType<T> toRule) {
			registry.add(new Pair<>(annotationType, toRule));
		}
	}
	
}

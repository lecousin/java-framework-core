package net.lecousin.framework.text.pattern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class StringPattern<T> {
	
	public interface Section<T> {
		void append(StringBuilder s, T object);
	}
	
	public static class StringSection<T> implements Section<T> {
		private String string;
		
		public StringSection(String string) {
			this.string = string;
		}
		
		@Override
		public void append(StringBuilder s, T object) {
			s.append(string);
		}
	}
	
	public static class VariableSection<T> implements Section<T> {
		private Function<T, String> variable;
		private List<StringPatternFilter.Filter> filters;
		
		public VariableSection(Function<T, String> variable, List<StringPatternFilter.Filter> filters) {
			this.variable = variable;
			this.filters = filters;
		}
		
		public Function<T, String> getVariable() {
			return variable;
		}
		
		@Override
		public void append(StringBuilder s, T object) {
			String value = variable.apply(object);
			if (filters != null)
				for (StringPatternFilter.Filter filter : filters)
					value = filter.filter(value);
			s.append(value);
		}
	}

	private List<Section<T>> sections;
	
	public StringPattern(List<Section<T>> sections) {
		concatenateStrings(sections);
		this.sections = new ArrayList<>(sections);
	}
	
	public List<Section<T>> getSections() {
		return sections;
	}
	
	public void append(StringBuilder s, T object) {
		for (Section<T> section : sections)
			section.append(s, object);
	}
	
	@SuppressWarnings("java:S1643")
	private static <T> void concatenateStrings(List<Section<T>> sections) {
		StringSection<T> previous = null;
		for (Iterator<Section<T>> it = sections.iterator(); it.hasNext(); ) {
			Section<T> s = it.next();
			if (s instanceof StringSection) {
				if (previous == null) {
					previous = (StringSection<T>)s;
				} else {
					previous.string += ((StringSection<T>)s).string;
					it.remove();
				}
			} else {
				previous = null;
			}
		}
	}

}

package net.lecousin.framework.text.pattern;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Build strings from an object of type T, based on a pattern.
 * 
 * <p>
 * A pattern is a string containing variables, with each variable being declared as follow:
 * <code>%{varname[parameters]|filters}</code><br/>
 * <ul>
 * <li>varname identifies a value to take from the object of type T</li>
 * <li>parameters are optional. The format is a list of key/value separated by a semicolon.<br/>
 * 		For example: param1=value1;param2=value2<br/>
 * 		In case a single parameter is allowed, only the value can be specified.
 * </li>
 * <li>filters are optional. The format is a list of key/value separated by a pipe.<br/>
 * </li>
 * </ul>
 * Characters '{', '}', '[', ']', '|', ';' and '=' are reserved and must be escaped if contained in a value or name.
 * </p>
 * 
 * @param <T> type from which the string is built
 */
public class StringPatternBuilder<T> {

	private static final String VAR_START = "%{";
	
	private List<StringPatternVariable<T>> variables;
	private List<StringPatternFilter> filters;
	
	public StringPatternBuilder(List<StringPatternVariable<T>> variables, List<StringPatternFilter> filters) {
		this.variables = variables;
		this.filters = filters;
	}
	
	public StringPattern<T> build(String pattern) {
		List<StringPattern.Section<T>> sections = new LinkedList<>();
		int pos = 0;
		do {
			int i = pattern.indexOf(VAR_START, pos);
			if (i < 0) {
				if (pos < pattern.length())
					sections.add(new StringPattern.StringSection<T>(pattern.substring(pos)));
				break;
			}
			if (i > pos) {
				sections.add(new StringPattern.StringSection<T>(pattern.substring(pos, i)));
			}
			pos = parseVariable(pattern, i + 2, sections);
		} while (true);
		return new StringPattern<>(sections);
	}
	
	private int parseVariable(String pattern, int pos, List<StringPattern.Section<T>> sections) {
		StringBuilder name = new StringBuilder();
		Map<String, String> params = new HashMap<>();
		List<StringPatternFilter.Filter> varFilters = null;
		while (pos < pattern.length()) {
			char c = pattern.charAt(pos++);
			if (c == '\\') {
				if (pos < pattern.length()) {
					c = pattern.charAt(pos++);
					name.append(c);
					continue;
				}
				break;
			}
			if (c == '}' || c == '[' || c == '|') {
				String varname = name.toString();
				StringPatternVariable<T> var = getVariable(varname);
				if (var == null) {
					sections.add(new StringPattern.StringSection<T>(VAR_START + varname + c));
					return pos;
				}
				if (c == '}') {
					sections.add(new StringPattern.VariableSection<T>(var.build(params), null));
					return pos;
				}
				if (c == '[') {
					pos = parseParameters(pattern, pos, params, ';', ']');
					if (pos < pattern.length() && pattern.charAt(pos) == '|') {
						Map<String, String> map = new HashMap<>();
						pos = parseParameters(pattern, pos + 1, map, '|', '}');
						for (Map.Entry<String, String> e : map.entrySet()) {
							StringPatternFilter filter = getFilter(e.getKey());
							if (filter != null) {
								if (varFilters == null)
									varFilters = new LinkedList<>();
								varFilters.add(filter.build(e.getValue()));
							}
						}
					} else {
						pos++;
					}
					sections.add(new StringPattern.VariableSection<T>(var.build(params), varFilters));
					return pos;
				}
				if (c == '|') {
					Map<String, String> map = new HashMap<>();
					pos = parseParameters(pattern, pos, map, '|', '}');
					for (Map.Entry<String, String> e : map.entrySet()) {
						StringPatternFilter filter = getFilter(e.getKey());
						if (filter != null) {
							if (varFilters == null)
								varFilters = new LinkedList<>();
							varFilters.add(filter.build(e.getValue()));
						}
					}
					sections.add(new StringPattern.VariableSection<T>(var.build(params), varFilters));
					return pos;
				}
			}
			name.append(c);
		}
		sections.add(new StringPattern.StringSection<T>(VAR_START + name.toString()));
		return pos;
	}
	
	private StringPatternVariable<T> getVariable(String name) {
		for (StringPatternVariable<T> v : variables)
			if (v.name().equals(name))
				return v;
		return null;
	}
	
	private StringPatternFilter getFilter(String name) {
		for (StringPatternFilter f : filters)
			if (f.name().equals(name))
				return f;
		return null;
	}
	
	private static int parseParameters(String pattern, int pos, Map<String, String> params, char delimiter, char end) {
		StringBuilder name = new StringBuilder();
		while (pos < pattern.length()) {
			char c = pattern.charAt(pos++);
			if (c == '\\') {
				if (pos < pattern.length()) {
					c = pattern.charAt(pos++);
					name.append(c);
					continue;
				}
				break;
			}
			if (c == '=') {
				pos = parseParameterValue(pattern, pos, name.toString(), params, delimiter, end);
				name = new StringBuilder();
				if (pos < pattern.length())
					c = pattern.charAt(pos++);
				else
					break;
			}
			if (c == end) {
				break;
			}
			if (c == delimiter && name.length() > 0) {
				params.put("", name.toString());
				name = new StringBuilder();
			}
			name.append(c);
		}
		return pos;
	}
	
	private static int parseParameterValue(String pattern, int pos, String name, Map<String, String> params, char delimiter, char end) {
		StringBuilder value = new StringBuilder();
		while (pos < pattern.length()) {
			char c = pattern.charAt(pos++);
			if (c == '\\') {
				if (pos < pattern.length()) {
					c = pattern.charAt(pos++);
					value.append(c);
					continue;
				}
				break;
			}
			if (c == delimiter || c == end) {
				params.put(name, value.toString());
				return pos - 1;
			}
			value.append(c);
		}
		return pos;
	}

}

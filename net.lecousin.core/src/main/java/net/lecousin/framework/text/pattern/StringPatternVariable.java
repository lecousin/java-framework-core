package net.lecousin.framework.text.pattern;

import java.util.Map;
import java.util.function.Function;

public interface StringPatternVariable<T> {

	String name();
	
	Function<T, String> build(Map<String, String> parameters);
	
}

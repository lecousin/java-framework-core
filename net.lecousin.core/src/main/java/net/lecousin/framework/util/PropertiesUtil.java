package net.lecousin.framework.util;

import java.util.Map;
import java.util.Properties;

/** Utilities for properties. */
public final class PropertiesUtil {
	
	private PropertiesUtil() {
		/* no instance */
	}

	/** Resolve the values of properties if any contains ${...}. */
	public static void resolve(Map<String, String> properties) {
		boolean resolved = true;
		while (resolved) {
			resolved = false;
			for (Map.Entry<String, String> p : properties.entrySet()) {
				String v = p.getValue();
				if (v == null) continue;
				int i = v.indexOf("${");
				if (i < 0) continue;
				int j = v.indexOf('}', i + 2);
				if (j < 0) continue;
				String name = v.substring(i + 2, j);
				String val = properties.get(name);
				if (val == null) val = "";
				v = v.substring(0, i) + val + v.substring(j + 1);
				properties.put(p.getKey(), v);
				resolved = true;
				break;
			}
		}
	}
	
	/** Resolve the given value with the given properties. */
	public static String resolve(String value, Map<String, String> properties) {
		do {
			int i = value.indexOf("${");
			if (i < 0) return value;
			int j = value.indexOf('}', i + 2);
			if (j < 0) return value;
			String name = value.substring(i + 2, j);
			String val = properties.get(name);
			if (val == null) val = "";
			value = value.substring(0, i) + val + value.substring(j + 1);
		} while (true);		
	}
	
	public static int getInt(Properties props, String name, int defaultValue) {
		String s = props.getProperty(name);
		if (s == null)
			return defaultValue;
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
}

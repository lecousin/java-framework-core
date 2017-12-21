package net.lecousin.framework.util;

import java.util.Map;

public final class PropertiesUtil {

	public static void resolve(Map<String, String> properties) {
		boolean resolved = true;
		while (resolved) {
			resolved = false;
			for (Map.Entry<String, String> p : properties.entrySet()) {
				String v = p.getValue();
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
	
}

package net.lecousin.framework.text.pattern;

public interface StringPatternFilter {

	public interface Filter {
		String filter(String value);
	}
	
	String name();
	
	Filter build(String parameter);

	
	public static class FixedLength implements StringPatternFilter {
		
		@Override
		public String name() {
			return "fixed";
		}
		
		@Override
		public Filter build(String parameter) {
			boolean dotStart = parameter.startsWith("..");
			boolean dotEnd = parameter.endsWith("..");
			if (dotStart) parameter = parameter.substring(2);
			if (dotEnd) parameter = parameter.substring(0, parameter.length() - 2);
			int size = Integer.parseInt(parameter);
			return value -> {
				if (value == null)
					value = "";
				int l = value.length();
				if (l == size)
					return value;
				if (l < size) {
					StringBuilder s = new StringBuilder(size);
					s.append(value);
					while (s.length() < size)
						s.append(' ');
					return s.toString();
				}
				if (dotStart)
					return ".." + value.substring(l - size + 2);
				if (dotEnd)
					return value.substring(size - 2) + "..";
				return value.substring(0, size);
			};
		}
		
	}
	
}

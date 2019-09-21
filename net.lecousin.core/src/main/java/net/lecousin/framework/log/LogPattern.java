package net.lecousin.framework.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.util.DebugUtil;

/**
 * %d{pattern} = date with the given pattern. The pattern is using the SimpleDateFormat pattern.
 * %t = thread name
 * %level = log level
 * %logger = logger name
 * %logger{20} = logger name with fixed length of 20 characters
 * %m = message
 * %C = class name
 * %M = method name
 * %L = line number
 * %f = file name 
 *
 */
public class LogPattern {

	/** Internally used to keep in memory a log to write. */
	@SuppressWarnings("squid:ClassVariableVisibilityCheck")
	public static class Log {
		public Level level;
		public String message;
		public Throwable trace;
		public String loggerName;
		public long timestamp;
		public String threadName;
		public StackTraceElement location;
	}
	
	/** Constructor. */
	public LogPattern(String pattern) {
		List<Section> sections = new LinkedList<>();
		int pos = 0;
		int len = pattern.length();
		while (pos < len) {
			int i = pattern.indexOf('%', pos);
			if (i > pos) {
				sections.add(new StringSection(pattern.substring(pos, i)));
				pos = i;
			} else if (i < 0) {
				sections.add(new StringSection(pattern.substring(pos)));
				break;
			}
			if (pos == len - 1) {
				sections.add(new StringSection("%"));
				break;
			}
			char c = pattern.charAt(pos + 1);
			if (c == 'd') {
				if (pos + 3 >= len || pattern.charAt(pos + 2) != '{') {
					sections.add(new StringSection(pattern.substring(pos, pos + 2)));
					pos += 2;
				} else {
					i = pattern.indexOf('}', pos + 3);
					if (i < 0) {
						sections.add(new StringSection(pattern.substring(pos, pos + 2)));
						pos += 2;
					} else {
						String format = pattern.substring(pos + 3, i);
						sections.add(new DateSection(format));
						pos = i + 1;
					}
				}
			} else if (c == 't') {
				sections.add(new ThreadNameSection());
				needsThreadName = true;
				pos += 2;
			} else if (c == '%') {
				sections.add(new StringSection("%"));
				pos += 2;
			} else if (c == 'l') {
				if (pos + 5 >= len) {
					sections.add(new StringSection(pattern.substring(pos, pos + 2)));
					pos += 2;
				} else {
					c = pattern.charAt(pos + 2);
					if (c == 'e') {
						// can be level
						if (pattern.charAt(pos + 3) == 'v' &&
							pattern.charAt(pos + 4) == 'e' &&
							pattern.charAt(pos + 5) == 'l') {
							sections.add(new LevelSection());
							pos += 6;
						} else {
							sections.add(new StringSection(pattern.substring(pos, pos + 2)));
							pos += 2;
						}
					} else if (c == 'o') {
						// can be logger or location
						if (pos < len - 7 &&
							pattern.charAt(pos + 3) == 'g' &&
							pattern.charAt(pos + 4) == 'g' &&
							pattern.charAt(pos + 5) == 'e' &&
							pattern.charAt(pos + 6) == 'r') {
							if (pos < len - 8 && pattern.charAt(pos + 7) == '{') {
								i = pattern.indexOf('}', pos + 8);
								if (i < 0) {
									sections.add(new LoggerSection(-1));
									pos += 7;
								} else {
									int size = -1;
									try { size = Integer.parseInt(pattern.substring(pos + 8, i)); }
									catch (Exception t) { /* ignore */ }
									sections.add(new LoggerSection(size));
									pos = i + 1;
								}
							} else {
								sections.add(new LoggerSection(-1));
								pos += 7;
							}
						} else {
							sections.add(new StringSection(pattern.substring(pos, pos + 2)));
							pos += 2;
						}
					} else {
						sections.add(new StringSection(pattern.substring(pos, pos + 2)));
						pos += 2;
					}
				}
			} else if (c == 'm') {
				sections.add(new MessageSection());
				pos += 2;
			} else if (c == 'C') {
				sections.add(new ClassNameSection());
				needsLocation = true;
				pos += 2;
			} else if (c == 'M') {
				sections.add(new MethodNameSection());
				needsLocation = true;
				pos += 2;
			} else if (c == 'L') {
				sections.add(new LineSection());
				needsLocation = true;
				pos += 2;
			} else if (c == 'f') {
				sections.add(new FileNameSection());
				needsLocation = true;
				pos += 2;
			} else {
				sections.add(new StringSection(pattern.substring(pos, pos + 2)));
				pos += 2;
			}
		}
		// TODO concatenate successive StringSection
		parts = sections.toArray(new Section[sections.size()]);
	}
	
	private Section[] parts;
	private boolean needsThreadName = false;
	private boolean needsLocation = false;

	public boolean needsThreadName() { return needsThreadName; }
	
	public boolean needsLocation() { return needsLocation; }
	
	/** Generate a log string. */
	public StringBuilder generate(Log log) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < parts.length; ++i)
			parts[i].append(s, log);
		if (log.trace != null)
			appendStack(s, log.trace);
		return s;
	}
	
	private static void appendStack(StringBuilder s, Throwable t) {
		s.append('\n');
		DebugUtil.createStackTrace(s, t, true);
	}
	
	private static interface Section {
		void append(StringBuilder s, Log log);
	}
	
	private static class StringSection implements Section {
		public StringSection(String str) {
			this.str = str;
		}
		
		private String str;
		
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(str);
		}
	}
	
	private static class MessageSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.message);
		}
	}
	
	private static class ThreadNameSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.threadName);
		}
	}
	
	private static class DateSection implements Section {
		public DateSection(String format) {
			this.format = new SimpleDateFormat(format);
		}
		
		private SimpleDateFormat format;
		
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(format.format(new Date(log.timestamp)));
		}
	}
	
	private static class ClassNameSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.location.getClassName());
		}
	}
	
	private static class MethodNameSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.location.getMethodName());
		}
	}
	
	private static class LineSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.location.getLineNumber());
		}
	}
	
	private static class FileNameSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			s.append(log.location.getFileName());
		}
	}
	
	private static class LevelSection implements Section {
		@Override
		public void append(StringBuilder s, Log log) {
			String name = log.level.name();
			s.append(name);
			for (int i = name.length(); i < 5; ++i)
				s.append(' ');
		}
	}
	
	private static class LoggerSection implements Section {
		private LoggerSection(int fixedSize) {
			if (fixedSize > 0 && fixedSize < 3) fixedSize = 3; 
			this.fixedSize = fixedSize;
		}
		
		private int fixedSize;
		
		@Override
		public void append(StringBuilder s, Log log) {
			int len = log.loggerName.length();
			if (len == fixedSize || fixedSize <= 0)
				s.append(log.loggerName);
			else if (len < fixedSize) {
				s.append(log.loggerName);
				for (int i = len; i < fixedSize; ++i)
					s.append(' ');
			} else {
				s.append("..").append(log.loggerName.substring(len - fixedSize + 2));
			}
		}
	}
	
}

package net.lecousin.framework.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.util.DebugUtil;

/**
 * %d{pattern} = date with the given pattern. The pattern is using the SimpleDateFormat pattern.<br/>
 * %t = thread name<br/>
 * %level = log level<br/>
 * %logger = logger name<br/>
 * %logger{20} = logger name with fixed length of 20 characters<br/>
 * %application = application full name (groupId-artifactId-version)<br/>
 * %application{20} = application full name with fixed length of 20 characters<br/>
 * %artifactId = application artifactId<br/>
 * %artifactId{20} = application artifactId with fixed length of 20 characters<br/>
 * %m = message<br/>
 * %C = class name<br/>
 * %M = method name<br/>
 * %L = line number<br/>
 * %f = file name <br/>
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
		public Application app;
	}
	
	/** Constructor. */
	public LogPattern(String pattern) {
		parsePattern(pattern);
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
	
	private static class ApplicationSection implements Section {
		private ApplicationSection(int fixedSize) {
			if (fixedSize > 0 && fixedSize < 3) fixedSize = 3; 
			this.fixedSize = fixedSize;
		}
		
		private int fixedSize;
		
		@Override
		public void append(StringBuilder s, Log log) {
			String applicationName = log.app.getFullName();
			int len = applicationName.length();
			if (len == fixedSize || fixedSize <= 0)
				s.append(applicationName);
			else if (len < fixedSize) {
				s.append(applicationName);
				for (int i = len; i < fixedSize; ++i)
					s.append(' ');
			} else {
				s.append("..").append(applicationName.substring(len - fixedSize + 2));
			}
		}
	}
	
	private static class ArtifactIdSection implements Section {
		private ArtifactIdSection(int fixedSize) {
			if (fixedSize > 0 && fixedSize < 3) fixedSize = 3; 
			this.fixedSize = fixedSize;
		}
		
		private int fixedSize;
		
		@Override
		public void append(StringBuilder s, Log log) {
			String artifactId = log.app.getArtifactId();
			int len = artifactId.length();
			if (len == fixedSize || fixedSize <= 0)
				s.append(artifactId);
			else if (len < fixedSize) {
				s.append(artifactId);
				for (int i = len; i < fixedSize; ++i)
					s.append(' ');
			} else {
				s.append("..").append(artifactId.substring(len - fixedSize + 2));
			}
		}
	}
	
	private void parsePattern(String pattern) {
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
			switch (c) {
			case 'a':
				pos = parsePatternA(pattern, pos, len, sections);
				break;
			case 'd':
				pos = parsePatternD(pattern, pos, len, sections);
				break;
			case 'f':
				sections.add(new FileNameSection());
				needsLocation = true;
				pos += 2;
				break;
			case 't':
				sections.add(new ThreadNameSection());
				needsThreadName = true;
				pos += 2;
				break;
			case 'l':
				pos = parsePatternL(pattern, pos, len, sections);
				break;
			case 'm':
				sections.add(new MessageSection());
				pos += 2;
				break;
			case 'C':
				sections.add(new ClassNameSection());
				needsLocation = true;
				pos += 2;
				break;
			case 'L':
				sections.add(new LineSection());
				needsLocation = true;
				pos += 2;
				break;
			case 'M':
				sections.add(new MethodNameSection());
				needsLocation = true;
				pos += 2;
				break;
			case '%':
				sections.add(new StringSection("%"));
				pos += 2;
				break;
			default:
				sections.add(new StringSection(pattern.substring(pos, pos + 2)));
				pos += 2;
				break;
			}
		}
		// TODO concatenate successive StringSection
		parts = sections.toArray(new Section[sections.size()]);
	}
	
	private static int parsePatternA(String pattern, int pos, int len, List<Section> sections) {
		if (pos + 10 >= len) {
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		char c = pattern.charAt(pos + 2);
		switch (c) {
		case 'p':
			// can be application
			if (pos <= len - 12 &&
				pattern.charAt(pos + 3) == 'p' &&
				pattern.charAt(pos + 4) == 'l' &&
				pattern.charAt(pos + 5) == 'i' &&
				pattern.charAt(pos + 6) == 'c' &&
				pattern.charAt(pos + 7) == 'a' &&
				pattern.charAt(pos + 8) == 't' &&
				pattern.charAt(pos + 9) == 'i' &&
				pattern.charAt(pos + 10) == 'o' &&
				pattern.charAt(pos + 11) == 'n') {
				if (pos <= len - 13 && pattern.charAt(pos + 12) == '{') {
					int i = pattern.indexOf('}', pos + 13);
					if (i < 0) {
						sections.add(new ApplicationSection(-1));
						return pos + 12;
					}
					int size = -1;
					try { size = Integer.parseInt(pattern.substring(pos + 13, i)); }
					catch (Exception t) { /* ignore */ }
					sections.add(new ApplicationSection(size));
					return i + 1;
				}
				sections.add(new ApplicationSection(-1));
				return pos + 12;
			}
			break;
		case 'r':
			// can be artifactId
			if (pos <= len - 11 &&
				pattern.charAt(pos + 3) == 't' &&
				pattern.charAt(pos + 4) == 'i' &&
				pattern.charAt(pos + 5) == 'f' &&
				pattern.charAt(pos + 6) == 'a' &&
				pattern.charAt(pos + 7) == 'c' &&
				pattern.charAt(pos + 8) == 't' &&
				pattern.charAt(pos + 9) == 'I' &&
				pattern.charAt(pos + 10) == 'd') {
				if (pos <= len - 12 && pattern.charAt(pos + 11) == '{') {
					int i = pattern.indexOf('}', pos + 12);
					if (i < 0) {
						sections.add(new ArtifactIdSection(-1));
						return pos + 11;
					}
					int size = -1;
					try { size = Integer.parseInt(pattern.substring(pos + 12, i)); }
					catch (Exception t) { /* ignore */ }
					sections.add(new ArtifactIdSection(size));
					return i + 1;
				}
				sections.add(new ArtifactIdSection(-1));
				return pos + 11;
			}
			break;
		default:
			break;
		}
		sections.add(new StringSection(pattern.substring(pos, pos + 2)));
		return pos + 2;
	}
	
	private static int parsePatternD(String pattern, int pos, int len, List<Section> sections) {
		if (pos + 3 >= len || pattern.charAt(pos + 2) != '{') {
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		int i = pattern.indexOf('}', pos + 3);
		if (i < 0) {
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		String format = pattern.substring(pos + 3, i);
		sections.add(new DateSection(format));
		return i + 1;
	}
	
	private static int parsePatternL(String pattern, int pos, int len, List<Section> sections) {
		if (pos + 5 >= len) {
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		char c = pattern.charAt(pos + 2);
		if (c == 'e') {
			// can be level
			if (pattern.charAt(pos + 3) == 'v' &&
				pattern.charAt(pos + 4) == 'e' &&
				pattern.charAt(pos + 5) == 'l') {
				sections.add(new LevelSection());
				return pos + 6;
			}
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		if (c == 'o') {
			// can be logger or location
			if (pos <= len - 7 &&
				pattern.charAt(pos + 3) == 'g' &&
				pattern.charAt(pos + 4) == 'g' &&
				pattern.charAt(pos + 5) == 'e' &&
				pattern.charAt(pos + 6) == 'r') {
				if (pos <= len - 8 && pattern.charAt(pos + 7) == '{') {
					int i = pattern.indexOf('}', pos + 8);
					if (i < 0) {
						sections.add(new LoggerSection(-1));
						return pos + 7;
					}
					int size = -1;
					try { size = Integer.parseInt(pattern.substring(pos + 8, i)); }
					catch (Exception t) { /* ignore */ }
					sections.add(new LoggerSection(size));
					return i + 1;
				}
				sections.add(new LoggerSection(-1));
				return pos + 7;
			}
			sections.add(new StringSection(pattern.substring(pos, pos + 2)));
			return pos + 2;
		}
		sections.add(new StringSection(pattern.substring(pos, pos + 2)));
		return pos + 2;
	}
}

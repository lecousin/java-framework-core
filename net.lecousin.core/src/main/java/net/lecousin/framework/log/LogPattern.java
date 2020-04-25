package net.lecousin.framework.log;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.text.pattern.StringPattern;
import net.lecousin.framework.text.pattern.StringPatternBuilder;
import net.lecousin.framework.text.pattern.StringPatternFilter;
import net.lecousin.framework.text.pattern.StringPatternVariable;
import net.lecousin.framework.util.DebugUtil;

/**
 * It uses {@link StringPatternBuilder} to build a string pattern.<br/>
 * The available variables are:
 * <ul>
 * <li>message</li>
 * <li>level: log level</li>
 * <li>logger</li>
 * <li>date: with one parameter "format" (see {@link SimpleDateFormat})</li>
 * <li>thread</li>
 * <li>class</li>
 * <li>method</li>
 * <li>line</li>
 * <li>filename</li>
 * <li>application: application full name</li>
 * <li>groupId</li>
 * <li>artifactId</li>
 * <li>version</li>
 * <li>context: with one parameter "name", to log the content of the current task's context attribute "name"</li>
 * </ul>
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
		public String[] contextsValues;
	}
	
	public static final StringPatternBuilder<Log> builder = new StringPatternBuilder<>(
		Arrays.asList(
			new MessageVariable(),
			new ThreadNameVariable(),
			new DateVariable(),
			new LevelVariable(),
			new LoggerVariable(),
			new ClassNameVariable(),
			new MethodNameVariable(),
			new LineVariable(),
			new FileNameVariable(),
			new ApplicationVariable(),
			new GroupIdVariable(),
			new ArtifactIdVariable(),
			new VersionVariable(),
			new ContextVariable()
		),
		Arrays.asList(
			new StringPatternFilter.FixedLength()
		)
	);
	
	/** Constructor. */
	public LogPattern(String pattern) {
		this.pattern = builder.build(pattern);
		List<String> contexts = new LinkedList<>();
		for (StringPattern.Section<Log> section : this.pattern.getSections())
			if (section instanceof StringPattern.VariableSection) {
				Function<Log, String> variable = ((StringPattern.VariableSection<Log>)section).getVariable();
				if (variable instanceof ThreadNameSection)
					needsThreadName = true;
				else if (variable instanceof ClassNameSection ||
						 variable instanceof MethodNameSection ||
						 variable instanceof LineSection ||
						 variable instanceof FileNameSection)
					needsLocation = true;
				else if (variable instanceof ContextSection) {
					((ContextSection)variable).index = contexts.size();
					contexts.add(((ContextSection)variable).name);
				}
			}
		if (!contexts.isEmpty())
			neededContexts = contexts.toArray(new String[contexts.size()]);
	}
	
	private StringPattern<Log> pattern;
	private boolean needsThreadName = false;
	private boolean needsLocation = false;
	private String[] neededContexts = null;

	public boolean needsThreadName() { return needsThreadName; }
	
	public boolean needsLocation() { return needsLocation; }
	
	public String[] neededContexts() { return neededContexts; }
	
	/** Generate a log string. */
	public StringBuilder generate(Log log) {
		StringBuilder s = new StringBuilder();
		pattern.append(s, log);
		if (log.trace != null)
			appendStack(s, log.trace);
		return s;
	}
	
	private static void appendStack(StringBuilder s, Throwable t) {
		s.append('\n');
		DebugUtil.createStackTrace(s, t, true);
	}
	
	private static class MessageVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "message";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new MessageSection();
		}
	}
	
	private static class MessageSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.message;
		}
	}
	
	private static class ThreadNameVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "thread";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new ThreadNameSection();
		}
	}
	
	private static class ThreadNameSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.threadName;
		}
	}

	private static class DateVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "date";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new DateSection(parameters.get("format"));
		}
	}
	
	private static class DateSection implements Function<Log, String> {
		public DateSection(String format) {
			if (format == null) format = "yyyy/MM/dd HH:mm:ss.SSS";
			this.format = new SimpleDateFormat(format);
		}
		
		private SimpleDateFormat format;

		@Override
		public synchronized String apply(Log log) {
			return format.format(new Date(log.timestamp));
		}
	}

	private static class ClassNameVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "class";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new ClassNameSection();
		}
	}
	
	private static class ClassNameSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.location.getClassName();
		}
	}

	private static class MethodNameVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "method";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new MethodNameSection();
		}
	}
	
	private static class MethodNameSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.location.getMethodName();
		}
	}

	private static class LineVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "line";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new LineSection();
		}
	}
	
	private static class LineSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return Integer.toString(log.location.getLineNumber());
		}
	}

	private static class FileNameVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "filename";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new FileNameSection();
		}
	}
	
	private static class FileNameSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.location.getFileName();
		}
	}

	private static class LevelVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "level";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new LevelSection();
		}
	}
	
	private static class LevelSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.level.name();
		}
	}

	private static class LoggerVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "logger";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new LoggerSection();
		}
	}
	
	private static class LoggerSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.loggerName;
		}
	}

	private static class ApplicationVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "application";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new ApplicationSection();
		}
	}
	
	private static class ApplicationSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.app.getFullName();
		}
	}

	private static class GroupIdVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "groupId";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new GroupIdSection();
		}
	}
	
	private static class GroupIdSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.app.getGroupId();
		}
	}

	private static class ArtifactIdVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "artifactId";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new ArtifactIdSection();
		}
	}
	
	private static class ArtifactIdSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.app.getArtifactId();
		}
	}

	private static class VersionVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "version";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new VersionSection();
		}
	}
	
	private static class VersionSection implements Function<Log, String> {
		@Override
		public String apply(Log log) {
			return log.app.getVersion().toString();
		}
	}

	private static class ContextVariable implements StringPatternVariable<Log> {
		@Override
		public String name() {
			return "context";
		}

		@Override
		public Function<Log, String> build(Map<String, String> parameters) {
			return new ContextSection(parameters.get("name"));
		}
	}
	
	private static class ContextSection implements Function<Log, String> {
		public ContextSection(String name) {
			this.name = name;
		}
		
		private String name;
		private int index;
		
		@Override
		public String apply(Log log) {
			return log.contextsValues[index];
		}
	}

}

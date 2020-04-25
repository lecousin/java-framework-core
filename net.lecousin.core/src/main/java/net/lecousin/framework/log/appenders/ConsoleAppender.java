package net.lecousin.framework.log.appenders;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;

/** Log appender to the console. */
public class ConsoleAppender extends Appender {
	
	private Console console;
	private LogPattern pattern;

	/** Constructor. */
	public ConsoleAppender(Console console, Level level, LogPattern pattern, List<LogFilter> filters) {
		super(level, filters);
		this.console = console;
		this.pattern = pattern;
	}
	
	/** Constructor. */
	public ConsoleAppender(LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
		super(factory, reader, appenders);
	}
	
	@Override
	protected void init(LoggerFactory factory, Map<String, Appender> appenders) {
		this.console = factory.getApplication().getConsole();
		super.init(factory, appenders);
	}
	
	@Override
	protected boolean configureAttribute(String name, String value) throws LoggerConfigurationException {
		if ("pattern".equals(name)) {
			this.pattern = new LogPattern(value);
			return true;
		}
		return super.configureAttribute(name, value);
	}
	
	@Override
	protected void checkAttributes() throws LoggerConfigurationException {
		super.checkAttributes();
		if (pattern == null) throw new LoggerConfigurationException("Missing attribute pattern on console Appender");
	}
	
	@Override
	public void append(Log log) {
		String s = pattern.generate(log).toString();
		if (log.level.ordinal() >= Level.ERROR.ordinal())
			console.err(s);
		else
			console.out(s);
	}

	@Override
	public boolean needsThreadName() {
		return pattern.needsThreadName();
	}

	@Override
	public boolean needsLocation() {
		return pattern.needsLocation();
	}
	
	@Override
	public String[] neededContexts() {
		return pattern.neededContexts();
	}
	
	@Override
	public IAsync<Exception> flush() {
		return new Async<>(true);
	}

}

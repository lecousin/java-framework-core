package net.lecousin.framework.log.appenders;

import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;

/** Log appender to the console. */
public class ConsoleAppender implements Appender {

	/** Constructor. */
	public ConsoleAppender(Console console, Level level, LogPattern pattern) {
		this.console = console;
		this.level = level;
		this.pattern = pattern;
	}
	
	/** Constructor. */
	public ConsoleAppender(
		LoggerFactory factory, XMLStreamReader reader, @SuppressWarnings({"unused","squid:S1172"}) Map<String,Appender> appenders
	) throws LoggerConfigurationException, XMLStreamException {
		this.console = factory.getApplication().getConsole();
		String levelStr = null;
		String patternStr = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("level".equals(attrName))
				levelStr = attrValue;
			else if ("pattern".equals(attrName))
				patternStr = attrValue;
			else if (!"name".equals(attrName) && !"class".equals(attrName))
				throw new LoggerConfigurationException("Unknown attribute " + attrName);
		}
		
		if (levelStr == null) throw new LoggerConfigurationException("Missing attribute level on console Appender");
		try { this.level = Level.valueOf(levelStr); }
		catch (Exception t) { throw new LoggerConfigurationException("Invalid level " + levelStr); }
		
		if (patternStr == null) throw new LoggerConfigurationException("Missing attribute pattern on console Appender");
		this.pattern = new LogPattern(patternStr);
		
		reader.next();
		do {
			if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
				break;
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				throw new LoggerConfigurationException("Unexpected inner element " + reader.getLocalName());
			}
			reader.next();
		} while (reader.hasNext());
	}
	
	private Console console;
	private Level level;
	private LogPattern pattern;
	
	@Override
	public void append(Log log) {
		String s = pattern.generate(log).toString();
		if (log.level.ordinal() >= Level.ERROR.ordinal())
			console.err(s);
		else
			console.out(s);
	}

	@Override
	public int level() {
		return level.ordinal();
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
	public ISynchronizationPoint<Exception> flush() {
		return new SynchronizationPoint<>(true);
	}

}

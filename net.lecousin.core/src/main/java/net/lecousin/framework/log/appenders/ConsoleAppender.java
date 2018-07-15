package net.lecousin.framework.log.appenders;

import java.io.IOException;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerFactory;

/** Log appender to the console. */
public class ConsoleAppender implements Appender {

	/** Constructor. */
	public ConsoleAppender(LoggerFactory factory, Level level, LogPattern pattern) {
		this.factory = factory;
		this.level = level;
		this.pattern = pattern;
	}
	
	/** Constructor. */
	public ConsoleAppender(
		LoggerFactory factory, XMLStreamReader reader, @SuppressWarnings("unused") Map<String,Appender> appenders
	) throws Exception, IOException {
		this.factory = factory;
		String level = null;
		String pattern = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("level".equals(attrName))
				level = attrValue;
			else if ("pattern".equals(attrName))
				pattern = attrValue;
			else if (!"name".equals(attrName) && !"class".equals(attrName))
				throw new Exception("Unknown attribute " + attrName);
		}
		
		if (level == null) throw new Exception("Missing attribute level on console Appender");
		try { this.level = Level.valueOf(level); }
		catch (Throwable t) { throw new Exception("Invalid level " + level); }
		
		if (pattern == null) throw new Exception("Missing attribute pattern on console Appender");
		this.pattern = new LogPattern(pattern);
		
		reader.next();
		do {
			if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
				break;
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				throw new Exception("Unexpected inner element " + reader.getLocalName());
			}
			reader.next();
		} while (reader.hasNext());
	}
	
	private LoggerFactory factory;
	private Level level;
	private LogPattern pattern;
	
	@Override
	public void append(Log log) {
		String s = pattern.generate(log).toString();
		if (log.level.ordinal() >= Level.ERROR.ordinal())
			factory.getApplication().getConsole().err(s);
		else
			factory.getApplication().getConsole().out(s);
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

package net.lecousin.framework.log.appenders;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;

/** Log appender. */
public abstract class Appender {
	
	protected int level = -1;
	protected List<LogFilter> filters;
	
	public Appender(Level level, List<LogFilter> filters) {
		this.level = level.ordinal();
		if (filters == null) filters = new LinkedList<>();
		this.filters = filters;
	}
	
	public Appender(LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
		this.filters = new LinkedList<>();
		init(factory, appenders);
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			if ("name".equals(attrName) || "class".equals(attrName))
				continue;
			String attrValue = reader.getAttributeValue(i);
			if (!configureAttribute(attrName, attrValue))
				throw new LoggerConfigurationException("Unknown attribute " + attrName);
		}
		checkAttributes();
		try {
			reader.next();
			do {
				if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
					break;
				if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && !configureInnerElement(factory, reader, appenders))
					throw new LoggerConfigurationException("Unexpected inner element " + reader.getLocalName());
				reader.next();
			} while (reader.hasNext());
		} catch (XMLStreamException e) {
			throw new LoggerConfigurationException("Invalid XML", e);
		}
	}
	
	@SuppressWarnings({"unused","squid:S1172"})
	protected void init(LoggerFactory factory, Map<String,Appender> appenders) {
		// nothing
	}
	
	protected boolean configureAttribute(String name, String value) throws LoggerConfigurationException {
		if ("level".equals(name)) {
			try { this.level = Level.valueOf(value).ordinal(); }
			catch (Exception t) { throw new LoggerConfigurationException("Invalid level " + value); }
			return true;
		}
		return false;
	}
	
	protected void checkAttributes() throws LoggerConfigurationException {
		if (level == -1)
			throw new LoggerConfigurationException("Missing attribute level on appender " + getClass().getName());
	}
	
	@SuppressWarnings({"unused","squid:S1172"})
	protected boolean configureInnerElement(LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
		if ("Filter".equals(reader.getLocalName())) {
			parseFilter(reader);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("java:S3776") // complexity
	private void parseFilter(XMLStreamReader reader) throws LoggerConfigurationException {
		LogFilter filter = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			if ("class".equals(attrName)) {
				Class<?> cl;
				try {
					cl = Class.forName(reader.getAttributeValue(i));
				} catch (Exception e) {
					throw new LoggerConfigurationException("Unknown log filter class: " + reader.getAttributeValue(i));
				}
				if (!LogFilter.class.isAssignableFrom(cl))
					throw new LoggerConfigurationException("Invalid log filter class: " + reader.getAttributeValue(i));
				try {
					Constructor<?> ctor = cl.getConstructor(XMLStreamReader.class);
					filter = (LogFilter)ctor.newInstance(reader);
				} catch (Exception e) {
					try {
						filter = (LogFilter)cl.newInstance();
					} catch (Exception e2) {
						throw new LoggerConfigurationException("Unable to instantiate log filter class " + cl.getName());
					}
					try {
						reader.next();
						do {
							if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
								break;
							if (reader.getEventType() == XMLStreamConstants.START_ELEMENT)
								throw new LoggerConfigurationException("Unexpected inner element "
									+ reader.getLocalName());
							reader.next();
						} while (reader.hasNext());
					} catch (XMLStreamException e2) {
						throw new LoggerConfigurationException("Invalid XML", e2);
					}
				}
				break;
			}
		}
		if (filter == null)
			throw new LoggerConfigurationException("Missing attribute class on Filter");
		this.filters.add(filter);
	}
	
	public void addFilter(LogFilter filter) {
		synchronized (filters) {
			filters.add(filter);
		}
	}
	
	public void removeFilter(LogFilter filter) {
		synchronized (filters) {
			filters.remove(filter);
		}
	}

	/** Append the given log. */
	public final boolean filter(Log log) {
		synchronized (filters) {
			if (filters != null)
				for (LogFilter filter : filters)
					if (filter.test(log))
						return true;
			return false;
		}
	}
	
	/** Append the given log. */
	public abstract void append(Log log);
	
	/** Return the level of this appender. */
	public final int level() {
		return level;
	}
	
	/** Return true if this appender needs the thread name. */
	public abstract boolean needsThreadName();
	
	/** Return true if this appender needs the location. */
	public abstract boolean needsLocation();
	
	/** Return the needed task context attributes. */
	public abstract String[] neededContexts();
	
	/** Ask to flush any pending log that this appender is still holding. */
	public abstract IAsync<?> flush();
	
}

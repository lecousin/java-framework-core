package net.lecousin.framework.log.appenders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;

/** Log appender that forwards logs to multiple appenders. */
public class MultipleAppender implements Appender {

	/** Constructor. */
	public MultipleAppender(@SuppressWarnings({"unused","squid:S1172"}) LoggerFactory factory, Appender...appenders) {
		this.appenders = appenders;
		level = Level.OFF.ordinal();
		for (Appender a : appenders) {
			if (a.level() < level)
				level = a.level();
			if (!threadName) threadName = a.needsThreadName();
			if (!location) location = a.needsLocation();
		}
	}
	
	/** Constructor. */
	public MultipleAppender(
		@SuppressWarnings({"unused","squid:S1172"}) LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders
	) throws LoggerConfigurationException, XMLStreamException {
		level = Level.OFF.ordinal();
		List<Appender> list = new ArrayList<>();
		
		reader.next();
		while (reader.hasNext()) {
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if ("AppenderRef".equals(reader.getLocalName())) {
					String name = null;
					for (int i = 0; i < reader.getAttributeCount(); ++i) {
						String attrName = reader.getAttributeLocalName(i);
						String attrValue = reader.getAttributeValue(i);
						if ("name".equals(attrName))
							name = attrValue;
						else
							throw new LoggerConfigurationException("Unknown attribute " + attrName + " in AppenderRef");
					}
					
					if (name == null) throw new LoggerConfigurationException("Missing attribute name on AppenderRef");
					Appender appender = appenders.get(name);
					if (appender == null) throw new LoggerConfigurationException("Unknown appender " + name + " in AppenderRef");
					list.add(appender);
					if (appender.level() < level)
						level = appender.level();
					if (!threadName) threadName = appender.needsThreadName();
					if (!location) location = appender.needsLocation();
					
					reader.next();
					do {
						if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
							break;
						if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
							throw new LoggerConfigurationException(
								"Unexpected element " + reader.getLocalName() + " in AppenderRef");
						}
						reader.next();
					} while (reader.hasNext());
				} else {
					throw new LoggerConfigurationException("Unknown element " + reader.getLocalName()
						+ " in MultipleAppender, only AppenderRef elements are expected");
				}
			} else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
				break;
			}
			reader.next();
		}

		this.appenders = list.toArray(new Appender[list.size()]);
	}
	
	private Appender[] appenders;
	private int level;
	private boolean threadName = false;
	private boolean location = false;
	
	@Override
	public void append(Log log) {
		for (Appender a : appenders)
			a.append(log);
	}

	@Override
	public int level() {
		return level;
	}

	@Override
	public boolean needsThreadName() {
		return threadName;
	}

	@Override
	public boolean needsLocation() {
		return location;
	}
	
	@Override
	public IAsync<Exception> flush() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (Appender a : appenders)
			jp.addToJoin(a.flush());
		jp.start();
		return jp;
	}
	
}

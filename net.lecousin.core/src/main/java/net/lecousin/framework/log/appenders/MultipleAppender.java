package net.lecousin.framework.log.appenders;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.log.LogFilter;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.math.RangeInteger;

/** Log appender that forwards logs to multiple appenders. */
public class MultipleAppender extends Appender {
	
	private Appender[] appenders;
	private boolean threadName = false;
	private boolean location = false;
	private String[] neededContexts;
	private RangeInteger[] appendersContextsRanges;

	/** Constructor. */
	public MultipleAppender(List<LogFilter> filters, Appender...appenders) {
		super(Level.OFF, filters);
		this.appenders = appenders;
		initEnd();
	}
	
	/** Constructor. */
	public MultipleAppender(LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
		super(factory, reader, appenders);
		initEnd();
	}
	
	@Override
	protected void init(LoggerFactory factory, Map<String, Appender> appenders) {
		this.appenders = new Appender[0];
		level = Level.OFF.ordinal();
		super.init(factory, appenders);
	}
	
	private void initEnd() {
		level = Level.OFF.ordinal();
		int nb = 0;
		for (Appender a : appenders) {
			if (a.level() < level)
				level = a.level();
			if (!threadName) threadName = a.needsThreadName();
			if (!location) location = a.needsLocation();

			String[] ctx = a.neededContexts();
			if (ctx != null) nb += ctx.length;
		}
		if (nb > 0) {
			this.appendersContextsRanges = new RangeInteger[this.appenders.length];
			neededContexts = new String[nb];
			int pos = 0;
			for (int i = 0; i < this.appenders.length; ++i) {
				String[] ctx = this.appenders[i].neededContexts();
				if (ctx == null)
					continue;
				this.appendersContextsRanges[i] = new RangeInteger(pos, pos + ctx.length - 1);
				System.arraycopy(ctx, 0, neededContexts, pos, ctx.length);
				pos += ctx.length;
			}
		}
	}
	
	@Override
	protected boolean configureInnerElement(LoggerFactory factory, XMLStreamReader reader, Map<String, Appender> appenders)
	throws LoggerConfigurationException {
		if ("AppenderRef".equals(reader.getLocalName())) {
			parseAppenderRef(reader, appenders);
			return true;
		}
		return super.configureInnerElement(factory, reader, appenders);
	}
	
	private void parseAppenderRef(XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
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
		if (appender == null) throw new LoggerConfigurationException("Unknown appender " + name);
		Appender[] a = new Appender[this.appenders.length + 1];
		System.arraycopy(this.appenders, 0, a, 0, this.appenders.length);
		a[this.appenders.length] = appender;
		this.appenders = a;
		
		try {
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
		} catch (XMLStreamException e) {
			throw new LoggerConfigurationException("Invalid XML in AppenderRef", e);
		}
	}
	
	@Override
	public void append(Log log) {
		String[] originalContexts = log.contextsValues;
		for (int i = 0; i < appenders.length; ++i) {
			Appender a = appenders[i];
			RangeInteger r = appendersContextsRanges != null ? appendersContextsRanges[i] : null;
			if (r == null)
				log.contextsValues = null;
			else {
				log.contextsValues = new String[r.max - r.min + 1];
				System.arraycopy(originalContexts, r.min, log.contextsValues, 0, log.contextsValues.length);
			}
			a.append(log);
		}
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
	public String[] neededContexts() {
		return neededContexts;
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

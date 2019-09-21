package net.lecousin.framework.log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.log.appenders.ConsoleAppender;

/** Provide logger for an application. */
public class LoggerFactory {
	
	Application application;
	LoggerThread thread;
	private Appender defaultAppender;
	private Map<String, Logger> loggers = new HashMap<>(50);
	private Logger defaultLogger;
	private Map<String,Appender> appenders = new HashMap<>();
	
	/** Constructor. */
	public LoggerFactory(Application app, Appender defaultAppender) {
		application = app;
		thread = new LoggerThread(app);
		if (defaultAppender == null)
			defaultAppender = new ConsoleAppender(app.getConsole(), app.isReleaseMode() ? Level.INFO : Level.DEBUG,
				new LogPattern("%d{HH:mm:ss.SSS} [%level] <%logger{20}> %m"));
		this.defaultAppender = defaultAppender;
		defaultLogger = new Logger(this, "default", defaultAppender, null);
		loggers.put("default", defaultLogger);
		
		String url = app.getProperty(Application.PROPERTY_LOGGING_CONFIGURATION_URL);
		if (url != null)
			configure(url);
	}
	
	public Application getApplication() {
		return application;
	}
	
	public Logger getDefault() {
		return defaultLogger;
	}
	
	/** Return the logger for the given class. */
	public Logger getLogger(Class<?> cl) {
		return getLogger(cl.getName());
	}
	
	/** Return the logger with the given name. */
	public synchronized Logger getLogger(String name) {
		Logger l = loggers.get(name);
		if (l != null)
			return l;
		l = new Logger(this, name, defaultAppender, null);
		loggers.put(name, l);
		return l;
	}
	
	/** Set the default appender. */
	public synchronized void setDefault(Appender appender) {
		for (Logger l : loggers.values())
			if (l.appender == defaultAppender)
				l.appender = appender;
		defaultAppender = appender;
	}
	
	/** Return a synchronization point that will be unblocked as soon as all pending logs have been written. */
	public ISynchronizationPoint<Exception> flush() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		thread.flush().listenAsync(new Task.Cpu.FromRunnable("Flushing log appenders", Task.PRIORITY_IMPORTANT, () -> {
			for (Appender a : appenders.values())
				jp.addToJoin(a.flush());
			jp.start();
		}), true);
		return jp;
	}
	
	/** Configure a logger with an appender. */
	public synchronized void configure(String name, Appender appender, Level level) {
		Logger l = loggers.get(name);
		if (l != null) {
			l.appender = appender;
			l.setLevel(level);
			return;
		}
		l = new Logger(this, name, appender, level);
		loggers.put(name, l);
	}
	
	/** Load configuration from a file. */
	public void configure(String url) {
		application.getConsole().out("Configuring logging " + url);
		InputStream input = null;
		try {
			input = new URL(url).openStream();
			configure(input);
		} catch (Exception e) {
			application.getConsole().err("Error configuring logging system from " + url + ": " + e.getMessage());
			application.getConsole().err(e);
		} finally {
			if (input != null) try { input.close(); } catch (Exception t) { /* ignore */ }
		}
	}
	
	/** Load configuration from a file. */
	public synchronized void configure(InputStream input) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		configure(factory.createXMLStreamReader(new PropertiesStream(application, input)));
	}
	
	private static class PropertiesStream extends InputStream {
		public PropertiesStream(Application app, InputStream input) throws IOException {
			this.app = app;
			this.input = input;
			pos = 0;
			nb = input.read(buffer);
			if (nb <= 0) {
				nb = 0;
				eof = true;
			}
		}
		
		private Application app;
		private InputStream input;
		private byte[] buffer = new byte[4096];
		private int pos;
		private int nb;
		private boolean eof = false;
		private LinkedList<Integer> back = new LinkedList<>();
		
		private int next() throws IOException {
			if (!back.isEmpty()) return back.removeFirst().intValue();
			if (pos == nb) {
				if (eof) return -1;
				pos = 0;
				nb = input.read(buffer);
				if (nb <= 0) {
					eof = true;
					nb = 0;
					return -1;
				}
			}
			return buffer[pos++] & 0xFF;
		}
		
		@Override
		public int read() throws IOException {
			int c1 = next();
			if (c1 < 0) return -1;
			if (c1 != '$') return c1;
			int c2 = next();
			if (c2 < 0) return c1;
			if (c2 != '{') {
				back.add(Integer.valueOf(c2));
				return c1;
			}
			StringBuilder s = new StringBuilder();
			do {
				int c = next();
				if (c < 0)
					throw new IOException("Property starts with ${ but ending } is missing");
				if (c == '}') {
					String prop = app.getProperty(s.toString());
					if (prop == null) {
						app.getConsole().err("Unknown property " + s.toString() + " used in Logging configuration file");
						return read();
					}
					byte[] bytes = prop.getBytes(StandardCharsets.ISO_8859_1);
					for (int i = 0; i < bytes.length; ++i)
						back.add(Integer.valueOf(bytes[i] & 0xFF));
					return read();
				}
				s.append((char)c);
			} while (true);
		}
	}
	
	/** Load configuration from a file. */
	public synchronized void configure(XMLStreamReader reader) throws LoggerConfigurationException, XMLStreamException {
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (!"LoggingConfiguration".equals(reader.getLocalName()))
					throw new LoggerConfigurationException("Root element must be LoggingConfiguration");
				readLoggingConfiguration(reader);
				return;
			}
		}
		throw new LoggerConfigurationException("No root element found in logging configuration file");
	}
	
	private void readLoggingConfiguration(XMLStreamReader reader) throws LoggerConfigurationException, XMLStreamException {
		reader.next();
		while (reader.hasNext()) {
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if ("Appender".equals(reader.getLocalName())) {
					try { readAppender(reader, appenders); }
					catch (Exception e) {
						throw new LoggerConfigurationException(
							"Invalid appender definition in logging configuration file", e);
					}
				} else if ("Logger".equals(reader.getLocalName())) {
					try { readLogger(reader, appenders); }
					catch (Exception e) {
						throw new LoggerConfigurationException("Invalid logger definition in logging configuration file", e);
					}
				} else if ("Default".equals(reader.getLocalName())) {
					try { readDefault(reader, appenders); }
					catch (Exception e) {
						throw new LoggerConfigurationException("Invalid logger definition in logging configuration file", e);
					}
				} else {
					throw new LoggerConfigurationException(
						"Unknown element " + reader.getLocalName() + " in logging configuration file");
				}
			}
			reader.next();
		}
	}
	
	private void readAppender(XMLStreamReader reader, Map<String,Appender> appenders) throws LoggerConfigurationException {
		String name = null;
		Class<?> cl = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("name".equals(attrName))
				name = attrValue;
			else if ("class".equals(attrName)) {
				try { cl = Class.forName(attrValue); }
				catch (ClassNotFoundException e) {
					throw new LoggerConfigurationException("Unknown class " + attrValue);
				}
			}
		}
		if (name == null)
			throw new LoggerConfigurationException("Missing attribute name on Appender");
		if (cl == null)
			throw new LoggerConfigurationException("Missing attribute class on Appender");
		if (!Appender.class.isAssignableFrom(cl))
			throw new LoggerConfigurationException("Class " + cl.getName() + " is not an Appender");
		Constructor<?> ctor;
		try { ctor = cl.getConstructor(LoggerFactory.class, XMLStreamReader.class, Map.class); }
		catch (NoSuchMethodException e) {
			throw new LoggerConfigurationException("Class " + cl.getName()
				+ " must have a constructor (LoggerFactory,XMLStreamReader,Map<String,Appender>)");
		}
		Appender appender;
		try { appender = (Appender)ctor.newInstance(this, reader, appenders); }
		catch (InvocationTargetException e) {
			Throwable ex = e.getTargetException();
			throw new LoggerConfigurationException("Class constructor " + cl.getName() + " thrown an exception", ex);
		} catch (Exception e) {
			throw new LoggerConfigurationException("Unable to instantiate class " + cl.getName(), e);
		}
		appenders.put(name, appender);
	}
	
	private void readLogger(XMLStreamReader reader, Map<String,Appender> appenders) throws LoggerConfigurationException, XMLStreamException {
		String name = null;
		String appenderName = null;
		Level level = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("name".equals(attrName))
				name = attrValue;
			else if ("appender".equals(attrName))
				appenderName = attrValue;
			else if ("level".equals(attrName))
				try { level = Level.valueOf(attrValue); }
				catch (Exception e) {
					throw new LoggerConfigurationException("Invalid Logger level: " + attrValue);
				}
			else
				throw new LoggerConfigurationException("Unknown attribute " + attrName);
		}
		if (name == null) throw new LoggerConfigurationException("Missing attribute name on Logger");
		if (appenderName == null) throw new LoggerConfigurationException("Missing attribute appender on Logger");

		Appender appender = appenders.get(appenderName);
		if (appender == null) throw new LoggerConfigurationException("Unknown appender name " + appenderName + " for logger " + name);
		configure(name, appender, level);
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
	
	private void readDefault(XMLStreamReader reader, Map<String,Appender> appenders) throws LoggerConfigurationException, XMLStreamException {
		String appenderName = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("appender".equals(attrName))
				appenderName = attrValue;
			else
				throw new LoggerConfigurationException("Unknown attribute " + attrName);
		}

		if (appenderName == null) throw new LoggerConfigurationException("Missing attribute appender on Default");
		Appender appender = appenders.get(appenderName);
		if (appender == null) throw new LoggerConfigurationException("Unknown appender name " + appenderName + " for default logger");
		setDefault(appender);
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
	
}

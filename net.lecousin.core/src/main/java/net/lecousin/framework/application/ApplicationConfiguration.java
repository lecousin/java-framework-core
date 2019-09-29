package net.lecousin.framework.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Application configuration, loaded from a lc-project.xml file.
 */
public class ApplicationConfiguration {

	private String splash;
	private String name;
	private String clazz;
	private Map<String, String> properties = new HashMap<>();
	
	public String getSplash() {
		return splash;
	}

	public String getName() {
		return name;
	}

	public String getClazz() {
		return clazz;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	/** Load the given file. */
	public static ApplicationConfiguration load(File file) throws ApplicationBootstrapException {
		try {
			try (FileInputStream in = new FileInputStream(file)) {
				return load(in);
			}
		} catch (IOException e) {
			throw new ApplicationBootstrapException("Error reading lc-project.xml file", e);
		}
	}
	
	/** Load from the given stream. */
	public static ApplicationConfiguration load(InputStream input) throws ApplicationBootstrapException {
		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES , Boolean.FALSE);
			XMLStreamReader xml = factory.createXMLStreamReader(input);
			while (nextStartElement(xml)) {
				if (!"project".equals(xml.getLocalName()))
					throw new ApplicationBootstrapException("Root element of an lc-project.xml file must be <project>");
				ApplicationConfiguration cfg = new ApplicationConfiguration();
				while (nextStartElement(xml)) {
					if ("application".equals(xml.getLocalName())) {
						cfg.loadApplication(xml);
						return cfg;
					}
				}
				throw new ApplicationBootstrapException("No application element found in lc-project.xml file");
			}
		} catch (XMLStreamException e) {
			throw new ApplicationBootstrapException("Error reading lc-project.xml file", e);
		}
		throw new ApplicationBootstrapException("Nothing found in lc-project.xml file");
	}
	
	private static boolean nextStartElement(XMLStreamReader xml) throws XMLStreamException {
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT)
				return true;
			if (xml.getEventType() == XMLStreamConstants.END_ELEMENT)
				return false;
		}
		return false;
	}
	
	private void loadApplication(XMLStreamReader xml) throws XMLStreamException {
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if ("name".equals(xml.getLocalName()))
					name = xml.getElementText();
				else if ("class".equals(xml.getLocalName()))
					clazz = xml.getElementText();
				else if ("splash".equals(xml.getLocalName()))
					splash = xml.getElementText();
				else if ("properties".equals(xml.getLocalName()))
					loadProperties(xml);
				else
					throw new XMLStreamException("Unknown element <" + xml.getLocalName() + "> in application");
			}
		}
	}
	
	private void loadProperties(XMLStreamReader xml) throws XMLStreamException {
		while (nextStartElement(xml)) {
			String propName = xml.getLocalName();
			String propValue = xml.getElementText();
			properties.put(propName, propValue);
		}
	}
	
}

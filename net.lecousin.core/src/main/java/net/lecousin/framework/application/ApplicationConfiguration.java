package net.lecousin.framework.application;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Application configuration, loaded from a lc-project.xml file.
 */
public class ApplicationConfiguration {

	public String splash;
	public String name;
	public String clazz;
	public Map<String, String> properties = new HashMap<>();
	
	/** Load the given file. */
	public static ApplicationConfiguration load(File file) throws Exception {
		try (FileInputStream in = new FileInputStream(file)) {
			XMLStreamReader xml = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (xml.hasNext()) {
				xml.next();
				if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
					if (!"project".equals(xml.getLocalName()))
						throw new Exception("Root element of an lc-project.xml file must be <project>");
					ApplicationConfiguration cfg = new ApplicationConfiguration();
					boolean found = false;
					while (xml.hasNext()) {
						xml.next();
						if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
							if ("application".equals(xml.getLocalName())) {
								found = true;
								cfg.load(xml);
								break;
							}
						}
					}
					if (!found)
						throw new Exception("No application element found in lc-project.xml file");
					return cfg;
				}
			}
			throw new Exception("Nothing found in lc-project.xml file");
		}
	}
	
	private void load(XMLStreamReader xml) throws Exception {
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if ("name".equals(xml.getLocalName())) {
					name = xml.getElementText();
					continue;
				}
				if ("class".equals(xml.getLocalName())) {
					clazz = xml.getElementText();
					continue;
				}
				if ("splash".equals(xml.getLocalName())) {
					splash = xml.getElementText();
					continue;
				}
				if ("properties".equals(xml.getLocalName())) {
					loadProperties(xml);
					continue;
				}
				throw new Exception("Unknown element <" + xml.getLocalName() + "> in application");
			}
		}
	}
	
	private void loadProperties(XMLStreamReader xml) throws Exception {
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
				String name = xml.getLocalName();
				String value = xml.getElementText();
				properties.put(name, value);
				continue;
			}
		}
	}
	
}

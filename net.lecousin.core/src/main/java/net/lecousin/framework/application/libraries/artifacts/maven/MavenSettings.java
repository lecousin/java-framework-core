package net.lecousin.framework.application.libraries.artifacts.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Information parsed from the settings.xml file for Maven.
 */
public class MavenSettings {

	public String localRepository = null;
	public ArrayList<String> activeProfiles = new ArrayList<>();
	
	/** Load a settings file. */
	public static MavenSettings load(File file) throws Exception {
		try (FileInputStream input = new FileInputStream(file)) {
			return load(input);
		}
	}
	
	/** Load a settings file. */
	public static MavenSettings load(InputStream input) throws Exception {
		XMLStreamReader xml = XMLInputFactory.newFactory().createXMLStreamReader(input);
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (!"settings".equals(xml.getLocalName()))
					throw new Exception("Root element of a Maven settings.xml file must be <settings>");
				MavenSettings settings = new MavenSettings();
				while (xml.hasNext()) {
					xml.next();
					if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
						if ("localRepository".equals(xml.getLocalName())) {
							settings.localRepository = xml.getElementText().trim();
						} else if ("activeProfiles".equals(xml.getLocalName())) {
							while (xml.hasNext()) {
								xml.next();
								if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
									if ("activeProfile".equals(xml.getLocalName()))
										settings.activeProfiles.add(xml.getElementText().trim());
								} else if (xml.getEventType() == XMLStreamConstants.END_ELEMENT)
									break;
							}
						}
					} else if (xml.getEventType() == XMLStreamConstants.END_ELEMENT)
						break;
				}
				settings.activeProfiles.trimToSize();
				return settings;
			}
		}
		return new MavenSettings();
	}
	
}

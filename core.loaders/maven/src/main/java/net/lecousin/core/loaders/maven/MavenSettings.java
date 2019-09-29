package net.lecousin.core.loaders.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.application.libraries.LibraryManagementException;

/**
 * Information parsed from the settings.xml file for Maven.
 */
public class MavenSettings {

	private String localRepository = null;
	private ArrayList<String> activeProfiles = new ArrayList<>();
	
	public String getLocalRepository() {
		return localRepository;
	}
	
	public List<String> getActiveProfiles() {
		return activeProfiles;
	}
	
	/** Load a settings file. */
	public static MavenSettings load(File file) throws IOException, XMLStreamException, LibraryManagementException {
		try (FileInputStream input = new FileInputStream(file)) {
			return load(input);
		}
	}
	
	/** Load a settings file. */
	public static MavenSettings load(InputStream input) throws XMLStreamException, LibraryManagementException {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		XMLStreamReader xml = factory.createXMLStreamReader(input);
		while (xml.hasNext()) {
			xml.next();
			if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (!"settings".equals(xml.getLocalName()))
					throw new LibraryManagementException("Root element of a Maven settings.xml file must be <settings>");
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
								} else if (xml.getEventType() == XMLStreamConstants.END_ELEMENT) {
									break;
								}
							}
						}
					} else if (xml.getEventType() == XMLStreamConstants.END_ELEMENT) {
						break;
					}
				}
				settings.activeProfiles.trimToSize();
				return settings;
			}
		}
		return new MavenSettings();
	}
	
}

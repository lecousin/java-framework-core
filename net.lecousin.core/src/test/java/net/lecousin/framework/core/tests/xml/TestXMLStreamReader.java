package net.lecousin.framework.core.tests.xml;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.xml.XMLStreamReader;

public class TestXMLStreamReader extends TestXMLStreamEventsSync {

	@Override
	protected XMLStreamReader parse(String resource) throws Exception {
		return new XMLStreamReader(LCCore.getApplication().getResource(resource, Task.PRIORITY_NORMAL), 512);
	}
	
}

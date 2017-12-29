package net.lecousin.framework.core.tests.xml;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

public class TestXMLStreamReaderAsync extends TestXMLStreamEventsAsync {

	@Override
	protected XMLStreamReaderAsync parse(String resource) throws Exception {
		return new XMLStreamReaderAsync(LCCore.getApplication().getResource(resource, Task.PRIORITY_NORMAL), 512);
	}
	
}

package net.lecousin.framework.core.tests.xml;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

public class TestXMLStreamReaderAsync extends TestXMLStreamEventsAsync {

	@Override
	protected XMLStreamReaderAsync parse(String resource) throws Exception {
		return new XMLStreamReaderAsync(LCCore.getApplication().getResource(resource, Task.PRIORITY_NORMAL), 512);
	}
	
	@Test
	public void testDocType() throws Exception {
		XMLStreamReaderAsync xml;
		xml = parse("xml-test-suite/mine/003.xml");
		xml.start().blockThrow(0);
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next().blockThrow(0);
		Assert.assertEquals("myPublic", xml.event.publicId.asString());
		Assert.assertEquals("mySystem", xml.event.system.asString());

		xml = parse("xml-test-suite/mine/004.xml");
		xml.start().blockThrow(0);
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next().blockThrow(0);
		Assert.assertEquals("myPublic", xml.event.publicId.asString());
		Assert.assertEquals("mySystem", xml.event.system.asString());
		
		xml = parse("xml-test-suite/mine/005.xml");
		xml.start().blockThrow(0);
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next().blockThrow(0);
		Assert.assertEquals("mySystem", xml.event.system.asString());
		
		xml = parse("xml-test-suite/mine/006.xml");
		xml.start().blockThrow(0);
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next().blockThrow(0);
		Assert.assertEquals("mySystem", xml.event.system.asString());
	}
	
}

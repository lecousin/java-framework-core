package net.lecousin.framework.core.tests.xml;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class TestXMLStreamReader extends TestXMLStreamEventsSync {

	@Override
	protected XMLStreamReader parse(String resource) {
		return new XMLStreamReader(LCCore.getApplication().getResource(resource, Task.PRIORITY_NORMAL), 512, 4);
	}
	
	@Test(timeout=60000)
	public void testDocType() throws Exception {
		XMLStreamReader xml;
		xml = parse("xml-test-suite/mine/003.xml");
		xml.start();
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next();
		Assert.assertEquals("myPublic", xml.event.publicId.asString());
		Assert.assertEquals("mySystem", xml.event.system.asString());

		xml = parse("xml-test-suite/mine/004.xml");
		xml.start();
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next();
		Assert.assertEquals("myPublic", xml.event.publicId.asString());
		Assert.assertEquals("mySystem", xml.event.system.asString());
		
		xml = parse("xml-test-suite/mine/005.xml");
		xml.start();
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next();
		Assert.assertEquals("mySystem", xml.event.system.asString());
		
		xml = parse("xml-test-suite/mine/006.xml");
		xml.start();
		while (!Event.Type.DOCTYPE.equals(xml.event.type)) xml.next();
		Assert.assertEquals("mySystem", xml.event.system.asString());
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=60000)
	public void testStart() throws Exception {
		IO.Readable io = LCCore.getApplication().getResource("xml-test-suite/mine/001.xml", Task.PRIORITY_NORMAL);
		SimpleBufferedReadable bio = new SimpleBufferedReadable(io, 1024);
		XMLStreamReader.start(bio, 1024, 4).blockResult(0);
		bio.close();
		
		XMLStreamReader xml;
		xml = parse("xml-test-suite/mine/003.xml");
		xml.startRootElement();
		Assert.assertEquals("myRoot", xml.event.localName.asString());
	}
	
}

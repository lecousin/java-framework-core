package net.lecousin.framework.core.tests.xml;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

public class TestXMLStreamReaderAsync extends TestXMLStreamEventsAsync {

	@Override
	protected XMLStreamReaderAsync parse(String resource) {
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
	
	@Test
	public void testLongTextAndCData() throws Exception {
		XMLStreamReaderAsync xml;
		xml = parse("xml-test-suite/mine/longText.xml");
		xml.start().blockThrow(0);
		xml.searchElement("testText").blockThrow(0);
		xml.next().blockThrow(0);
		Assert.assertEquals(Event.Type.TEXT, xml.event.type);
		Assert.assertEquals(2008, xml.event.text.length());
		do {
			xml.next().blockThrow(0);
		} while (!Event.Type.CDATA.equals(xml.event.type));
		Assert.assertEquals(2012, xml.event.text.length());

		xml = parse("xml-test-suite/mine/longText.xml");
		xml.start().blockThrow(0);
		xml.setMaximumTextSize(555);
		xml.setMaximumCDataSize(666);
		xml.searchElement("testText").blockThrow(0);
		xml.next().blockThrow(0);
		Assert.assertEquals(Event.Type.TEXT, xml.event.type);
		Assert.assertEquals(555, xml.event.text.length());
		do {
			xml.next().blockThrow(0);
		} while (!Event.Type.CDATA.equals(xml.event.type));
		Assert.assertEquals(666, xml.event.text.length());

		xml = parse("xml-test-suite/mine/longText.xml");
		xml.start().blockThrow(0);
		xml.setMaximumTextSize(5555);
		xml.setMaximumCDataSize(6666);
		xml.searchElement("testText").blockThrow(0);
		xml.next().blockThrow(0);
		Assert.assertEquals(Event.Type.TEXT, xml.event.type);
		Assert.assertEquals(2008, xml.event.text.length());
		do {
			xml.next().blockThrow(0);
		} while (!Event.Type.CDATA.equals(xml.event.type));
		Assert.assertEquals(2012, xml.event.text.length());
	}
	
}

package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.io.IOException;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.xml.XMLException;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamEventsSync;
import net.lecousin.framework.xml.XMLStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class TestXMLStreamReader extends TestXMLStreamEventsSync {

	@Override
	protected XMLStreamReader parse(String resource) {
		return new XMLStreamReader(LCCore.getApplication().getResource(resource, Task.PRIORITY_NORMAL), 512, 4);
	}
	
	@Override
	protected XMLStreamEventsSync parse(IO.Readable io) throws Exception {
		return new XMLStreamReader(io, 512, 4);
	}
	
	@Test
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
	
	@Test
	public void testStart() throws Exception {
		IO.Readable io = LCCore.getApplication().getResource("xml-test-suite/mine/001.xml", Task.PRIORITY_NORMAL);
		SimpleBufferedReadable bio = new SimpleBufferedReadable(io, 1024);
		XMLStreamReader.start(bio, 1024, 4, true).blockResult(0);
		bio.close();
		
		XMLStreamReader xml;
		xml = parse("xml-test-suite/mine/003.xml");
		xml.startRootElement();
		Assert.assertEquals("myRoot", xml.event.localName.asString());
		
		try {
			XMLStreamReader.start(new TestIOError.IOErrorAlways(), 1024, 4, true).blockResult(0);
			throw new AssertionError("Error expected");
		} catch (Exception e) {
			// ok
		}
	}
	
	@Test
	public void testMaxTextSize() throws XMLException, IOException {
		XMLStreamReader xml = parse("xml-test-suite/mine/longText.xml");
		xml.setMaximumCDataSize(15);
		xml.setMaximumTextSize(15);
		xml.startRootElement();
		while (true)
			try { xml.next(); }
			catch (EOFException e) { break; }
		xml = parse("xml-test-suite/mine/longText2.xml");
		xml.setMaximumCDataSize(15);
		xml.setMaximumTextSize(15);
		xml.startRootElement();
		while (true)
			try { xml.next(); }
			catch (EOFException e) { break; }
	}
	
}

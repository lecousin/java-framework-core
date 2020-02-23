package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;
import net.lecousin.framework.xml.XMLStreamEvents.Event;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestXMLStreamReaderAsync extends TestXMLStreamEventsAsync {

	@Parameters(name = "efficient = {0}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>(2);
		list.add(new Object[] { Boolean.TRUE });
		list.add(new Object[] { Boolean.FALSE });
		return list;
	}

	public TestXMLStreamReaderAsync(boolean efficient) {
		this.efficient = efficient;
	}
	
	protected boolean efficient;
	
	@Override
	protected XMLStreamReaderAsync parse(String resource) {
		if (efficient)
			return new XMLStreamReaderAsync(LCCore.getApplication().getResource(resource, Task.Priority.NORMAL), 512, 8);
		IO.Readable io = LCCore.getApplication().getResource(resource, Task.Priority.NORMAL);
		SingleBufferReadable bio = new SingleBufferReadable(io, 2, false);
		return new XMLStreamReaderAsync(bio, 1, 64);
	}
	
	@Override
	protected XMLStreamReaderAsync parse(IO.Readable io) {
		if (efficient)
			return new XMLStreamReaderAsync(io, 512, 8);
		SingleBufferReadable bio = new SingleBufferReadable(io, 2, false);
		return new XMLStreamReaderAsync(bio, 1, 64);
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
		do {
			xml.next().blockThrow(0);
		} while (!Event.Type.COMMENT.equals(xml.event.type));
		Assert.assertEquals(553, xml.event.text.length());

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
		do {
			xml.next().blockThrow(0);
		} while (!Event.Type.COMMENT.equals(xml.event.type));
		Assert.assertEquals(2015, xml.event.text.length());

		xml = parse("xml-test-suite/mine/longText2.xml");
		xml.start().blockThrow(0);
		xml.setMaximumTextSize(25);
		xml.setMaximumCDataSize(25);
		do {
			try { xml.next().blockThrow(0); }
			catch (EOFException e) { break; }
		} while (true);
	}
	
}

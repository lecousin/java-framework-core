package net.lecousin.framework.core.tests.xml;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEventsSync;

public abstract class TestXMLStreamEventsSync extends LCCoreAbstractTest {

	protected abstract XMLStreamEventsSync parse(String resource) throws Exception;
	
	@Test
	public void test1() throws Exception {
		XMLStreamEventsSync xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("myRoot", xml.event.text.asString());
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("hello", xml.event.text.asString());
		ElementContext ctx = xml.event.context.getFirst();
		Assert.assertFalse(xml.nextInnerElement(ctx));
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("translation", xml.event.text.asString());
		ctx = xml.event.context.getFirst();
		Assert.assertTrue(xml.nextInnerElement(ctx));
		Assert.assertEquals("hello", xml.event.text.asString());
		ctx = xml.event.context.getFirst();
		Assert.assertTrue(xml.nextInnerElement(ctx));
		Assert.assertEquals("french", xml.event.text.asString());
		Assert.assertEquals("bonjour", xml.readInnerText().asString());
		xml.nextStartElement();
		Assert.assertEquals("spanish", xml.event.text.asString());
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		Assert.assertTrue(xml.readInnerText().length() == 0);
		Assert.assertTrue(xml.nextInnerElement(ctx));
		Assert.assertEquals("english", xml.event.text.asString());
		Assert.assertTrue(xml.getAttributeByLocalName("same") != null);
		Assert.assertTrue(xml.getAttributeValueByLocalName("same").length() == 0);
		Assert.assertFalse(xml.nextInnerElement(ctx));
	}
	
}

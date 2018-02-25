package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.util.Map;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEventsAsync;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestXMLStreamEventsAsync extends LCCoreAbstractTest {

	protected abstract XMLStreamEventsAsync parse(String resource) throws Exception;
	
	@SuppressWarnings("boxing")
	@Test
	public void test1() throws Exception {
		XMLStreamEventsAsync xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("myRoot", xml.event.text.asString());
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("hello", xml.event.text.asString());
		ElementContext ctx = xml.event.context.getFirst();
		Assert.assertFalse(xml.nextInnerElement(ctx).blockResult(0));
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("translation", xml.event.text.asString());
		ctx = xml.event.context.getFirst();
		Assert.assertTrue(xml.nextInnerElement(ctx).blockResult(0));
		Assert.assertEquals("hello", xml.event.text.asString());
		ctx = xml.event.context.getFirst();
		Assert.assertTrue(xml.nextInnerElement(ctx).blockResult(0));
		Assert.assertEquals("french", xml.event.text.asString());
		Assert.assertEquals("bonjour", xml.readInnerText().blockResult(0).asString());
		xml.nextStartElement();
		Assert.assertEquals("spanish", xml.event.text.asString());
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		Assert.assertTrue(xml.readInnerText().blockResult(0).length() == 0);
		Assert.assertTrue(xml.nextInnerElement(ctx).blockResult(0));
		Assert.assertEquals("english", xml.event.text.asString());
		Assert.assertTrue(xml.getAttributeByLocalName("same") != null);
		Assert.assertTrue(xml.getAttributeValueByLocalName("same").length() == 0);
		Assert.assertFalse(xml.nextInnerElement(ctx).blockResult(0));
	}
	
	@Test
	public void testSearchElement() throws Exception {
		XMLStreamEventsAsync xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.searchElement("spanish").blockThrow(0);
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		try {
			xml.searchElement("german").blockThrow(0);
			throw new AssertionError("EOF expected");
		} catch (EOFException e) {}
	}
	
	@SuppressWarnings("boxing")
	@Test
	public void testGoInto() throws Exception {
		XMLStreamEventsAsync xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("myRoot", xml.event.text.asString());
		ElementContext root = xml.event.context.getFirst();
		Assert.assertTrue(xml.goInto(root, "translation", "hello", "spanish").blockResult(0));
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("myRoot", xml.event.text.asString());
		root = xml.event.context.getFirst();
		Assert.assertFalse(xml.goInto(root, "translation", "hello", "german").blockResult(0));
	}
	
	@Test
	public void testReadInnerElementsText() throws Exception {
		XMLStreamEventsAsync xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.nextStartElement().blockThrow(0);
		Assert.assertEquals("myRoot", xml.event.text.asString());
		ElementContext root = xml.event.context.getFirst();
		xml.goInto(root, "translation", "hello").blockThrow(0);
		Map<String, String> map = xml.readInnerElementsText().blockResult(0);
		Assert.assertEquals("bonjour", map.get("french"));
		Assert.assertEquals("", map.get("spanish"));
		Assert.assertEquals("", map.get("english"));
		Assert.assertEquals(null, map.get("german"));
	}
	
	@Test
	public void test2() throws Exception {
		XMLStreamEventsAsync xml;
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start().blockThrow(0);
		xml.searchElement("translation").blockThrow(0);
		xml.nextStartElement().blockThrow(0); // hello
		xml.closeElement().blockThrow(0);
	}
	
	@Test
	public void testInnerText() throws Exception {
		XMLStreamEventsAsync xml;
		xml = parse("xml-unit-tests/innerText01.xml");
		xml.start().blockThrow(0);
		Assert.assertEquals("Hello World", xml.readInnerText().blockResult(0).trim().asString());
	}
	
	@Test(timeout=120000)
	public void testErrors() {
		for (int i = 1; i <= 15; ++i) {
			String s = Integer.toString(i);
			while (s.length() != 3) s = "0" + s;
			try {
				XMLStreamEventsAsync xml = parse("xml-unit-tests/error/error" + s + ".xml");
				xml.start().blockThrow(0);
				xml.closeElement().blockThrow(0);
				throw new AssertionError("Error expected");
			} catch (Exception err) {
				// ok
			}
		}
	}
}

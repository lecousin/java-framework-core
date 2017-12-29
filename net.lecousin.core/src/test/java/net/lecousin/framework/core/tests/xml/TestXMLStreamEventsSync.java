package net.lecousin.framework.core.tests.xml;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.xml.XMLStreamEvents.Attribute;
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
		Assert.assertEquals("fr", xml.getAttributeValueWithPrefix("language", "code").asString());
		Assert.assertEquals("fr", xml.getAttributeValueWithNamespaceURI("http://language", "code").asString());
		Attribute a = xml.removeAttributeByFullName("language:code");
		Assert.assertEquals("fr", a.value.asString());
		xml.event.attributes.add(a);
		a = xml.removeAttributeWithNamespaceURI("http://language", "code");
		Assert.assertEquals("fr", a.value.asString());
		a = xml.removeAttributeWithNamespaceURI("http://language", "code");
		Assert.assertTrue(null == a);
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
		Assert.assertEquals("hola", xml.getAttributeValueByFullName("value").asString());
		Assert.assertTrue(xml.readInnerText().length() == 0);
		Assert.assertTrue(xml.nextInnerElement(ctx));
		Assert.assertEquals("english", xml.event.text.asString());
		Assert.assertTrue(xml.getAttributeByLocalName("same") != null);
		Assert.assertTrue(xml.getAttributeValueByLocalName("same").length() == 0);
		Assert.assertFalse(xml.nextInnerElement(ctx));
	}
	
	@Test
	public void testSearchElement() throws Exception {
		XMLStreamEventsSync xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertTrue(xml.searchElement("spanish"));
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		xml.closeElement();
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertFalse(xml.searchElement("german"));
	}
	
	@Test
	public void testGoInto() throws Exception {
		XMLStreamEventsSync xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("myRoot", xml.event.text.asString());
		ElementContext root = xml.event.context.getFirst();
		Assert.assertTrue(xml.goInto(root, "translation", "hello", "spanish"));
		Assert.assertEquals("hola", xml.getAttributeValueByLocalName("value").asString());
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("myRoot", xml.event.text.asString());
		root = xml.event.context.getFirst();
		Assert.assertFalse(xml.goInto(root, "translation", "hello", "german"));
	}
	
	@Test
	public void testReadInnerElementsText() throws Exception {
		XMLStreamEventsSync xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("myRoot", xml.event.text.asString());
		ElementContext root = xml.event.context.getFirst();
		Assert.assertTrue(xml.goInto(root, "translation", "hello"));
		Map<String, String> map = xml.readInnerElementsText(xml.event.context.getFirst());
		Assert.assertEquals("bonjour", map.get("french"));
		Assert.assertEquals("", map.get("spanish"));
		Assert.assertEquals("", map.get("english"));
		Assert.assertEquals(null, map.get("german"));
	}
	
	@Test
	public void test2() throws Exception {
		XMLStreamEventsSync xml;
		xml = parse("xml-test-suite/mine/002.xml");
		xml.start();
		xml.searchElement("translation");
		xml.nextStartElement(); // hello
		xml.closeElement();
	}
	
}

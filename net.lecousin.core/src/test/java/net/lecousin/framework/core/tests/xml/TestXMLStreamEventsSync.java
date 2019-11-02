package net.lecousin.framework.core.tests.xml;

import java.util.Map;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.xml.XMLStreamEvents.Attribute;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEventsSync;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestXMLStreamEventsSync extends LCCoreAbstractTest {

	protected abstract XMLStreamEventsSync parse(String resource) throws Exception;
	protected abstract XMLStreamEventsSync parse(IO.Readable io) throws Exception;
	
	@Test
	public void test1() throws Exception {
		XMLStreamEventsSync xml = parse("xml-test-suite/mine/002.xml");
		xml.getMaximumTextSize();
		xml.getMaximumCDataSize();
		xml.start();
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("myRoot", xml.event.text.asString());
		Assert.assertTrue(xml.nextStartElement());
		Assert.assertEquals("hello", xml.event.text.asString());
		Assert.assertEquals("fr", xml.getAttributeValueWithPrefix("language", "code").asString());
		Assert.assertNull(xml.getAttributeValueWithPrefix("language2", "code"));
		Assert.assertNull(xml.getAttributeValueWithPrefix("language", "code2"));
		Assert.assertEquals("fr", xml.getAttributeValueWithNamespaceURI("http://language", "code").asString());
		Assert.assertNull(xml.getAttributeValueWithNamespaceURI("http://language2", "code"));
		Assert.assertNull(xml.getAttributeValueWithNamespaceURI("http://language", "code2"));
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
		Assert.assertNull(xml.getAttributeByFullName("tutu"));
		Assert.assertNull(xml.getAttributeByLocalName("tutu"));
		Assert.assertNull(xml.getAttributeWithPrefix("tutu", "value"));
		Assert.assertNull(xml.getAttributeValueByFullName("tutu"));
		Assert.assertNull(xml.getAttributeValueByLocalName("tutu"));
		Assert.assertNull(xml.getAttributeValueWithPrefix("tutu", "value"));
		Assert.assertNull(xml.getAttributeValueWithNamespaceURI("tutu", "value"));
		Assert.assertNull(xml.removeAttributeByFullName("tutu"));
		Assert.assertNull(xml.removeAttributeWithPrefix("tutu", "value"));
		Assert.assertNull(xml.removeAttributeWithNamespaceURI("tutu", "value"));
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
		Assert.assertFalse(xml.nextInnerElement(xml.event.context.getFirst()));
	}
	
	@Test
	public void testInnerText() throws Exception {
		XMLStreamEventsSync xml;
		xml = parse("xml-unit-tests/innerText01.xml");
		xml.start();
		Assert.assertEquals("Hello World", xml.readInnerText().trim().asString());
	}
	
	public static final int ERROR_START = 1;
	public static final int ERROR_END = 93;
	
	@Test
	public void testErrors() {
		try {
			parse(new TestIOError.IOError1()).start();
			throw new AssertionError("Error expected");
		} catch (Exception err) {
			// ok
		}
		for (int i = ERROR_START; i <= ERROR_END; ++i) {
			String s = Integer.toString(i);
			while (s.length() != 3) s = "0" + s;
			try {
				XMLStreamEventsSync xml = parse("xml-unit-tests/error/error" + s + ".xml");
				xml.start();
				xml.closeElement();
				throw new AssertionError("Error expected");
			} catch (Exception err) {
				// ok
			}
		}
	}
}

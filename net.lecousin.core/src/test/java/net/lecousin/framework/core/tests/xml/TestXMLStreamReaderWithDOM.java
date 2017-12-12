package net.lecousin.framework.core.tests.xml;

import java.util.Collection;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.xml.XMLStreamEventsSync;
import net.lecousin.framework.xml.XMLStreamReader;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestXMLStreamReaderWithDOM extends TestXMLStreamEventsWithDOM<XMLStreamEventsSync> {

	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		return getFiles();
	}
	
	public TestXMLStreamReaderWithDOM(String filepath) {
		super(filepath);
	}

	@Override
	protected XMLStreamEventsSync start(IO.Readable input) throws Exception {
		XMLStreamReader xml = new XMLStreamReader(input, 1024);
		xml.start();
		return xml;
	}
	
	@Override
	protected void next(XMLStreamEventsSync xml) throws Exception {
		xml.next();
	}
	
}

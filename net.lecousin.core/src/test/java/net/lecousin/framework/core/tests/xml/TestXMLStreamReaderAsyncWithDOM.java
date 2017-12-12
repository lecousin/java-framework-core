package net.lecousin.framework.core.tests.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

@RunWith(Parameterized.class)
public class TestXMLStreamReaderAsyncWithDOM extends TestXMLStreamEventsWithDOM<XMLStreamReaderAsync> {

	@Parameters(name = "file = {0}, efficient = {1}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> files = getFiles();
		List<Object[]> params = new ArrayList<>(files.size());
		for (Object[] o : files) {
			params.add(new Object[] { o[0], Boolean.TRUE });
			params.add(new Object[] { o[0], Boolean.FALSE });
		}
		return params;
	}
	
	public TestXMLStreamReaderAsyncWithDOM(String filepath, boolean efficient) {
		super(filepath);
		this.efficient = efficient;
	}
	
	protected boolean efficient;

	@Override
	protected XMLStreamReaderAsync start(IO.Readable input) throws Exception {
		XMLStreamReaderAsync xml;
		if (efficient)
			xml = new XMLStreamReaderAsync(input, 1024);
		else {
			SimpleBufferedReadable bio = new SimpleBufferedReadable(input, 5);
			xml = new XMLStreamReaderAsync(bio, 3);
		}
		xml.start().blockException(0);
		return xml;
	}
	
	@Override
	protected void next(XMLStreamReaderAsync xml) throws Exception {
		xml.next().blockException(0);
	}
	
}

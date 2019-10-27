package net.lecousin.framework.core.tests.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SingleBufferReadable;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamReaderAsync;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestXMLStreamReaderAsyncWithDOM extends TestXMLStreamEventsWithDOM<XMLStreamEventsAsync> {

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
	protected XMLStreamEventsAsync start(IO.Readable input) throws Exception {
		XMLStreamReaderAsync xml;
		if (efficient)
			xml = new XMLStreamReaderAsync(input, 1024, 8);
		else {
			@SuppressWarnings("resource")
			SingleBufferReadable bio = new SingleBufferReadable(input, 2, false);
			xml = new XMLStreamReaderAsync(bio, 1, 64);
		}
		xml.start().blockException(0);
		return xml;
	}
	
	@Override
	protected void next(XMLStreamEventsAsync xml) throws Exception {
		xml.next().blockException(0);
	}
	
}

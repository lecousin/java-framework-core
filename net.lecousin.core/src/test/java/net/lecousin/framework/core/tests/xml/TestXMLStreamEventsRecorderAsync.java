package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.xml.XMLStreamEventsAsync;
import net.lecousin.framework.xml.XMLStreamEventsRecorder;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestXMLStreamEventsRecorderAsync extends TestXMLStreamReaderAsyncWithDOM {

	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		return getFiles();
	}
	
	public TestXMLStreamEventsRecorderAsync(String filepath) {
		super(filepath, true);
	}
	
	 @Override
	protected XMLStreamEventsAsync start(Readable input) throws Exception {
		 XMLStreamEventsAsync reader = super.start(input);
		 XMLStreamEventsRecorder.Async recorder = new XMLStreamEventsRecorder.Async(reader);
		 recorder.startRecording(true);
		 while (true) {
			 try { recorder.next().blockException(0); }
			 catch (EOFException e) { break; }
		 }
		 recorder.stopRecording();
		 recorder.replay();
		 return recorder;
	}
	
}

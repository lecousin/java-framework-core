package net.lecousin.framework.core.tests.xml;

import java.io.EOFException;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.xml.XMLStreamEventsRecorder;
import net.lecousin.framework.xml.XMLStreamEventsSync;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestXMLStreamEventsRecorderSync extends TestXMLStreamReaderWithDOM {

	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		return getFiles();
	}
	
	public TestXMLStreamEventsRecorderSync(String filepath) {
		super(filepath);
	}
	
	 @Override
	protected XMLStreamEventsSync start(Readable input) throws Exception {
		 XMLStreamEventsSync reader = super.start(input);
		 XMLStreamEventsRecorder.Sync recorder = new XMLStreamEventsRecorder.Sync(reader);
		 recorder.startRecording(true);
		 while (true) {
			 try { recorder.next(); }
			 catch (EOFException e) { break; }
		 }
		 recorder.stopRecording();
		 recorder.replay();
		 return recorder;
	}
	
}

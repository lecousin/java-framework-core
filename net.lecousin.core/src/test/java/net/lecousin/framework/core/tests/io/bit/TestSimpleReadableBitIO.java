package net.lecousin.framework.core.tests.io.bit;

import net.lecousin.framework.core.test.io.bit.TestBitIOReadable;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.bit.SimpleReadableBitIO;

import org.junit.runner.RunWith;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestSimpleReadableBitIO extends TestBitIOReadable {

	public TestSimpleReadableBitIO(byte[] bytes, boolean[] bits) {
		super(bytes, bits);
	}
	
	@Override
	protected SimpleReadableBitIO createLittleEndian(IO.Readable.Buffered io) {
		return new SimpleReadableBitIO.LittleEndian(io);
	}
	
	@Override
	protected SimpleReadableBitIO createBigEndian(IO.Readable.Buffered io) {
		return new SimpleReadableBitIO.BigEndian(io);
	}
	
}

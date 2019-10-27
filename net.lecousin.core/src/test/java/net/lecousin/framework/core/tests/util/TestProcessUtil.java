package net.lecousin.framework.core.tests.util;

import java.io.IOException;

import org.junit.Test;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.ProcessUtil;

public class TestProcessUtil extends LCCoreAbstractTest {

	@Test
	public void test() throws IOException {
		ProcessBuilder pb = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-version");
		Process p = pb.start();
		Async<Exception> sp = new Async<>();
		ProcessUtil.onProcessExited(p, (val) -> { sp.unblock(); });
		Console c = LCCore.getApplication().getConsole();
		ProcessUtil.consumeProcessConsole(p, (line) -> { c.out(line); }, (line) -> { c.err(line); });
		sp.block(0);
	}
	
}

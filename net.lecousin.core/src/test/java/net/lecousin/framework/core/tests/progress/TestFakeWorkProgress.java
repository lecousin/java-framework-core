package net.lecousin.framework.core.tests.progress;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.progress.FakeWorkProgress;

import org.junit.Test;

public class TestFakeWorkProgress extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		FakeWorkProgress p = new FakeWorkProgress();
		p.getPosition();
		p.getAmount();
		p.getRemainingWork();
		p.getText();
		p.getSubText();
		p.progress(0);
		p.done();
		p.error(new Exception());
		p.cancel(new CancelException("test"));
		p.getSynch();
		p.setAmount(0);
		p.setPosition(0);
		p.setText("");
		p.setSubText("");
		p.listen(() -> {});
		p.unlisten(() -> {});
	}
	
}

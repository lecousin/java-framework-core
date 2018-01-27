package net.lecousin.framework.core.tests.exception;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.AlreadyExistsException;
import net.lecousin.framework.exception.InvalidException;
import net.lecousin.framework.exception.LocalizableException;

import org.junit.Assert;
import org.junit.Test;

public class TestExceptions extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		Assert.assertEquals("Fichier 'test' existe déjà", new AlreadyExistsException("b", "File", "test").getLocalizable().localize("fr").blockResult(0));
		Assert.assertEquals("Fichier invalide: test", new InvalidException("b", "File", "test").getLocalizable().localize("fr").blockResult(0));
		new LocalizableException("b", "file").getMessage();
		new LocalizableException("message", new Exception()).getMessage();
		new LocalizableException((String)null).getMessage();
	}
	
}

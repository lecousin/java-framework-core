package net.lecousin.framework.core.tests.concurrent.util;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncConsumer extends LCCoreAbstractTest {

	@Test
	public void testSimple() throws Exception {
		AsyncConsumer.Simple<Integer, Exception> c = new AsyncConsumer.Simple<Integer, Exception>() {
			@Override
			public IAsync<Exception> consume(Integer data) {
				return new Async<>(true);
			}
		};
		c.consume(Integer.valueOf(0));
		c.end();
		c.error(new Exception());
	}
	
	@Test
	public void testError() throws Exception {
		AsyncConsumer.Error<Integer, Exception> c = new AsyncConsumer.Error<>(new Exception("ok"));
		IAsync<Exception> a;
		a = c.consume(Integer.valueOf(0));
		Assert.assertTrue(a.hasError());
		Assert.assertEquals("ok", a.getError().getMessage());
		a = c.end();
		Assert.assertTrue(a.hasError());
		Assert.assertEquals("ok", a.getError().getMessage());
		c.error(new Exception());
	}

	@Test
	public void testConverter() throws Exception {
		MutableInteger mi = new MutableInteger(0);
		AsyncConsumer.Simple<Integer, Exception> ci = new AsyncConsumer.Simple<Integer, Exception>() {
			@Override
			public IAsync<Exception> consume(Integer data) {
				mi.set(data.intValue());
				return new Async<>(true);
			}
		};
		AsyncConsumer<Long, Exception> cl = ci.convert(l -> Integer.valueOf(l.intValue()));
		Assert.assertEquals(0, mi.get());
		cl.consume(Long.valueOf(51)).blockThrow(0);
		Assert.assertEquals(51, mi.get());
		cl.end().blockThrow(0);
		cl.error(new Exception());
	}
	
}

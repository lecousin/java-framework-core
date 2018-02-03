package net.lecousin.framework.core.tests.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TestPair extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		Pair<Object, Object> p;
		p = new Pair<>(Integer.valueOf(10), Integer.valueOf(20));
		Assert.assertEquals(Integer.valueOf(10), p.getValue1());
		Assert.assertEquals(Integer.valueOf(20), p.getValue2());
		p.setValue1(Integer.valueOf(100));
		Assert.assertEquals(Integer.valueOf(100), p.getValue1());
		p.setValue2(Integer.valueOf(200));
		Assert.assertEquals(Integer.valueOf(200), p.getValue2());
		Assert.assertTrue(p.equals(new Pair<>(Integer.valueOf(100), Integer.valueOf(200))));
		Assert.assertFalse(p.equals(new Pair<>(Integer.valueOf(100), Integer.valueOf(201))));
		Assert.assertFalse(p.equals(new Pair<>(Integer.valueOf(101), Integer.valueOf(200))));
		Assert.assertFalse(p.equals(null));
		Assert.assertFalse(p.equals(new Object()));
		p.hashCode();
		p.toString();
		new Pair<>(null, null).hashCode();
		new Pair<>(null, null).toString();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(new Pair<>(Integer.valueOf(10), "test"));
		oout.close();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ObjectInputStream oin = new ObjectInputStream(in);
		Assert.assertEquals(new Pair<>(Integer.valueOf(10), "test"), oin.readObject());
		oin.close();
	}
	
}

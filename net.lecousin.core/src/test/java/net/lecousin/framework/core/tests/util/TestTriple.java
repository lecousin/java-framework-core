package net.lecousin.framework.core.tests.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Triple;

public class TestTriple extends LCCoreAbstractTest {

	@Test
	public void test() throws IOException, ClassNotFoundException {
		Triple<Object, Object, Object> t;
		t = new Triple<>(Integer.valueOf(10), Integer.valueOf(20), Integer.valueOf(30));
		Assert.assertEquals(Integer.valueOf(10), t.getValue1());
		Assert.assertEquals(Integer.valueOf(20), t.getValue2());
		Assert.assertEquals(Integer.valueOf(30), t.getValue3());
		t.setValue1(Integer.valueOf(100));
		Assert.assertEquals(Integer.valueOf(100), t.getValue1());
		t.setValue2(Integer.valueOf(200));
		Assert.assertEquals(Integer.valueOf(200), t.getValue2());
		t.setValue3(Integer.valueOf(300));
		Assert.assertEquals(Integer.valueOf(300), t.getValue3());
		Assert.assertTrue(t.equals(new Triple<>(Integer.valueOf(100), Integer.valueOf(200), Integer.valueOf(300))));
		Assert.assertFalse(t.equals(new Triple<>(Integer.valueOf(101), Integer.valueOf(200), Integer.valueOf(300))));
		Assert.assertFalse(t.equals(new Triple<>(Integer.valueOf(100), Integer.valueOf(201), Integer.valueOf(300))));
		Assert.assertFalse(t.equals(new Triple<>(Integer.valueOf(100), Integer.valueOf(200), Integer.valueOf(301))));
		Assert.assertFalse(t.equals(null));
		Assert.assertFalse(t.equals(new Object()));
		Assert.assertFalse(t.equals(new Triple<>(null, Integer.valueOf(200), Integer.valueOf(300))));
		Assert.assertFalse(t.equals(new Triple<>(Integer.valueOf(100), null, Integer.valueOf(300))));
		Assert.assertFalse(t.equals(new Triple<>(Integer.valueOf(100), Integer.valueOf(200), null)));
		t.hashCode();
		t.toString();
		new Triple<>(null, null, null).hashCode();
		new Triple<>(null, null, null).toString();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(new Triple.Serializable<>(Integer.valueOf(10), "test", Boolean.TRUE));
		oout.close();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ObjectInputStream oin = new ObjectInputStream(in);
		Assert.assertEquals(new Triple<>(Integer.valueOf(10), "test", Boolean.TRUE), oin.readObject());
		oin.close();
	}
	
}

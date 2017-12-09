package net.lecousin.framework.core.test.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.serialization.Deserializer;
import net.lecousin.framework.io.serialization.Serializer;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.annotations.Transient;
import net.lecousin.framework.util.ClassUtil;

public abstract class TestSerialization extends LCCoreAbstractTest {

	protected abstract Serializer createSerializer();
	protected abstract Deserializer createDeserializer();
	
	/** Structure to test booleans. */
	public static class TestBooleans {
		public boolean attr1 = false;
		public boolean attr2 = true;
		public Boolean attr3 = Boolean.FALSE;
		public Boolean attr4 = Boolean.TRUE;
		public Boolean attr5 = Boolean.FALSE;
	}
	
	public static TestBooleans createBooleans() {
		TestBooleans b = new TestBooleans();
		b.attr1 = true;
		b.attr2 = false;
		b.attr3 = Boolean.TRUE;
		b.attr4 = Boolean.FALSE;
		return b;
	}
	
	@Test
	public void testBooleans() throws Exception {
		test(createBooleans(), TestBooleans.class);
	}
	
	public static class TestNumbers {
		public byte b1 = 2;
		public Byte b2 = Byte.valueOf((byte)-11);
		public short s1 = 51;
		public Short s2 = Short.valueOf((short)-23);
		public int i1 = -111;
		public Integer i2 = Integer.valueOf(222);
		public long l1 = 1234567L;
		public Long l2 = Long.valueOf(-987654321L);
		public float f1 = 0.0123f;
		public Float f2 = Float.valueOf(-9.87654321f);
		public double d1 = -1.112233d;
		public Double d2 = Double.valueOf(9.887766d);
		public BigInteger bi1 = new BigInteger("1234567890");
		public BigInteger bi2 = new BigInteger("-987654321");
		public BigDecimal bd1 = new BigDecimal("0.112233445566778899");
		public BigDecimal bd2 = new BigDecimal("-1.998877665544332211");
	}
	
	public static TestNumbers createNumbers() {
		TestNumbers n = new TestNumbers();
		n.b1 = -45;
		n.b2 = Byte.valueOf((byte)67);
		n.s1 = -15;
		n.s2 = Short.valueOf((short)32);
		n.i1 = 333;
		n.i2 = Integer.valueOf(-444);
		n.l1 = -1234567890L;
		n.l2 = Long.valueOf(9876543210L);
		n.f1 = -0.00112233f;
		n.f2 = Float.valueOf(9.88776655f);
		n.d1 = 2.33445566d;
		n.d2 = Double.valueOf(-99.88777666d);
		n.bi1 = new BigInteger("-9876543210");
		n.bi2 = new BigInteger("51234567890");
		n.bd1 = new BigDecimal("-0.00112233445566778899");
		n.bd2 = new BigDecimal("3.998877665544332211");
		return n;
	}

	@Test
	public void testNumbers() throws Exception {
		test(createNumbers(), TestNumbers.class);
	}
	
	
	public static class TestString {
		public String str = "1";
	}
	
	public void testString(String s) throws Exception {
		TestString ts = new TestString();
		ts.str = s;
		test(ts, TestString.class);
	}
	
	@Test
	public void testStrings() throws Exception {
		testString("hello");
		testString("123");
		testString("a\tb\rc\nd\be\\fg\"hi\'jk&#{([-|_@)]=+}£$*%!:/;.,?<012>34");
	}
	
	
	public static class TestChar {
		public char c = '1';
		public Character C = Character.valueOf('2');
	}
	
	public void testChar(char c) throws Exception {
		TestChar tc = new TestChar();
		tc.c = c;
		tc.C = c;
		test(tc, TestChar.class);
	}
	
	@Test
	public void testChars() throws Exception {
		testChar('0');
		testChar('3');
		testChar('c');
		testChar('R');
		testChar('&');
		testChar('#');
		testChar('\'');
		testChar('"');
		testChar('\\');
		testChar('$');
		testChar('%');
		testChar('.');
		testChar('?');
		testChar(':');
		testChar('/');
		testChar('<');
		testChar('>');
		testChar('!');
		testChar('\n');
		testChar('\r');
		testChar('\t');
		testChar('\b');
		testChar('\f');
	}
	
	public static class TestSimpleObjects {
		public TestBooleans booleans;
		public int i = 51;
		public TestNumbers numbers;
		public String s = "hello";
		public TestString string;
		public TestChar ch;
	}
	
	@Test
	public void testSimpleObjects() throws Exception {
		TestSimpleObjects o = new TestSimpleObjects();
		o.booleans = createBooleans();
		o.i = 52;
		o.numbers = createNumbers();
		o.s = "world";
		o.string = new TestString();
		o.string.str = "a string";
		o.ch = new TestChar();
		o.ch.c = 'o';
		o.ch.C = Character.valueOf('p');
		test(o, TestSimpleObjects.class);
		// test with null values
		o = new TestSimpleObjects();
		o.i = 53;
		o.s = "s";
		test(o, TestSimpleObjects.class);
	}
	
	public static class TestLists {
		public List<Boolean> booleans;
		public List<Integer> integers;
		public List<Float> floats;
		public List<String> strings;
		public List<Character> characters;
		public List<TestBooleans> testBooleans;
		public List<TestNumbers> testNumbers;
	}
	
	@Test
	public void testLists() throws Exception {
		TestLists t;
		t = new TestLists();
		t.booleans = Arrays.asList(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
		t.integers = Arrays.asList(Integer.valueOf(12), Integer.valueOf(-98), Integer.valueOf(18347));
		t.floats = Arrays.asList(Float.valueOf(0.1234f), Float.valueOf(-823.674f), Float.valueOf(11.22f));
		t.strings = Arrays.asList("hello", "world", "!!!");
		t.characters = Arrays.asList(Character.valueOf('H'), Character.valueOf('e'), Character.valueOf('L'), Character.valueOf('l'), Character.valueOf('O'));
		t.testBooleans = Arrays.asList(createBooleans(), new TestBooleans(), null);
		t.testNumbers = Arrays.asList(createNumbers(), new TestNumbers(), null, createNumbers());
		test(t, TestLists.class);
	}
	
	public static class TestWithTransient {
		public boolean b1 = true;
		public transient boolean b2 = false;
		public int i1 = 10;
		@Transient
		public int i2 = 20;
	}
	public static class TestWithoutTransient {
		public boolean b1 = true;
		public boolean b2 = false;
		public int i1 = 10;
		public int i2 = 20;
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testTransient() throws Exception {
		TestWithTransient t1 = new TestWithTransient();
		t1.b1 = false;
		t1.b2 = true;
		t1.i1 = 99;
		t1.i2 = 88;
		MemoryIO io = serialize(t1);
		print(io, t1);
		TestWithTransient t2 = deserialize(io, TestWithTransient.class);
		Assert.assertFalse(t2.b1);
		Assert.assertFalse(t2.b2);
		Assert.assertEquals(99, t2.i1);
		Assert.assertEquals(20, t2.i2);
		TestWithoutTransient t3 = new TestWithoutTransient();
		t3.b1 = false;
		t3.b2 = true;
		t3.i1 = 99;
		t3.i2 = 88;
		io = serialize(t3);
		print(io, t3);
		TestWithTransient t4 = deserialize(io, TestWithTransient.class);
		Assert.assertFalse(t4.b1);
		Assert.assertFalse(t4.b2);
		Assert.assertEquals(99, t4.i1);
		Assert.assertEquals(20, t4.i2);
	}
	
	protected <T> void test(T object, Class<T> type) throws Exception {
		@SuppressWarnings("resource")
		MemoryIO io = serialize(object);
		print(io, object);
		T o2 = deserialize(io, type);
		check(object, o2);
	}
	
	protected MemoryIO serialize(Object o) throws Exception {
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> res = s.serialize(o, new TypeDefinition(o.getClass()), io, new ArrayList<>(0));
		res.block(0);
		if (res.hasError()) throw res.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T deserialize(MemoryIO io, Class<T> type) throws Exception {
		Deserializer d = createDeserializer();
		AsyncWork<Object, Exception> res = d.deserialize(new TypeDefinition(type), io, new ArrayList<>(0));
		res.block(0);
		if (res.hasError()) throw res.getError();
		return (T)res.getResult();
	}
	
	protected void checkValue(Object expected, Object found) throws Exception {
		if (expected == null || found == null) {
			Assert.assertEquals(expected, found);
			return;
		}
		Class<?> type = expected.getClass();
		if (type.isPrimitive() ||
			Number.class.isAssignableFrom(type) ||
			CharSequence.class.isAssignableFrom(type) ||
			Boolean.class.equals(type) ||
			Character.class.equals(type)) {
			Assert.assertEquals(expected, found);
			return;
		}
		if (List.class.isAssignableFrom(type)) {
			checkList((List<?>)expected, (List<?>)found);
			return;
		}
		check(expected, found);
	}
	
	protected void check(Object expected, Object found) throws Exception {
		Assert.assertEquals(expected.getClass(), found.getClass());
		for (Field f : ClassUtil.getAllFields(expected.getClass())) {
			if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
			Object o1 = f.get(expected);
			Object o2 = f.get(found);
			checkValue(o1, o2);
		}
	}
	
	protected void checkList(List<?> l1, List<?> l2) throws Exception {
		Assert.assertEquals(l1.size(), l2.size());
		Iterator<?> it1 = l1.iterator();
		Iterator<?> it2 = l2.iterator();
		while (it1.hasNext())
			checkValue(it1.next(), it2.next());
	}
	
	protected void print(MemoryIO io, Object o) throws Exception {
		String content = IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_8);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		System.out.println("Serialization result for " + o.getClass().getName() + "\r\n" + content);
	}
	
}

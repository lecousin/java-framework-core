package net.lecousin.framework.core.test.serialization;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
	
	@SuppressWarnings("resource")
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

	@SuppressWarnings("resource")
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
	
	protected <T> void test(T object, Class<T> type) throws Exception {
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
	
	protected void check(Object expected, Object found) throws Exception {
		Assert.assertEquals(expected.getClass(), found.getClass());
		for (Field f : ClassUtil.getAllFields(expected.getClass())) {
			Object o1 = f.get(expected);
			Object o2 = f.get(found);
			if (o1 == null || o2 == null) {
				Assert.assertEquals(f.get(expected), f.get(found));
				continue;
			}
			Class<?> type = o1.getClass();
			if (type.isPrimitive() ||
				Number.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Boolean.class.equals(type) ||
				Character.class.equals(type)) {
				Assert.assertEquals(f.get(expected), f.get(found));
				continue;
			}
			check(o1, o2);
		}
	}
	
	protected void print(MemoryIO io, Object o) throws Exception {
		String content = IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_8);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		System.out.println("Serialization result for " + o.getClass().getName() + "\r\n" + content);
	}
	
}

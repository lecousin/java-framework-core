package net.lecousin.framework.core.test.serialization;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.Deserializer;
import net.lecousin.framework.io.serialization.SerializationContext.AttributeContext;
import net.lecousin.framework.io.serialization.Serializer;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.annotations.Instantiate;
import net.lecousin.framework.io.serialization.annotations.SerializationMethods;
import net.lecousin.framework.io.serialization.annotations.SerializationName;
import net.lecousin.framework.io.serialization.annotations.Transient;
import net.lecousin.framework.io.serialization.annotations.TypeSerializationMethod;
import net.lecousin.framework.io.serialization.annotations.TypeSerializer;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.Factory;
import net.lecousin.framework.util.UnprotectedStringBuffer;

import org.junit.Assert;
import org.junit.Test;

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
		test(Boolean.TRUE, Boolean.class);
		test(Boolean.FALSE, Boolean.class);
		test(createBooleans(), TestBooleans.class);
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testBooleanPrimitiveTrue() throws Exception {
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> r1 = s.serialize(Boolean.TRUE, new TypeDefinition(boolean.class), io, new ArrayList<>(0));
		r1.block(0);
		if (r1.hasError()) throw r1.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Deserializer d = createDeserializer();
		AsyncWork<Object, Exception> r2 = d.deserialize(new TypeDefinition(boolean.class), io, new ArrayList<>(0));
		r2.block(0);
		Assert.assertEquals(Boolean.TRUE, r2.getResult());
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testBooleanPrimitiveFalse() throws Exception {
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> r1 = s.serialize(Boolean.FALSE, new TypeDefinition(boolean.class), io, new ArrayList<>(0));
		r1.block(0);
		if (r1.hasError()) throw r1.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Deserializer d = createDeserializer();
		AsyncWork<Object, Exception> r2 = d.deserialize(new TypeDefinition(boolean.class), io, new ArrayList<>(0));
		r2.block(0);
		Assert.assertEquals(Boolean.FALSE, r2.getResult());
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
		test(Byte.valueOf((byte)0), Byte.class);
		test(Byte.valueOf((byte)1), Byte.class);
		test(Byte.valueOf((byte)123), Byte.class);
		test(Byte.valueOf((byte)-1), Byte.class);
		test(Byte.valueOf((byte)-123), Byte.class);
		test(Short.valueOf((short)0), Short.class);
		test(Short.valueOf((short)10), Short.class);
		test(Short.valueOf((short)-2340), Short.class);
		test(Integer.valueOf(0), Integer.class);
		test(Integer.valueOf(-12345), Integer.class);
		test(Integer.valueOf(54321), Integer.class);
		test(Long.valueOf(0), Long.class);
		test(Long.valueOf(123456789L), Long.class);
		test(Long.valueOf(987654321L), Long.class);
		test(Float.valueOf(0f), Float.class);
		test(Float.valueOf(450.678f), Float.class);
		test(Float.valueOf(-0.0000111f), Float.class);
		test(Double.valueOf(0d), Double.class);
		test(Double.valueOf(1122330d), Double.class);
		test(Double.valueOf(-1.234567890d), Double.class);
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

	public static class TestIString {
		public UnprotectedStringBuffer str;
	}

	public void testIString(String s) throws Exception {
		TestIString ts = new TestIString();
		ts.str = new UnprotectedStringBuffer(s);
		test(ts, TestIString.class);
	}
	
	@Test
	public void testStrings() throws Exception {
		test("Hello World!", String.class);
		test("", String.class);
		testString("hello");
		testString("123");
		testString("a\tb\rc\nd\be\\fg\"hi\'jk&#{([-|_@)]=+}£$*%!:/;.,?<012>34");
		testIString("hello");
		testIString("123");
		testIString("a\tb\rc\nd\be\\fg\"hi\'jk&#{([-|_@)]=+}£$*%!:/;.,?<012>34");
	}
	
	
	public static class TestChar {
		public char c = '1';
		public Character C = Character.valueOf('2');
	}
	
	public void testChar(char c) throws Exception {
		test(Character.valueOf('A'), Character.class);
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
	
	public static enum Enum1 { VAL1, VAL2, VAL3 };
	public static enum Enum2 { VAL11, VAL22, VAL33 };
	
	@Test
	public void testEnum() throws Exception {
		test(Enum1.VAL2, Enum1.class);
		test(Enum2.VAL33, Enum2.class);
	}
	
	public static class TestSimpleObjects {
		public TestBooleans booleans;
		public int i = 51;
		public TestNumbers numbers;
		public String s = "hello";
		public TestString string;
		public TestChar ch;
		public Enum1 e1;
		public Enum2 e2;
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
		o.e1 = Enum1.VAL3;
		o.e2 = null;
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
	
	public static class TestArrays {
		public boolean[] b1;
		public Boolean[] b2;
		public Integer[] i;
		public long[] l;
		public Float[] f;
		public double[] d;
		public String[] strings;
		public char[] chars;
		public byte[] bytes;
		public TestBooleans[] testBooleans;
		public TestNumbers[] testNumbers;
	}
	
	@Test
	public void testArrays() throws Exception {
		TestArrays t = new TestArrays();
		t.b1 = new boolean[] { true, true, false, true, false, false };
		t.b2 = new Boolean[] { Boolean.FALSE, Boolean.TRUE };
		t.i = new Integer[] { Integer.valueOf(55), Integer.valueOf(-123), Integer.valueOf(12345) };
		t.l = new long[] { 1111, -2222, 345, -678 };
		t.f = new Float[] { Float.valueOf(-0.01234f), Float.valueOf(9.87654f) };
		t.d = new double[] { 11.223344d, -99.887766 };
		t.strings = new String[] { "Hello", "World", "!", "Salut\ntoi" };
		t.chars = new char[] { 'Q', '&', '<', '=', '"', '\n', '\'', ']' };
		t.bytes = new byte[] { 1, 2, 3, 4, 5, 6 };
		t.testBooleans = new TestBooleans[] { new TestBooleans(), createBooleans() };
		t.testNumbers = new TestNumbers[] { createNumbers(), new TestNumbers() };
		test(t, TestArrays.class);
		test(new int[] { 11, 33, 55, 77, 99 }, int[].class);
	}
	
	public static class TestListOfList {
		public List<List<List<Integer>>> list;
	}
	
	@Test
	public void testListOfList() throws Exception {
		TestListOfList t = new TestListOfList();
		t.list = Arrays.asList(
			Arrays.asList(
				Arrays.asList(Integer.valueOf(123), Integer.valueOf(456))
			),
			new ArrayList<>(),
			Arrays.asList(
				Arrays.asList(Integer.valueOf(987), Integer.valueOf(654), Integer.valueOf(321))
			),
			new LinkedList<>()
		);
		test(t, TestListOfList.class);
	}
	
	public static interface MyInterface {}
	
	public static class MyImplementation implements MyInterface {}
	public static class MyImplementation2 implements MyInterface {}
	
	public static class MyContainerOfAbstracts {
		public MyInterface interf;
		@Instantiate(factory=MyImplementation2AttributeFactory.class)
		public MyInterface interf2;
	}
	
	public static class MyImplementation2AttributeFactory implements Factory<MyInterface, AttributeContext> {
		@Override
		public MyInterface create(AttributeContext discriminator) {
			return new MyImplementation2();
		}
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testInstantiation() throws Exception {
		MyImplementation impl = new MyImplementation();
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> res = s.serialize(impl, new TypeDefinition(MyInterface.class), io, new ArrayList<>(0));
		res.block(0);
		if (res.hasError()) throw res.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		print(io, impl);
		MyInterface interf = deserialize(io, MyInterface.class);
		Assert.assertEquals(MyImplementation.class, interf.getClass());
		MyContainerOfAbstracts t = new MyContainerOfAbstracts();
		t.interf = new MyImplementation();
		t.interf2 = new MyImplementation();
		io = serializeInMemory(t);
		print(io, t);
		MyContainerOfAbstracts t2 = deserialize(io, MyContainerOfAbstracts.class);
		Assert.assertEquals(MyImplementation.class, t2.interf.getClass());
		Assert.assertEquals(MyImplementation2.class, t2.interf2.getClass());
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
		MemoryIO io = serializeInMemory(t1);
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
		io = serializeInMemory(t3);
		print(io, t3);
		TestWithTransient t4 = deserialize(io, TestWithTransient.class);
		Assert.assertFalse(t4.b1);
		Assert.assertFalse(t4.b2);
		Assert.assertEquals(99, t4.i1);
		Assert.assertEquals(20, t4.i2);
	}
	
	public static class TestRename1 {
		@SerializationName("world")
		public String hello;
	}
	
	public static class TestRename2 {
		public String world;
	}
	
	@Test
	public void testRename() throws Exception {
		TestRename1 t1 = new TestRename1();
		t1.hello = "bonjour";
		test(t1, TestRename1.class);
		@SuppressWarnings("resource")
		MemoryIO ioMem = serializeInMemory(t1);
		TestRename2 t2 = deserialize(ioMem, TestRename2.class);
		Assert.assertEquals("bonjour", t2.world);
	}
	
	public static class TestNoDefaultConstructor1 {
		public TestNoDefaultConstructor1(String value) {
			this.value = value;
		}
		
		public String value;
		
		@Override
		public String toString() {
			return value;
		}
	}
	
	public static class TestNoDefaultConstructor1Serializer implements CustomSerializer {
		@Override
		public TypeDefinition sourceType() {
			return new TypeDefinition(TestNoDefaultConstructor1.class);
		}
		@Override
		public TypeDefinition targetType() {
			return new TypeDefinition(String.class);
		}
		@Override
		public Object serialize(Object src, Object containerInstance) {
			return ((TestNoDefaultConstructor1)src).value;
		}
		@Override
		public Object deserialize(Object src, Object containerInstance) {
			return new TestNoDefaultConstructor1((String)src);
		}
	}
	
	@TypeSerializer(TestNoDefaultConstructor1Serializer.class)
	public static class TestTypeSerializer1 {
		public TestNoDefaultConstructor1 test;
	}
	
	@Test
	public void testTypeSerializer() throws Exception {
		TestTypeSerializer1 t = new TestTypeSerializer1();
		t.test = new TestNoDefaultConstructor1("Hello");
		test(t, TestTypeSerializer1.class);
	}
	
	public static class TestTypeSerializationMethod {
		@TypeSerializationMethod("toString")
		public TestNoDefaultConstructor1 test;
	}

	@Test
	public void testTypeSerializationMethod() throws Exception {
		TestTypeSerializationMethod t = new TestTypeSerializationMethod();
		t.test = new TestNoDefaultConstructor1("World");
		test(t, TestTypeSerializationMethod.class);
	}
	
	public static class TestSerializationMethods {
		@SerializationMethods(serialization="testToString", deserialization="testFromString")
		public TestNoDefaultConstructor1 test;
		
		public String testToString() { return test.value; }
		public TestNoDefaultConstructor1 testFromString(String s) { return new TestNoDefaultConstructor1(s); }
	}

	@Test
	public void testSerializationMethods() throws Exception {
		TestSerializationMethods t = new TestSerializationMethods();
		t.test = new TestNoDefaultConstructor1("Hello World!");
		test(t, TestSerializationMethods.class);
	}
	
	
	protected <T> void test(T object, Class<T> type) throws Exception {
		@SuppressWarnings("resource")
		MemoryIO ioMem = serializeInMemory(object);
		print(ioMem, object);
		T o2 = deserialize(ioMem, type);
		check(object, o2);
		@SuppressWarnings("resource")
		FileIO.ReadWrite ioFile = serializeInFile(object);
		print(ioFile, object);
		o2 = deserialize(ioFile, type);
		check(object, o2);
	}
	
	protected MemoryIO serializeInMemory(Object o) throws Exception {
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> res = s.serialize(o, new TypeDefinition(o.getClass()), io, new ArrayList<>(0));
		res.block(0);
		if (res.hasError()) throw res.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
	protected FileIO.ReadWrite serializeInFile(Object o) throws Exception {
		File tmp = File.createTempFile("test", "serialization");
		FileIO.ReadWrite io = new FileIO.ReadWrite(tmp, Task.PRIORITY_NORMAL);
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> res = s.serialize(o, new TypeDefinition(o.getClass()), io, new ArrayList<>(0));
		res.block(0);
		if (res.hasError()) throw res.getError();
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		return io;
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T deserialize(IO.Readable io, Class<T> type) throws Exception {
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
		if (type.isArray()) {
			checkArray(expected, found);
			return;
		}
		if (type.isPrimitive() ||
			Number.class.isAssignableFrom(type) ||
			Boolean.class.equals(type) ||
			Character.class.equals(type)) {
			Assert.assertEquals(expected, found);
			return;
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			Assert.assertEquals(((CharSequence)expected).toString(), ((CharSequence)found).toString());
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
		if (expected.getClass().isPrimitive()) return;
		if (expected.getClass().equals(String.class)) return;
		for (Field f : ClassUtil.getAllFields(expected.getClass())) {
			if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
			if ((f.getModifiers() & Modifier.FINAL) != 0) continue;
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
	
	protected void checkArray(Object expected, Object found) throws Exception {
		Assert.assertEquals(Array.getLength(expected), Array.getLength(found));
		int l = Array.getLength(expected);
		for (int i = 0; i < l; ++i)
			checkValue(Array.get(expected, i), Array.get(found, i));
	}
	
	protected void print(IO.Readable.Seekable io, Object o) throws Exception {
		String content = IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_8);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		System.out.println("Serialization result for " + o.getClass().getName() + "\r\n" + content);
	}
	
}

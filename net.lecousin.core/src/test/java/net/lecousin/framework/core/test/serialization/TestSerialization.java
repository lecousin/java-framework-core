package net.lecousin.framework.core.test.serialization;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
	
	@Test
	public void testBooleans() throws Exception {
		TestBooleans b = new TestBooleans();
		b.attr1 = true;
		b.attr2 = false;
		b.attr3 = Boolean.TRUE;
		b.attr4 = Boolean.FALSE;
		MemoryIO io = serialize(b);
		print(io, b);
		TestBooleans b2 = deserialize(io, TestBooleans.class);
		check(b, b2);
	}
	
	protected MemoryIO serialize(Object o) throws Exception {
		MemoryIO io = new MemoryIO(1024, "test");
		Serializer s = createSerializer();
		ISynchronizationPoint<Exception> res = s.serialize(o, io, new ArrayList<>(0));
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
			Assert.assertEquals(f.get(expected), f.get(found));
		}
	}
	
	protected void print(MemoryIO io, Object o) throws Exception {
		String content = IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_8);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		System.out.println("Serialization result for " + o.getClass().getName() + "\r\n" + content);
	}
	
}

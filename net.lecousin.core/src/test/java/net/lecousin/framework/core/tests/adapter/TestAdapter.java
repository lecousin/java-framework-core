package net.lecousin.framework.core.tests.adapter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.lecousin.framework.adapter.Adapter;
import net.lecousin.framework.adapter.AdapterException;
import net.lecousin.framework.adapter.AdapterRegistry;
import net.lecousin.framework.adapter.FileInfoToFile;
import net.lecousin.framework.adapter.FileToIO;
import net.lecousin.framework.adapter.LinkedAdapter;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.util.FileInfo;

public class TestAdapter extends LCCoreAbstractTest {

	@BeforeClass
	public static void register() {
		AdapterRegistry.get().addPlugin(new Adapter1());
		AdapterRegistry.get().addPlugin(new Adapter2_1_Error());
		AdapterRegistry.get().addPlugin(new Adapter2_2_Error());
		AdapterRegistry.get().addPlugin(new Adapter2_1());
		AdapterRegistry.get().addPlugin(new Adapter2_2());
		AdapterRegistry.get().addPlugin(new Adapter2_3());
		AdapterRegistry.get().addPlugin(new Adapter3());
		AdapterRegistry.get().addPlugin(new Adapter4bis());
		AdapterRegistry.get().addPlugin(new Adapter4ter());
		AdapterRegistry.get().addPlugin(new Source1ToSource2());
		AdapterRegistry.get().addPlugin(new Source1ToSource3());
		AdapterRegistry.get().addPlugin(new Target2ToTarget4());
		AdapterRegistry.get().addPlugin(new Target3ToTarget4());
	}
	
	public static class Source1 {
		public int value = 0;
	}
	public static class Target1 {
		public int value = 0;
	}
	
	public static class Adapter1 implements Adapter<Source1, Target1> {
		@Override
		public Class<Source1> getInputType() { return Source1.class; }
		@Override
		public Class<Target1> getOutputType() { return Target1.class; }
		@Override
		public boolean canAdapt(Source1 input) { return true; }
		@Override
		public Target1 adapt(Source1 input) {
			Target1 t = new Target1();
			t.value = input.value;
			return t;
		}
	}
	
	@Test
	public void testDirectAdapter() throws Exception {
		Source1 src = new Source1();
		src.value = 51;
		Assert.assertTrue(AdapterRegistry.get().canAdapt(src, Target1.class));
		Assert.assertFalse(AdapterRegistry.get().canAdapt(src, Target4ter.class));
		Target1 t = AdapterRegistry.get().adapt(src, Target1.class);
		Assert.assertEquals(t.value, src.value);
	}
	
	public static class Source2 {
		public int value = 0;
	}
	public static class Intermediate2_1 {
		public int value = 0;
	}
	public static class Intermediate2_2 {
		public int value = 0;
	}
	public static class Target2 {
		public int value = 0;
	}
	
	public static class Adapter2_1 implements Adapter<Source2, Intermediate2_1> {
		@Override
		public Class<Source2> getInputType() { return Source2.class; }
		@Override
		public Class<Intermediate2_1> getOutputType() { return Intermediate2_1.class; }
		@Override
		public boolean canAdapt(Source2 input) { return true; }
		@Override
		public Intermediate2_1 adapt(Source2 input) {
			Intermediate2_1 t = new Intermediate2_1();
			t.value = input.value;
			return t;
		}
	}
	public static class Adapter2_2 implements Adapter<Intermediate2_1, Intermediate2_2> {
		@Override
		public Class<Intermediate2_1> getInputType() { return Intermediate2_1.class; }
		@Override
		public Class<Intermediate2_2> getOutputType() { return Intermediate2_2.class; }
		@Override
		public boolean canAdapt(Intermediate2_1 input) { return true; }
		@Override
		public Intermediate2_2 adapt(Intermediate2_1 input) {
			Intermediate2_2 t = new Intermediate2_2();
			t.value = input.value;
			return t;
		}
	}
	public static class Adapter2_3 implements Adapter<Intermediate2_2, Target2> {
		@Override
		public Class<Intermediate2_2> getInputType() { return Intermediate2_2.class; }
		@Override
		public Class<Target2> getOutputType() { return Target2.class; }
		@Override
		public boolean canAdapt(Intermediate2_2 input) { return true; }
		@Override
		public Target2 adapt(Intermediate2_2 input) {
			Target2 t = new Target2();
			t.value = input.value;
			return t;
		}
	}
	public static class Adapter2_1_Error implements Adapter<Source2, Intermediate2_1> {
		@Override
		public Class<Source2> getInputType() { return Source2.class; }
		@Override
		public Class<Intermediate2_1> getOutputType() { return Intermediate2_1.class; }
		@Override
		public boolean canAdapt(Source2 input) { return true; }
		@Override
		public Intermediate2_1 adapt(Source2 input) throws AdapterException {
			throw new AdapterException("Do not use this adapter", new Exception());
		}
	}
	public static class Adapter2_2_Error implements Adapter<Intermediate2_1, Intermediate2_2> {
		@Override
		public Class<Intermediate2_1> getInputType() { return Intermediate2_1.class; }
		@Override
		public Class<Intermediate2_2> getOutputType() { return Intermediate2_2.class; }
		@Override
		public boolean canAdapt(Intermediate2_1 input) { return true; }
		@Override
		public Intermediate2_2 adapt(Intermediate2_1 input) throws AdapterException {
			throw new AdapterException("Do not use this adapter");
		}
	}
	
	@Test
	public void testIntermediates() throws Exception {
		Source2 src = new Source2();
		src.value = 51;
		Target2 t = AdapterRegistry.get().adapt(src, Target2.class);
		Assert.assertEquals(t.value, src.value);
	}
	
	@Test
	public void testFileToIO() throws Exception {
		File f = File.createTempFile("test", "filetoio");
		f.deleteOnExit();
		IO.Writable out = AdapterRegistry.get().adapt(f, IO.Writable.class);
		out.writeSync(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
		out.close();
		IO.Readable in = AdapterRegistry.get().adapt(f, IO.Readable.class);
		byte[] buf = new byte[5];
		Assert.assertEquals(3, in.readFullySync(ByteBuffer.wrap(buf)));
		Assert.assertEquals(1, buf[0]);
		Assert.assertEquals(2, buf[1]);
		Assert.assertEquals(3, buf[2]);
		in.close();
	}
	
	@Test
	public void basicTests() throws Exception {
		Assert.assertNull(AdapterRegistry.get().adapt(new Object(), TestAdapter.class));
		FileInfo fi = new FileInfo();
		Assert.assertNull(AdapterRegistry.get().findAdapter(fi, FileInfo.class, File.class));
		Assert.assertNull(AdapterRegistry.get().findAdapter(fi, FileInfo.class, Path.class));
		@SuppressWarnings("rawtypes")
		LinkedList<Adapter> list = new LinkedList<>();
		list.add(new FileInfoToFile());
		list.add(new FileToIO.Readable());
		LinkedAdapter la = new LinkedAdapter(list);
		Assert.assertEquals(FileInfo.class, la.getInputType());
		Assert.assertEquals(IO.Readable.Seekable.class, la.getOutputType());
		la.canAdapt(fi);
		la.adapt(fi);
	}
	
	public static class Source3 {
		public int value = 0;
	}
	public static class Target3 {
		public int value = 0;
	}
	public static class Target4 {
		public int value = 0;
	}
	public static class Target4bis extends Target4 {
	}
	public static class Target4ter extends Target4bis {
	}
	public static class Adapter3 implements Adapter<Source3, Target3> {
		@Override
		public Class<Source3> getInputType() { return Source3.class; }
		@Override
		public Class<Target3> getOutputType() { return Target3.class; }
		@Override
		public boolean canAdapt(Source3 input) {
			return input.value > 100;
		}
		@Override
		public Target3 adapt(Source3 input) {
			Target3 t = new Target3();
			t.value = input.value - 100;
			return t;
		}
	}
	public static class Adapter4bis implements Adapter<Source3, Target4bis> {
		@Override
		public Class<Source3> getInputType() { return Source3.class; }
		@Override
		public Class<Target4bis> getOutputType() { return Target4bis.class; }
		@Override
		public boolean canAdapt(Source3 input) {
			return input.value > 100;
		}
		@Override
		public Target4bis adapt(Source3 input) {
			Target4bis t = new Target4bis();
			t.value = input.value - 10;
			return t;
		}
	}
	public static class Adapter4ter implements Adapter<Source3, Target4ter> {
		@Override
		public Class<Source3> getInputType() { return Source3.class; }
		@Override
		public Class<Target4ter> getOutputType() { return Target4ter.class; }
		@Override
		public boolean canAdapt(Source3 input) {
			return input.value > 100;
		}
		@Override
		public Target4ter adapt(Source3 input) {
			Target4ter t = new Target4ter();
			t.value = input.value - 1;
			return t;
		}
	}
	
	@Test
	public void test3() throws Exception {
		Source3 src = new Source3();
		src.value = 1;
		Assert.assertNull(AdapterRegistry.get().adapt(src, Target3.class));
		src.value = 111;
		Assert.assertEquals(11, AdapterRegistry.get().adapt(src, Target3.class).value);

		src.value = 111;
		Assert.assertEquals(110, AdapterRegistry.get().adapt(src, Target4.class).value);
	}
	
	public static class Source1ToSource2 implements Adapter<Source1, Source2> {
		@Override
		public Class<Source1> getInputType() { return Source1.class; }
		@Override
		public Class<Source2> getOutputType() { return Source2.class; }
		@Override
		public boolean canAdapt(Source1 input) {
			return input.value > 0;
		}
		@Override
		public Source2 adapt(Source1 input) {
			Source2 t = new Source2();
			t.value = input.value;
			return t;
		}
	}

	public static class Source1ToSource3 implements Adapter<Source1, Source3> {
		@Override
		public Class<Source1> getInputType() { return Source1.class; }
		@Override
		public Class<Source3> getOutputType() { return Source3.class; }
		@Override
		public boolean canAdapt(Source1 input) {
			return input.value > 0;
		}
		@Override
		public Source3 adapt(Source1 input) {
			Source3 t = new Source3();
			t.value = input.value;
			return t;
		}
	}

	public static class Target2ToTarget4 implements Adapter<Target2, Target4> {
		@Override
		public Class<Target2> getInputType() { return Target2.class; }
		@Override
		public Class<Target4> getOutputType() { return Target4.class; }
		@Override
		public boolean canAdapt(Target2 input) {
			return input.value > 0;
		}
		@Override
		public Target4 adapt(Target2 input) {
			Target4 t = new Target4();
			t.value = input.value;
			return t;
		}
	}

	public static class Target3ToTarget4 implements Adapter<Target3, Target4> {
		@Override
		public Class<Target3> getInputType() { return Target3.class; }
		@Override
		public Class<Target4> getOutputType() { return Target4.class; }
		@Override
		public boolean canAdapt(Target3 input) {
			return input.value > 0;
		}
		@Override
		public Target4 adapt(Target3 input) {
			Target4 t = new Target4();
			t.value = input.value;
			return t;
		}
	}

	@Test
	public void testDifferentIntermediates() throws Exception {
		Source1 src = new Source1();
		src.value = 10;
		AdapterRegistry.get().adapt(src, Target4.class);
		src.value = 110;
		AdapterRegistry.get().adapt(src, Target4.class);
	}

}

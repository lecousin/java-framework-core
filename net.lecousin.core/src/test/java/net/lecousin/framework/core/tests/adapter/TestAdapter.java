package net.lecousin.framework.core.tests.adapter;

import net.lecousin.framework.adapter.Adapter;
import net.lecousin.framework.adapter.AdapterRegistry;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestAdapter extends LCCoreAbstractTest {

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
	
	@Test(timeout=120000)
	public void testDirectAdapter() throws Exception {
		AdapterRegistry.get().addPlugin(new Adapter1());
		Source1 src = new Source1();
		src.value = 51;
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
	
	@Test(timeout=120000)
	public void testIntermediates() throws Exception {
		AdapterRegistry.get().addPlugin(new Adapter2_1());
		AdapterRegistry.get().addPlugin(new Adapter2_2());
		AdapterRegistry.get().addPlugin(new Adapter2_3());
		Source2 src = new Source2();
		src.value = 51;
		Target2 t = AdapterRegistry.get().adapt(src, Target2.class);
		Assert.assertEquals(t.value, src.value);
	}
	
	
}

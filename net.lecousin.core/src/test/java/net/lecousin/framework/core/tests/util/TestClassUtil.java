package net.lecousin.framework.core.tests.util;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.properties.Property;
import net.lecousin.framework.util.ClassUtil;

public class TestClassUtil extends LCCoreAbstractTest {

	public static class Class1 {
		public int myInteger;
	}
	
	public static class Class2 extends Class1 {
		public boolean myBoolean;
		public boolean ok;
		
		public void setMyBoolean(boolean value, String something) {
			myBoolean = value && something.isEmpty();
		}
		
		@Property(name="good", value="yes")
		public void setMyBoolean(boolean value) {
			myBoolean = value;
		}

		protected void setMyBoolean2(boolean value) {
			myBoolean = value;
		}

		public static void setMyBoolean3(@SuppressWarnings("unused") boolean value) {
		}
		
		@Property(name="good", value="yes")
		public boolean getMyBoolean() {
			return myBoolean;
		}

		protected boolean getMyBoolean2(@SuppressWarnings("unused") int notused) {
			return false;
		}
		protected boolean isMyBoolean2(@SuppressWarnings("unused") int notused) {
			return false;
		}
		
		protected boolean getMyBoolean2() {
			return false;
		}
		
		public static boolean getMyBoolean3() {
			return true;
		}
		
		@Property(name="good", value="yes")
		public boolean isOk() {
			return ok;
		}
	}
	
	public static class Class4 {
		boolean bool;
		Boolean bool2;
		boolean bool3;
		Boolean bool4;
		int i;
		public boolean getBool(@SuppressWarnings("unused") int toto) { return true; }
		public boolean isBool(@SuppressWarnings("unused") int toto) { return true; }
		public static boolean isBool2() { return true; }
		public int isBool3() { return 0; }
		public Boolean isBool4() { return bool4; }
	}
	
	@Test(timeout=30000)
	public void testGetAllFieldsInheritedFirst() {
		//ArrayList<Field> fields =
		ClassUtil.getAllFieldsInheritedFirst(Class2.class);
		//Assert.assertEquals(3, fields.size());
		//Assert.assertEquals("myInteger", fields.get(0).getName());
	}
	
	@Test(timeout=30000)
	public void testGetAllMethodsInheritedFirst() {
		ClassUtil.getAllMethodsInheritedFirst(Class2.class);
	}
	
	@Test(timeout=30000)
	public void testGettersAndSetters() {
		Assert.assertNotNull(ClassUtil.getGetter(Class2.class, "myBoolean").getAnnotation(Property.class));
		Assert.assertNull(ClassUtil.getGetter(Class2.class, "myBoolean2"));
		Assert.assertNull(ClassUtil.getGetter(Class2.class, "myBoolean3"));
		Assert.assertNotNull(ClassUtil.getGetter(Class2.class, "ok").getAnnotation(Property.class));
		Assert.assertNotNull(ClassUtil.getSetter(Class2.class, "myBoolean").getAnnotation(Property.class));
		Assert.assertNull(ClassUtil.getSetter(Class2.class, "myBoolean2"));
		Assert.assertNull(ClassUtil.getSetter(Class2.class, "myBoolean3"));
		
		Assert.assertNull(ClassUtil.getGetter(Class4.class, "bool"));
		Assert.assertNull(ClassUtil.getGetter(Class4.class, "bool2"));
		Assert.assertNull(ClassUtil.getGetter(Class4.class, "bool3"));
		Assert.assertNotNull(ClassUtil.getGetter(Class4.class, "bool4"));
		Assert.assertNull(ClassUtil.getGetter(Class4.class, "i"));
	}
	
	@SuppressWarnings("unused")
	public static class Class3 {
		public int toto(String s, int i) { return 1; }
		public int toto(String s, long l) { return 2; }
		public int toto(int i, String s) { return 3; }
	}
	
	@Test(timeout=30000)
	public void testGetMethod() throws Exception {
		Object[] toto1 = new Object[] { "test", Integer.valueOf(51) };
		Object[] toto2 = new Object[] { "test", Long.valueOf(51) };
		Object[] toto3 = new Object[] { Integer.valueOf(1), "test" };
		Class3 o = new Class3();
		Assert.assertEquals(Integer.valueOf(1), ClassUtil.getMethodFor(Class3.class, "toto", toto1).invoke(o, toto1));
		Assert.assertEquals(Integer.valueOf(2), ClassUtil.getMethodFor(Class3.class, "toto", toto2).invoke(o, toto2));
		Assert.assertEquals(Integer.valueOf(3), ClassUtil.getMethodFor(Class3.class, "toto", toto3).invoke(o, toto3));
		
		Assert.assertNotNull(ClassUtil.getMethod(Class3.class, "toto"));
		Assert.assertNull(ClassUtil.getMethod(Class3.class, "titi"));
		Assert.assertNotNull(ClassUtil.getMethod(Class3.class, "toto", 2));
		Assert.assertNull(ClassUtil.getMethod(Class3.class, "toto", 3));
	}
	
	public static class Root {
		public Element e1 = new Element();
		public Element e2 = new Element();
	}
	
	public static class Element {
		public Leaf sub1 = new Leaf();
		public Leaf sub2 = new Leaf();
	}
	
	public static class Leaf {
		public int i = 1;
	}
	
	@Test(timeout=30000)
	public void testFieldPath() throws Exception {
		Root root = new Root();
		Assert.assertEquals(1, root.e1.sub1.i);
		ClassUtil.setFieldFromPath(root, "e1.sub1.i", Integer.valueOf(51));
		Assert.assertEquals(51, root.e1.sub1.i);
		root.e2.sub2.i = 11;
		Assert.assertEquals(Integer.valueOf(11), ClassUtil.getFieldFromPath(root, "e2.sub2.i"));
	}
	
	@Test(timeout=30000)
	public void testGetMethods() {
		Assert.assertEquals(1, ClassUtil.getMethods(Class2.class, "setMyBoolean", 1).size());
	}

	@Test(timeout=30000)
	public void test() throws ClassNotFoundException {
		Assert.assertEquals("", ClassUtil.getPackageName(getClass().getClassLoader().loadClass("NoPackageClass")));
	}

}

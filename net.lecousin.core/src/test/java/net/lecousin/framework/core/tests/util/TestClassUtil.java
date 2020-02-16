package net.lecousin.framework.core.tests.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.properties.Property;
import net.lecousin.framework.util.ClassUtil;

import org.junit.Assert;
import org.junit.Test;

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
	
	public static class ClassEmpty {}
	
	@Test
	public void testGetAllFieldsInheritedFirst() {
		ArrayList<Field> fields = ClassUtil.getAllFieldsInheritedFirst(Class2.class);
		boolean myInteger = false;
		boolean myBoolean = false;
		boolean ok = false;
		for (Field f : fields) {
			if (f.getName().equals("myInteger")) {
				Assert.assertFalse(myInteger);
				myInteger = true;
			} else if (f.getName().equals("myBoolean")) {
				Assert.assertTrue(myInteger);
				Assert.assertFalse(myBoolean);
				myBoolean = true;
			} else if (f.getName().equals("ok")) {
				Assert.assertTrue(myInteger);
				Assert.assertFalse(ok);
				ok = true;
			}
		}
		Assert.assertTrue(myInteger);
		Assert.assertTrue(myBoolean);
		Assert.assertTrue(ok);
		
		fields = ClassUtil.getAllFieldsInheritedFirst(ClassEmpty.class);
	}
	
	@Test
	public void testGetAllMethodsInheritedFirst() {
		ClassUtil.getAllMethodsInheritedFirst(Class2.class);
	}
	
	@Test
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
		
		public int tutu(boolean b) { return b ? 51 : -81; }
		public int tutu(byte b) { return b + 65; }
		public int tutu(int i) { return i / 2; }
		public int tutu(short s) { return s * 2; }
		public int tutu(long l) { return (int)(l % 10); }
		public int tutu(float f) { return (int)(f * 4); }
		public int tutu(double d) { return (int)(d * 6.5d); }
	}
	
	@Test
	public void testGetMethod() throws Exception {
		Object[] toto1 = new Object[] { "test", Integer.valueOf(51) };
		Object[] toto2 = new Object[] { "test", Long.valueOf(51) };
		Object[] toto3 = new Object[] { Integer.valueOf(1), "test" };
		Class3 o = new Class3();
		Assert.assertEquals(Integer.valueOf(1), ClassUtil.getMethodFor(Class3.class, "toto", toto1).invoke(o, toto1));
		Assert.assertEquals(Integer.valueOf(2), ClassUtil.getMethodFor(Class3.class, "toto", toto2).invoke(o, toto2));
		Assert.assertEquals(Integer.valueOf(3), ClassUtil.getMethodFor(Class3.class, "toto", toto3).invoke(o, toto3));
		Assert.assertNull(ClassUtil.getMethodFor(Class3.class, "toto", new Object[0]));
		Assert.assertNull(ClassUtil.getMethodFor(Class3.class, "titi", toto1));
		
		Object[] tutu1 = new Object[] { Boolean.TRUE };
		Assert.assertEquals(Integer.valueOf(51), ClassUtil.getMethodFor(Class3.class, "tutu", tutu1).invoke(o, tutu1));
		Object[] tutu2 = new Object[] { Byte.valueOf((byte)15) };
		Assert.assertEquals(Integer.valueOf(80), ClassUtil.getMethodFor(Class3.class, "tutu", tutu2).invoke(o, tutu2));
		Object[] tutu3 = new Object[] { Integer.valueOf(64) };
		Assert.assertEquals(Integer.valueOf(32), ClassUtil.getMethodFor(Class3.class, "tutu", tutu3).invoke(o, tutu3));
		Object[] tutu4 = new Object[] { Short.valueOf((short)147) };
		Assert.assertEquals(Integer.valueOf(294), ClassUtil.getMethodFor(Class3.class, "tutu", tutu4).invoke(o, tutu4));
		Object[] tutu5 = new Object[] { Long.valueOf(18953) };
		Assert.assertEquals(Integer.valueOf(3), ClassUtil.getMethodFor(Class3.class, "tutu", tutu5).invoke(o, tutu5));
		Object[] tutu6 = new Object[] { Float.valueOf(45.256f) };
		Assert.assertEquals(Integer.valueOf(181), ClassUtil.getMethodFor(Class3.class, "tutu", tutu6).invoke(o, tutu6));
		Object[] tutu7 = new Object[] { Double.valueOf(187.2314d) };
		Assert.assertEquals(Integer.valueOf(1217), ClassUtil.getMethodFor(Class3.class, "tutu", tutu7).invoke(o, tutu7));
		
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
	
	@Test
	public void testFieldPath() throws Exception {
		Root root = new Root();
		Assert.assertEquals(1, root.e1.sub1.i);
		ClassUtil.setFieldFromPath(root, "e1.sub1.i", Integer.valueOf(51));
		Assert.assertEquals(51, root.e1.sub1.i);
		root.e2.sub2.i = 11;
		Assert.assertEquals(Integer.valueOf(11), ClassUtil.getFieldFromPath(root, "e2.sub2.i"));

		try {
			ClassUtil.getFieldFromPath(root, "e2.sub3.i");
			throw new AssertionError("exception expected");
		} catch (NoSuchFieldException e) {
			// ok
		}
		
		root.e2.sub2 = null;
		try {
			ClassUtil.getFieldFromPath(root, "e2.sub2.i");
			throw new AssertionError("exception expected");
		} catch (NoSuchFieldException e) {
			// ok
		}
	}

	public static class Root2 {
		public Element2 _e1 = new Element2();
		public Element2 _e2 = new Element2();

		public Element2 getE1() {
			return _e1;
		}
		public void setE1(Element2 e1) {
			this._e1 = e1;
		}
		public Element2 getE2() {
			return _e2;
		}
		public void setE2(Element2 e2) {
			this._e2 = e2;
		}
		
	}
	
	public static class Element2 {
		public Leaf2 _sub1 = new Leaf2();
		public Leaf2 _sub2 = new Leaf2();
		
		public Leaf2 getSub1() {
			return _sub1;
		}
		public void setSub1(Leaf2 sub1) {
			this._sub1 = sub1;
		}
		public Leaf2 getSub2() {
			return _sub2;
		}
		public void setSub2(Leaf2 sub2) {
			this._sub2 = sub2;
		}

	}
	
	public static class Leaf2 {
		public int _i = 1;

		public int getI() {
			return _i;
		}

		public void setI(int i) {
			this._i = i;
		}
		
	}
	
	@Test
	public void testFieldPath2() throws Exception {
		Root2 root = new Root2();
		Assert.assertEquals(1, root._e1._sub1._i);
		ClassUtil.setFieldFromPath(root, "e1.sub1.i", Integer.valueOf(51));
		Assert.assertEquals(51, root._e1._sub1._i);
		root._e2._sub2._i = 11;
		Assert.assertEquals(Integer.valueOf(11), ClassUtil.getFieldFromPath(root, "e2.sub2.i"));

		root._e2._sub2 = null;
		try {
			ClassUtil.getFieldFromPath(root, "e2.sub2.i");
			throw new AssertionError("exception expected");
		} catch (NoSuchFieldException e) {
			// ok
		}
	}
	
	@Test
	public void testGetMethods() {
		Assert.assertEquals(1, ClassUtil.getMethods(Class2.class, "setMyBoolean", 1).size());
	}

	@Test
	public void test() throws ClassNotFoundException {
		Assert.assertEquals("", ClassUtil.getPackageName(getClass().getClassLoader().loadClass("NoPackageClass")));
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
	public @interface Annot1 {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
	public @interface Annot2 {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
	public @interface Annot3 {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
	public @interface Annot4 {}
	
	@Annot1
	public interface AnnotInterface1 {
		
	}
	
	@Annot2
	public class AnnotClass1 {
		
	}
	
	@Annot3
	public class AnnotClass2 implements AnnotInterface1 {
		
	}
	
	@Annot4
	public class AnnotClass3 extends AnnotClass1 implements AnnotInterface1 {
		
	}
	
	@Test
	public void testAnnotations() {
		List<Annotation> list = ClassUtil.getAllAnnotations(Class1.class);
		Assert.assertEquals(0, list.size());
		
		list = ClassUtil.getAllAnnotations(AnnotInterface1.class);
		Assert.assertEquals(1, list.size());
		
		list = ClassUtil.getAllAnnotations(AnnotClass1.class);
		Assert.assertEquals(1, list.size());
		
		list = ClassUtil.getAllAnnotations(AnnotClass2.class);
		Assert.assertEquals(2, list.size());
		
		list = ClassUtil.getAllAnnotations(AnnotClass3.class);
		Assert.assertEquals(3, list.size());
	}

}

package net.lecousin.framework.core.tests.util;

import java.lang.reflect.Field;
import java.util.ArrayList;

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

		public static void setMyBoolean3(boolean value) {
		}
		
		@Property(name="good", value="yes")
		public boolean getMyBoolean() {
			return myBoolean;
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
	
	@Test(timeout=30000)
	public void testGetAllFieldsInheritedFirst() {
		ArrayList<Field> fields = ClassUtil.getAllFieldsInheritedFirst(Class2.class);
		Assert.assertEquals(3, fields.size());
		Assert.assertEquals("myInteger", fields.get(0).getName());
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
	}
	
}

package net.lecousin.framework.core.tests.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import net.lecousin.framework.collections.ArrayIterator;
import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestArrayUtil extends LCCoreAbstractTest {

	@Test
	public void testContains() {
		Assert.assertTrue(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)0));
		Assert.assertTrue(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)80));
		Assert.assertTrue(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)-80));
		Assert.assertTrue(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)10));
		Assert.assertFalse(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)11));
		Assert.assertFalse(ArrayUtil.contains(new byte[] { 10,  8, 99, 0, -6, 80, -80 }, (byte)1));
		Assert.assertFalse(ArrayUtil.contains((byte[])null, (byte)1));
		
		Assert.assertTrue(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)0));
		Assert.assertTrue(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)80));
		Assert.assertTrue(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)-80));
		Assert.assertTrue(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)10));
		Assert.assertFalse(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)11));
		Assert.assertFalse(ArrayUtil.contains(new short[] { 10,  8, 99, 0, -6, 80, -80 }, (short)1));
		Assert.assertFalse(ArrayUtil.contains((short[])null, (short)1));
		
		Assert.assertTrue(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, 0));
		Assert.assertTrue(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, 80));
		Assert.assertTrue(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, -80));
		Assert.assertTrue(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, 10));
		Assert.assertFalse(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, 11));
		Assert.assertFalse(ArrayUtil.contains(new int[] { 10,  8, 99, 0, -6, 80, -80 }, 1));
		Assert.assertFalse(ArrayUtil.contains((int[])null, 1));
		
		Assert.assertTrue(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, 0));
		Assert.assertTrue(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, 80));
		Assert.assertTrue(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, -80));
		Assert.assertTrue(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, 10));
		Assert.assertFalse(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, 11));
		Assert.assertFalse(ArrayUtil.contains(new long[] { 10,  8, 99, 0, -6, 80, -80 }, 1));
		Assert.assertFalse(ArrayUtil.contains((long[])null, 1));
		
		Assert.assertTrue(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, (char)0));
		Assert.assertTrue(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, (char)80));
		Assert.assertTrue(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, (char)-80));
		Assert.assertTrue(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, 'a'));
		Assert.assertFalse(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, 'b'));
		Assert.assertFalse(ArrayUtil.contains(new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }, (char)1));
		Assert.assertFalse(ArrayUtil.contains((char[])null, (char)1));
		
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		Object[] a = new Object[] { o1, o2, o3 };
		Assert.assertTrue(ArrayUtil.contains(a, o1));
		Assert.assertTrue(ArrayUtil.contains(a, o2));
		Assert.assertTrue(ArrayUtil.contains(a, o3));
		Assert.assertFalse(ArrayUtil.contains(a, o4));
		Assert.assertFalse(ArrayUtil.contains((Object[])null, o1));
	}
	
	@Test
	public void testIndexOf() {
		Assert.assertEquals(3, ArrayUtil.indexOf((byte)0, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(5, ArrayUtil.indexOf((byte)80, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(6, ArrayUtil.indexOf((byte)-80, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(0, ArrayUtil.indexOf((byte)10, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf((byte)11, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf((byte)1, new byte[] { 10,  8, 99, 0, -6, 80, -80 }));

		Assert.assertEquals(3, ArrayUtil.indexOf((short)0, new short[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(5, ArrayUtil.indexOf((short)80, new short[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(6, ArrayUtil.indexOf((short)-80, new short[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(0, ArrayUtil.indexOf((short)10, new short[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf((short)11, new short[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf((short)1, new short[] { 10,  8, 99, 0, -6, 80, -80 }));

		Assert.assertEquals(3, ArrayUtil.indexOf(0, new int[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(5, ArrayUtil.indexOf(80, new int[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(6, ArrayUtil.indexOf(-80, new int[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(0, ArrayUtil.indexOf(10, new int[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf(11, new int[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf(1, new int[] { 10,  8, 99, 0, -6, 80, -80 }));

		Assert.assertEquals(3, ArrayUtil.indexOf(0, new long[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(5, ArrayUtil.indexOf(80, new long[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(6, ArrayUtil.indexOf(-80, new long[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(0, ArrayUtil.indexOf(10, new long[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf(11, new long[] { 10,  8, 99, 0, -6, 80, -80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf(1, new long[] { 10,  8, 99, 0, -6, 80, -80 }));

		Assert.assertEquals(3, ArrayUtil.indexOf((char)0, new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));
		Assert.assertEquals(5, ArrayUtil.indexOf((char)80, new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));
		Assert.assertEquals(6, ArrayUtil.indexOf((char)-80, new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));
		Assert.assertEquals(0, ArrayUtil.indexOf('a', new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf('b', new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));
		Assert.assertEquals(-1, ArrayUtil.indexOf((char)1, new char[] { 'a',  '1', '\t', 0, (char)-6, 80, (char)-80 }));

		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		Object[] a = new Object[] { o1, o2, o3 };
		Assert.assertEquals(0, ArrayUtil.indexOf(o1, a));
		Assert.assertEquals(1, ArrayUtil.indexOf(o2, a));
		Assert.assertEquals(2, ArrayUtil.indexOf(o3, a));
		Assert.assertEquals(-1, ArrayUtil.indexOf(o4, a));
		Assert.assertEquals(0, ArrayUtil.indexOfInstance(o1, a));
		Assert.assertEquals(1, ArrayUtil.indexOfInstance(o2, a));
		Assert.assertEquals(2, ArrayUtil.indexOfInstance(o3, a));
		Assert.assertEquals(-1, ArrayUtil.indexOfInstance(o4, a));
	}
	
	@Test
	public void testEquals() {
		Assert.assertTrue(ArrayUtil.equals(new byte[] { 1, 10, 22 }, new byte[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new byte[] { 2, 10, 22 }, new byte[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new byte[] { 1, 10, 22 }, new byte[] { 1, 10, 21 }));
		Assert.assertTrue(ArrayUtil.equals(new byte[] { 2, 10, 22 }, 1, new byte[] { 1, 10, 22 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals(new byte[] { 2, 10, 22 }, 1, new byte[] { 10, 22, 33 }, 0, 2));
		Assert.assertFalse(ArrayUtil.equals(new byte[] { 2, 10, 22 }, 0, new byte[] { 1, 10, 22 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals((byte[])null, (byte[])null));
		Assert.assertFalse(ArrayUtil.equals(new byte[] {}, (byte[])null));
		Assert.assertFalse(ArrayUtil.equals((byte[])null, new byte[] {}));
		
		Assert.assertTrue(ArrayUtil.equals(new short[] { 1, 10, 22 }, new short[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new short[] { 2, 10, 22 }, new short[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new short[] { 1, 10, 22 }, new short[] { 1, 10, 21 }));
		Assert.assertTrue(ArrayUtil.equals(new short[] { 2, 10, 22 }, 1, new short[] { 1, 10, 22 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals(new short[] { 2, 10, 22 }, 1, new short[] { 10, 22, 33 }, 0, 2));
		Assert.assertFalse(ArrayUtil.equals(new short[] { 2, 10, 22 }, 0, new short[] { 1, 10, 22 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals((short[])null, (short[])null));
		Assert.assertFalse(ArrayUtil.equals(new short[] {}, (short[])null));
		Assert.assertFalse(ArrayUtil.equals((short[])null, new short[] {}));
		
		Assert.assertTrue(ArrayUtil.equals(new int[] { 1, 10, 22 }, new int[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new int[] { 2, 10, 22 }, new int[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new int[] { 1, 10, 22 }, new int[] { 1, 10, 21 }));
		Assert.assertTrue(ArrayUtil.equals(new int[] { 2, 10, 22 }, 1, new int[] { 1, 10, 22 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals(new int[] { 2, 10, 22 }, 1, new int[] { 10, 22, 33 }, 0, 2));
		Assert.assertFalse(ArrayUtil.equals(new int[] { 2, 10, 22 }, 0, new int[] { 1, 10, 22 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals((int[])null, (int[])null));
		Assert.assertFalse(ArrayUtil.equals(new int[] {}, (int[])null));
		Assert.assertFalse(ArrayUtil.equals((int[])null, new int[] {}));
		
		Assert.assertTrue(ArrayUtil.equals(new long[] { 1, 10, 22 }, new long[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new long[] { 2, 10, 22 }, new long[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new long[] { 1, 10, 22 }, new long[] { 1, 10, 21 }));
		Assert.assertTrue(ArrayUtil.equals(new long[] { 2, 10, 22 }, 1, new long[] { 1, 10, 22 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals(new long[] { 2, 10, 22 }, 1, new long[] { 10, 22, 33 }, 0, 2));
		Assert.assertFalse(ArrayUtil.equals(new long[] { 2, 10, 22 }, 0, new long[] { 1, 10, 22 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals((long[])null, (long[])null));
		Assert.assertFalse(ArrayUtil.equals(new long[] {}, (long[])null));
		Assert.assertFalse(ArrayUtil.equals((long[])null, new long[] {}));
		
		Assert.assertTrue(ArrayUtil.equals(new char[] { 1, 10, 22 }, new char[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new char[] { 2, 10, 22 }, new char[] { 1, 10, 22 }));
		Assert.assertFalse(ArrayUtil.equals(new char[] { 1, 10, 22 }, new char[] { 1, 10, 21 }));
		Assert.assertTrue(ArrayUtil.equals(new char[] { 2, 10, 22 }, 1, new char[] { 1, 10, 22 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals(new char[] { 2, 10, 22 }, 1, new char[] { 10, 22, 33 }, 0, 2));
		Assert.assertFalse(ArrayUtil.equals(new char[] { 2, 10, 22 }, 0, new char[] { 1, 10, 22 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals((char[])null, (char[])null));
		Assert.assertFalse(ArrayUtil.equals(new char[] {}, (char[])null));
		Assert.assertFalse(ArrayUtil.equals((char[])null, new char[] {}));
		
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		Assert.assertTrue(ArrayUtil.equals(new Object[] { o1, o2, o3 }, new Object[] { o1, o2, o3 }));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o1, o2, o3 }, new Object[] { o1, o2, o3, o4 }));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o1, o2, o3, o4 }, new Object[] { o1, o2, o3 }));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o4, o2, o3 }, new Object[] { o1, o2, o3 }));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o1, o2, o3 }, new Object[] { o1, o2, o4 }));
		Assert.assertTrue(ArrayUtil.equals(new Object[] { o1, o2, o3 }, 1, new Object[] { o1, o2, o3 }, 1, 2));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o1, o2, o3 }, 0, new Object[] { o1, o2, o3 }, 1, 2));
		Assert.assertFalse(ArrayUtil.equals(new Object[] { o1, o2, o3 }, 1, new Object[] { o1, o2, o3 }, 0, 2));
		Assert.assertTrue(ArrayUtil.equals(new Object[] { o1, o2, o3 }, 1, new Object[] { o4, o2, o3 }, 1, 2));
		Assert.assertTrue(ArrayUtil.equals((Object[])null, (Object[])null));
		Assert.assertFalse(ArrayUtil.equals(new Object[] {}, (Object[])null));
		Assert.assertFalse(ArrayUtil.equals((Object[])null, new Object[] {}));
	}
	
	@Test
	public void otherTests() {
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		
		Collection<Object> col = ArrayUtil.intersectionIdentity(new Object[] { o1,  o2, o3 }, new Object[] { o4, o1, o3 });
		Assert.assertTrue(col.contains(o1));
		Assert.assertFalse(col.contains(o2));
		Assert.assertTrue(col.contains(o3));
		Assert.assertFalse(col.contains(o4));
		
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.concatenate(new Object[] { o1,  o2, o3 }, new Object[] { o2, o4 }), new Object[] { o1, o2, o3, o2, o4 }));
		
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.merge(Arrays.asList(new byte[] { 0, 1, 2 }, new byte[] { 10, 20, 30 }, new byte[] { 90 })), new byte[] { 0, 1, 2, 10, 20, 30, 90 }));
		
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 0,  1, 2 } ,new byte[] { 10, 20, 30 }) < 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 10,  1, 2 } ,new byte[] { 10, 20, 30 }) < 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 11,  1, 2 } ,new byte[] { 10, 20, 30 }) > 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 0,  1, 2 } ,new byte[] { 0, 1, 2 }) == 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 10,  20, 21 } ,new byte[] { 10, 20, 30 }) < 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 10,  20, 31 } ,new byte[] { 10, 20, 30 }) > 0);
		Assert.assertTrue(ArrayUtil.compare(new byte[] { 10,  20, 30, 0 } ,new byte[] { 10, 20, 30 }) > 0);
		
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.add(new Object[] { o1, o2 }, o3), new Object[] { o1, o2, o3 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.add(new Object[] { o1, o2 }, o3, 0), new Object[] { o3, o1, o2 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.add(new Object[] { o1, o2 }, o3, 1), new Object[] { o1, o3, o2 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.add(new Object[] { o1, o2 }, o3, 2), new Object[] { o1, o2, o3 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.add(new Object[] { o1, o2 }, Arrays.asList(o4, o3)), new Object[] { o1, o2, o4, o3 }));
		
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.remove(new Object[] { o1, o2, o3, o4 }, o3), new Object[] { o1, o2, o4 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.remove(new Object[] { o1, o2, o3, o4 }, o1), new Object[] { o2, o3, o4 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.remove(new Object[] { o1, o2, o3, o4 }, o4), new Object[] { o1, o2, o3 }));
		
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.removeAt(new Object[] { o1, o2, o3, o4 }, 2), new Object[] { o1, o2, o4 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.removeAt(new Object[] { o1, o2, o3, o4 }, 0), new Object[] { o2, o3, o4 }));
		Assert.assertTrue(ArrayUtil.equals(ArrayUtil.removeAt(new Object[] { o1, o2, o3, o4 }, 3), new Object[] { o1, o2, o3 }));
	}
	
	@Test(timeout=30000)
	public void testArrayIterator() {
		ArrayIterator<Integer> it = new ArrayIterator<>(new Integer[] { Integer.valueOf(10), Integer.valueOf(20) });
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(10, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(20, it.next().intValue());
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("Iterator must throw NoSuchElementException");
		} catch (NoSuchElementException e) {
		}
	}
	
}

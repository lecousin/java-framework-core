package net.lecousin.framework.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** Object that contains 3 objects.
 * @param <T1> type of first object
 * @param <T2> type of second object
 * @param <T3> type of third object
 */
public class Triple<T1,T2,T3> {

	/** Constructor. */
	public Triple(T1 value1, T2 value2, T3 value3) {
        super();
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }
	
	/** Constructor with null values. */
	public Triple() {
		this(null, null, null);
	}
    
    protected T1 value1;
    protected T2 value2;
    protected T3 value3;

    public T1 getValue1() { return value1; }
    
    public T2 getValue2() { return value2; }
    
    public T3 getValue3() { return value3; }
    
    public void setValue1(T1 value) { value1 = value; }
    
    public void setValue2(T2 value) { value2 = value; }
    
    public void setValue3(T3 value) { value3 = value; }
    
    @Override
    public boolean equals(Object obj) {
    	return (obj instanceof Triple) &&
    		ObjectUtil.equalsOrNull(value1, ((Triple<?,?,?>)obj).value1) &&
    		ObjectUtil.equalsOrNull(value2, ((Triple<?,?,?>)obj).value2) &&
    		ObjectUtil.equalsOrNull(value3, ((Triple<?,?,?>)obj).value3);
    }
    
    @Override
    public int hashCode() {
    	return (value1 == null ? 0 : value1.hashCode()) + (value2 == null ? 0 : value2.hashCode()) + (value3 == null ? 0 : value3.hashCode());
    }

    @Override
    public String toString() {
    	return new StringBuilder("{").append(value1).append(",").append(value2).append(",").append(value3).append("}").toString();
    }
    
    /** Serializable Triple.
	 * @param <T1> type of first object
	 * @param <T2> type of second object
	 * @param <T3> type of third object
     */
    public static class Serializable<T1 extends java.io.Serializable, T2 extends java.io.Serializable, T3 extends java.io.Serializable>
    	extends Triple<T1, T2, T3> implements Externalizable {
    	/** Constructor for serialization purpose only.
         */
    	public Serializable() {
    		super();
    	}
    	
    	/** Constructor. */
    	public Serializable(T1 value1, T2 value2, T3 value3) {
    		super(value1, value2, value3);
    	}

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
        	out.writeObject(value1);
        	out.writeObject(value2);
        	out.writeObject(value3);
        }
        
        @Override
        @SuppressWarnings("unchecked")
    	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        	value1 = (T1)in.readObject();
        	value2 = (T2)in.readObject();
        	value3 = (T3)in.readObject();
        }

    }

}

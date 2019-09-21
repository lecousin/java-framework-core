package net.lecousin.framework.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** Object that contains 2 objects.
 * @param <T1> type of first object
 * @param <T2> type of second object
 */
public class Pair<T1,T2> {

	/** Constructor. */
	public Pair(T1 value1, T2 value2) {
    	this.value1 = value1;
    	this.value2 = value2;
    }
	
	/** Constructor with null values. */
	public Pair() {
		this(null, null);
	}
    
    protected T1 value1;
    protected T2 value2;

    public T1 getValue1() { return value1; }
    
    public T2 getValue2() { return value2; }
    
    public void setValue1(T1 value) { value1 = value; }
    
    public void setValue2(T2 value) { value2 = value; }
    
	@Override
    public boolean equals(Object obj) {
    	return (obj instanceof Pair) &&
    		ObjectUtil.equalsOrNull(value1, ((Pair<?,?>)obj).value1) &&
    		ObjectUtil.equalsOrNull(value2, ((Pair<?,?>)obj).value2)
    	;
    }
    
    @Override
    public int hashCode() {
    	return (value1 == null ? 0 : value1.hashCode()) + (value2 == null ? 0 : value2.hashCode());
    }
        
    @Override
    public java.lang.String toString() {
    	return new StringBuilder("{").append(value1).append(",").append(value2).append("}").toString();
    }
    
    /** Serializable Pair.
	 * @param <T1> type of first object
	 * @param <T2> type of second object
     */
    public static class Serializable<T1 extends java.io.Serializable, T2 extends java.io.Serializable>
    	extends Pair<T1, T2> implements Externalizable {
    	/** Constructor for serialization purpose only.
         */
    	public Serializable() {
    		super();
    	}
    	
    	/** Constructor. */
    	public Serializable(T1 value1, T2 value2) {
    		super(value1, value2);
    	}

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
        	out.writeObject(value1);
        	out.writeObject(value2);
        }
        
        @Override
        @SuppressWarnings("unchecked")
    	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        	value1 = (T1)in.readObject();
        	value2 = (T2)in.readObject();
        }

    }
}

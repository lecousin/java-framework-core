package net.lecousin.framework.util;

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
    
    private T1 value1;
    private T2 value2;
    private T3 value3;

    public T1 getValue1() { return value1; }
    
    public T2 getValue2() { return value2; }
    
    public T3 getValue3() { return value3; }
    
    public void setValue1(T1 value) { value1 = value; }
    
    public void setValue2(T2 value) { value2 = value; }
    
    public void setValue3(T3 value) { value3 = value; }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null || !(obj instanceof Triple)) return false;
    	if (!ObjectUtil.equalsOrNull(value1, ((Triple<?,?,?>)obj).value1))
    		return false;
    	if (!ObjectUtil.equalsOrNull(value2, ((Triple<?,?,?>)obj).value2))
    		return false;
    	if (!ObjectUtil.equalsOrNull(value3, ((Triple<?,?,?>)obj).value3))
    		return false;
    	return true;
    }
    
    @Override
    public int hashCode() {
    	return (value1 == null ? 0 : value1.hashCode()) + (value2 == null ? 0 : value2.hashCode()) + (value3 == null ? 0 : value3.hashCode());
    }

    @Override
    public String toString() {
    	return new StringBuilder("{").append(value1 == null ? "null" : value1.toString()).append(",")
    		.append(value2 == null ? "null" : value2.toString()).append(",")
    		.append(value3 == null ? "null" : value3.toString()).append("}").toString();
    }
}

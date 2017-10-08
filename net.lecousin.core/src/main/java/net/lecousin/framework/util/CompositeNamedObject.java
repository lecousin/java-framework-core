package net.lecousin.framework.util;

import java.util.ArrayList;

import net.lecousin.framework.locale.ILocalizableString;

/** Simple utility class that contains several named objects. */
public class CompositeNamedObject {

	public ArrayList<Pair<ILocalizableString,Object>> objects = new ArrayList<>();
	
	/** Add an object. */
	public void add(ILocalizableString name, Object object) {
		objects.add(new Pair<>(name, object));
	}
	
	/** Return the object at the given index. */
	public Object get(int index) {
		return objects.get(index).getValue2();
	}
	
	/** Return the name of the object at the given index. */
	public ILocalizableString getName(int index) {
		return objects.get(index).getValue1();
	}
	
}

package net.lecousin.framework.util;

import java.util.HashMap;

/** Abstract class implementing AttributesContainer with a HashMap. */
public abstract class AbstractAttributesContainer implements AttributesContainer {

	private HashMap<String,Object> attributes = new HashMap<>(20);

	@Override
	public void setAttribute(String key, Object value) { attributes.put(key, value); }
	
	@Override
	public Object getAttribute(String key) { return attributes.get(key); }
	
	@Override
	public Object removeAttribute(String key) { return attributes.remove(key); }
	
	@Override
	public boolean hasAttribute(String name) { return attributes.containsKey(name); }

}

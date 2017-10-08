package net.lecousin.framework.util;

/** Base interface to match a string against a pattern. */
public interface IStringPattern {

	/** Return true if the given string matches this pattern. */
	public boolean matches(String s);
	
}

package net.lecousin.framework.text;

/** Base interface to match a string against a pattern. */
public interface IStringPattern {

	/** Return true if the given string matches this pattern. */
	boolean matches(String s);
	
}

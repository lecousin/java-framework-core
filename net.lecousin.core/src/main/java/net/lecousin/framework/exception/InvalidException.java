package net.lecousin.framework.exception;

import net.lecousin.framework.locale.LocalizableString;

/** Invalid value. */
@SuppressWarnings("serial")
public class InvalidException extends LocalizableException {

	/** Constructor. */
	public InvalidException(String namespace, String string, String value) {
		super(new LocalizableString("b", "Invalid _", new LocalizableString(namespace, string), value));
	}
	
}

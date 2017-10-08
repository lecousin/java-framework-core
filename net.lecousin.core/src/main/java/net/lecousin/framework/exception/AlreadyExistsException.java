package net.lecousin.framework.exception;

import net.lecousin.framework.locale.LocalizableString;

/** Error when an element already exists. */
@SuppressWarnings("serial")
public class AlreadyExistsException extends LocalizableException {

	/** Constructor. */
	public AlreadyExistsException(String namespace, String string, String value) {
		super(new LocalizableString("b", "_ _ already exists", new LocalizableString(namespace, string), value));
	}
	
}

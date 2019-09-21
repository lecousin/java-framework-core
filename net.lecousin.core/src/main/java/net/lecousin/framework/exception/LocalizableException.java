package net.lecousin.framework.exception;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.locale.LocalizableString;

/** Exception that can be translated. */
@SuppressWarnings("serial")
public class LocalizableException extends Exception {

	/** Constructor. */
	public LocalizableException(ILocalizableString string, Throwable cause) {
		super(cause);
		this.string = string;
	}

	/** Constructor. */
	public LocalizableException(ILocalizableString string) {
		this(string, null);
	}

	/** Constructor. */
	public LocalizableException(String namespace, String string, Object... values) {
		this(new LocalizableString(namespace, string, values), null);
	}

	/** Constructor. */
	public LocalizableException(String message, Throwable cause) {
		super(message, cause);
		string = null;
	}

	/** Constructor. */
	public LocalizableException(String message) {
		super(message);
		string = null;
	}
	
	private final ILocalizableString string;
	
	@Override
	public String getMessage() {
		if (string == null) return super.getMessage();
		return string.localizeSync(LCCore.getApplication().getLanguageTag());
	}
	
	public ILocalizableString getLocalizable() {
		return string;
	}
	
}

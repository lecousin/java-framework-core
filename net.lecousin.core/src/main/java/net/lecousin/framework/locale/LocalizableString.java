package net.lecousin.framework.locale;

import java.util.Arrays;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;

/**
 * A string that can be localized using LocalizedProperties.
 */
public class LocalizableString implements ILocalizableString {

	/** Constructor. */
	public LocalizableString(String namespace, String string, Object... values) {
		this.namespace = namespace;
		this.string = string;
		this.values = values;
	}
	
	private String namespace;
	private String string;
	private Object[] values;
	
	public String getNamespace() {
		return namespace;
	}
	
	public String getString() {
		return string;
	}
	
	public Object[] getValues() {
		return values;
	}
	
	@Override
	public AsyncWork<String, NoException> localize(String[] languageTag) {
		return LCCore.getApplication().getLocalizedProperties().localize(languageTag, namespace, string, values);
	}
	
	@Override
	public String localizeSync(String[] languageTag) {
		return LCCore.getApplication().getLocalizedProperties().localizeSync(languageTag, namespace, string, values);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LocalizableString)) return false;
		LocalizableString o = (LocalizableString)obj;
		return namespace.equals(o.namespace) && string.equals(o.string) && Arrays.equals(values, o.values);
	}
	
	@Override
	public int hashCode() {
		return string.hashCode();
	}
	
}

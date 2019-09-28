package net.lecousin.framework.locale;

import java.io.Serializable;
import java.util.Arrays;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.exception.NoException;

/**
 * A string that can be localized using LocalizedProperties.
 */
public class LocalizableString implements ILocalizableString {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public LocalizableString(String namespace, String string, Serializable... values) {
		this.namespace = namespace;
		this.string = string;
		this.values = values;
	}
	
	private String namespace;
	private String string;
	private Serializable[] values;
	
	public String getNamespace() {
		return namespace;
	}
	
	public String getString() {
		return string;
	}
	
	public Serializable[] getValues() {
		return values;
	}
	
	@Override
	public AsyncSupplier<String, NoException> localize(String[] languageTag) {
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

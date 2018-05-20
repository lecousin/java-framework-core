package net.lecousin.framework.locale;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;

/**
 * Implement ILocalizableString with a fixed value (not localized).
 */
public class FixedLocalizedString implements ILocalizableString {

	/** Constructor. */
	public FixedLocalizedString(String fixedString) {
		str = fixedString;
	}
	
	private String str;
	
	@Override
	public AsyncWork<String, NoException> localize(String[] languageTag) {
		return new AsyncWork<>(str, null);
	}
	
	@Override
	public AsyncWork<String, NoException> localize(String languageTag) {
		return new AsyncWork<>(str, null);
	}
	
	@Override
	public String localizeSync(String languageTag) {
		return str;
	}
	
	@Override
	public String localizeSync(String[] languageTag) {
		return str;
	}
	
	@Override
	public AsyncWork<String, NoException> appLocalization() {
		return new AsyncWork<>(str, null);
	}
	
	@Override
	public String appLocalizationSync() {
		return str;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FixedLocalizedString)) return false;
		return str.equals(((FixedLocalizedString)obj).str);
	}
	
	@Override
	public int hashCode() {
		return str.hashCode();
	}
	
}

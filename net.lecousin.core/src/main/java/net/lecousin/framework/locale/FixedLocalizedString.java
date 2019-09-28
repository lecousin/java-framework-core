package net.lecousin.framework.locale;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.exception.NoException;

/**
 * Implement ILocalizableString with a fixed value (not localized).
 */
public class FixedLocalizedString implements ILocalizableString {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public FixedLocalizedString(String fixedString) {
		str = fixedString;
	}
	
	private String str;
	
	@Override
	public AsyncSupplier<String, NoException> localize(String[] languageTag) {
		return new AsyncSupplier<>(str, null);
	}
	
	@Override
	public AsyncSupplier<String, NoException> localize(String languageTag) {
		return new AsyncSupplier<>(str, null);
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
	public AsyncSupplier<String, NoException> appLocalization() {
		return new AsyncSupplier<>(str, null);
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

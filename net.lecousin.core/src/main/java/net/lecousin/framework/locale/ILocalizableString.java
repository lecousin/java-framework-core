package net.lecousin.framework.locale;

import java.io.Serializable;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.exception.NoException;

/** Interface for an object that can be localized into a string. */
public interface ILocalizableString extends Serializable {

	/** Return the localized string, based on language tag. */
	AsyncSupplier<String, NoException> localize(String[] languageTag);

	/** Return the localized string, based on language tag. */
	default AsyncSupplier<String, NoException> localize(String languageTag) {
		return localize(languageTag.split("-"));
	}

	/** Return the localized string, based on language tag. */
	String localizeSync(String[] languageTag);

	/** Return the localized string, based on language tag. */
	default String localizeSync(String languageTag) {
		return localizeSync(languageTag.split("-"));
	}

	/** Return the string localized in the application language. */
	default AsyncSupplier<String, NoException> appLocalization() {
		return localize(LCCore.getApplication().getLanguageTag());
	}
	
	/** Return the string localized in the application language. */
	default String appLocalizationSync() {
		return localizeSync(LCCore.getApplication().getLanguageTag());
	}
	
}

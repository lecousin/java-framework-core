package net.lecousin.framework.locale;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;

/** Interface for an object that can be localized into a string. */
public interface ILocalizableString {

	/** Return the localized string, based on language tag. */
	public AsyncWork<String, NoException> localize(String[] languageTag);

	/** Return the localized string, based on language tag. */
	public default AsyncWork<String, NoException> localize(String languageTag) {
		return localize(languageTag.split("-"));
	}

	/** Return the localized string, based on language tag. */
	public String localizeSync(String[] languageTag);

	/** Return the localized string, based on language tag. */
	public default String localizeSync(String languageTag) {
		return localizeSync(languageTag.split("-"));
	}

	/** Return the string localized in the application language. */
	public default AsyncWork<String, NoException> appLocalization() {
		return localize(LCCore.getApplication().getLanguageTag());
	}
	
	/** Return the string localized in the application language. */
	public default String appLocalizationSync() {
		return localizeSync(LCCore.getApplication().getLanguageTag());
	}
	
}

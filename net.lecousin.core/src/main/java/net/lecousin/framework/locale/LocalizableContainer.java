package net.lecousin.framework.locale;

/**
 * Something that contain localizable data, on which we can ask to update localized elements with the new application language.
 */
public interface LocalizableContainer {

	/** Update with current application language. */
	void updateLocalizedElements();
	
}

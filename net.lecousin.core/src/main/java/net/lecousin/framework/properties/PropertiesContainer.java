package net.lecousin.framework.properties;

import java.util.Collection;

import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.util.Pair;

/**
 * Object that contains a set of properties.
 */
public interface PropertiesContainer {

	/** Return the properties of this object. */
	public Collection<Pair<ILocalizableString,Object>> getProperties();
	
}

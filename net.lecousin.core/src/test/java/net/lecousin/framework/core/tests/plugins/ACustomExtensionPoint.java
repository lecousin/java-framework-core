package net.lecousin.framework.core.tests.plugins;

import net.lecousin.framework.plugins.CustomExtensionPoint;

public class ACustomExtensionPoint implements CustomExtensionPoint {

	@Override
	public boolean keepAfterInit() {
		return true;
	}
	
}

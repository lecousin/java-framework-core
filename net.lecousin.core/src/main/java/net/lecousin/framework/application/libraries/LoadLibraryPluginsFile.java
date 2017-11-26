package net.lecousin.framework.application.libraries;

import java.io.IOException;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.FullReadLines;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.plugins.Plugin;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Task to load a plugins file.
 */
public class LoadLibraryPluginsFile extends FullReadLines<Void> {
	
	/** Constructor. */
	public LoadLibraryPluginsFile(BufferedReadableCharacterStream stream, ApplicationClassLoader classLoader) {
		super("Initializing plugins: " + stream.getDescription(), stream, Task.PRIORITY_IMPORTANT, IO.OperationType.ASYNCHRONOUS);
		this.classLoader = classLoader;
	}
	
	private ApplicationClassLoader classLoader;
	
	@Override
	protected void processLine(UnprotectedStringBuffer line) throws IOException {
		int i = line.indexOf(':');
		if (i < 0) {
			if (line.length() > 0 && classLoader.getApplication().getDefaultLogger().warn())
				classLoader.getApplication().getDefaultLogger()
					.warn("Warning: plugins file " + getSourceDescription() + " contains an invalid line: " + line);
			return;
		}
		UnprotectedStringBuffer s = line.substring(0, i);
		s.trim();
		String epName = s.asString();
		s = line.substring(i + 1);
		s.trim();
		String piName = s.asString();
		if (classLoader.getApplication().getDefaultLogger().debug())
			classLoader.getApplication().getDefaultLogger().debug("Plugin " + piName + " found for extension point " + epName);
		try {
			Class<?> cl = Class.forName(piName, true, classLoader);
			if (!Plugin.class.isAssignableFrom(cl))
				throw new Exception("Invalid plugin class: " + line.asString() + " must extend Plugin");
			Plugin pi = (Plugin)cl.newInstance();
			ExtensionPoints.add(epName, pi);
		} catch (Exception e) {
			throw new IOException("Error loading plugins of library: " + getSourceDescription(), e);
		}
	}
	
	@Override
	protected Void generateResult() {
		return null;
	}

}

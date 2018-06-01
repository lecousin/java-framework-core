package net.lecousin.framework.application.libraries.classpath;

import java.io.IOException;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.FullReadLines;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Task to read an extensionpoints file.
 */
public class LoadLibraryExtensionPointsFile extends FullReadLines<Void> {
	
	/** Constructor. */
	public <T extends ClassLoader & ApplicationClassLoader> LoadLibraryExtensionPointsFile(
		BufferedReadableCharacterStream stream, T classLoader
	) {
		super("Initializing extension points: " + stream.getDescription(), stream,
				Task.PRIORITY_IMPORTANT, IO.OperationType.ASYNCHRONOUS);
		this.classLoader = classLoader;
	}
	
	private ClassLoader classLoader;
	
	@Override
	protected void processLine(UnprotectedStringBuffer line) throws IOException {
		try {
			Class<?> cl = Class.forName(line.asString(), true, classLoader);
			if (ExtensionPoint.class.isAssignableFrom(cl)) {
				ExtensionPoint<?> ext = (ExtensionPoint<?>)cl.newInstance();
				ExtensionPoints.add(ext);
				((ApplicationClassLoader)classLoader).getApplication().getDefaultLogger().info("Extension point: " + cl.getName());
			} else if (CustomExtensionPoint.class.isAssignableFrom(cl)) {
				CustomExtensionPoint ext = (CustomExtensionPoint)cl.newInstance();
				ExtensionPoints.add(ext);
			} else
				throw new Exception("Invalid extension point class: " + line + " must extend ExtensionPoint or CustomExtensionPoint");
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	@Override
	protected Void generateResult() {
		return null;
	}

}

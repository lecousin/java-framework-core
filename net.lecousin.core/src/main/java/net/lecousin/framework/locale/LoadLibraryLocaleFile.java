package net.lecousin.framework.locale;

import java.io.IOException;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.FullReadLines;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Load file META-INF/net.lecousin/locale from a library.
 */
public class LoadLibraryLocaleFile extends FullReadLines<Void> {
	
	/** Constructor. */
	public LoadLibraryLocaleFile(BufferedReadableCharacterStream stream, ApplicationClassLoader classLoader) {
		super("Initializing localized properties: " + stream.getSourceDescription(),
			stream, Task.PRIORITY_IMPORTANT, IO.OperationType.ASYNCHRONOUS);
		this.classLoader = classLoader;
	}
	
	private ApplicationClassLoader classLoader;
	
	@Override
	protected void processLine(UnprotectedStringBuffer line) throws IOException {
		int i = line.indexOf('=');
		if (i <= 0) return;
		UnprotectedStringBuffer s = line.substring(0, i);
		line = line.substring(i + 1);
		line.trim();
		s.trim();
		try { classLoader.getApplication().getLocalizedProperties().registerNamespace(s.asString(), line.asString(), classLoader); }
		catch (Exception e) { throw new IOException(e); }
	}
	
	@Override
	protected Void generateResult() {
		return null;
	}
}


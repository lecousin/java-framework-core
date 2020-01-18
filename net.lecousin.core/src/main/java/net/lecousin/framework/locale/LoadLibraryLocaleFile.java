package net.lecousin.framework.locale;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.FullReadLines;
import net.lecousin.framework.text.CharArrayStringBuffer;

/**
 * Load file META-INF/net.lecousin/locale from a library.
 */
public class LoadLibraryLocaleFile extends FullReadLines<Void> {
	
	/** Constructor. */
	public <T extends ClassLoader & ApplicationClassLoader> LoadLibraryLocaleFile(BufferedReadableCharacterStream stream, T classLoader) {
		super("Initializing localized properties: " + stream.getDescription(),
			stream, Task.PRIORITY_IMPORTANT, null);
		this.classLoader = classLoader;
	}
	
	private ClassLoader classLoader;
	
	@Override
	protected void processLine(CharArrayStringBuffer line) {
		int i = line.indexOf('=');
		if (i <= 0) return;
		CharArrayStringBuffer s = line.substring(0, i);
		line = line.substring(i + 1);
		line.trim();
		s.trim();
		((ApplicationClassLoader)classLoader).getApplication().getLocalizedProperties()
			.registerNamespace(s.asString(), line.asString(), classLoader);
	}
	
	@Override
	protected Void generateResult() {
		return null;
	}
}


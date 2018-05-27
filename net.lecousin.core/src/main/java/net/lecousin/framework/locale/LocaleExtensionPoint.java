package net.lecousin.framework.locale;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.plugins.CustomExtensionPoint;

/**
 * Extension point that loads localized properties from the list in META-INF/net.lecousin/locale.
 */
public class LocaleExtensionPoint implements CustomExtensionPoint {

	@Override
	public String getPluginConfigurationFilePath() {
		return "META-INF/net.lecousin/locale";
	}
	
	@Override
	public boolean keepAfterInit() {
		return false;
	}
	
	@Override
	public <T extends ClassLoader & ApplicationClassLoader> ISynchronizationPoint<Exception> loadPluginConfiguration(
		IO.Readable io, T classLoader, ISynchronizationPoint<?>... startOn
	) {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Task<Void,Exception> task = new Task.Cpu<Void,Exception>("Loading locale file", Task.PRIORITY_NORMAL) {
			@SuppressWarnings("resource")
			@Override
			public Void run() {
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryLocaleFile load = new LoadLibraryLocaleFile(stream, classLoader);
				load.start().listenInline(sp);
				return null;
			}
		};
		task.startOn(false, startOn);
		return sp;
	}
	
}

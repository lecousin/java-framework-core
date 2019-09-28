package net.lecousin.framework.locale;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
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
	public <T extends ClassLoader & ApplicationClassLoader> IAsync<Exception> loadPluginConfiguration(
		IO.Readable io, T classLoader, IAsync<?>... startOn
	) {
		Async<Exception> sp = new Async<>();
		Task<Void,Exception> task = new Task.Cpu<Void,Exception>("Loading locale file", Task.PRIORITY_NORMAL) {
			@SuppressWarnings("resource")
			@Override
			public Void run() {
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryLocaleFile load = new LoadLibraryLocaleFile(stream, classLoader);
				load.start().onDone(sp);
				return null;
			}
		};
		task.startOn(false, startOn);
		return sp;
	}
	
}

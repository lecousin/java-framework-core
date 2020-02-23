package net.lecousin.framework.core.tests.plugins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.text.CharArrayStringBuffer;

public class ACustomExtensionPointWithFile implements CustomExtensionPoint {

	@Override
	public boolean keepAfterInit() {
		return true;
	}
	
	@Override
	public String getPluginConfigurationFilePath() {
		return "META-INF/net.lecousin/testCustom";
	}
	
	public String pluginContent = null;
	
	@Override
	public <T extends ClassLoader & ApplicationClassLoader> IAsync<Exception> loadPluginConfiguration(
		IO.Readable io, T libraryClassLoader, IAsync<?>... startOn
	) {
		AsyncSupplier<CharArrayStringBuffer, IOException> task = IOUtil.readFullyAsString(io, StandardCharsets.UTF_8, Task.Priority.NORMAL);
		Async<Exception> sp = new Async<>();
		task.onDone(() -> {
			pluginContent = task.getResult().asString();
			sp.unblock();
		}, sp, e -> e);
		return sp;
	}
	
}

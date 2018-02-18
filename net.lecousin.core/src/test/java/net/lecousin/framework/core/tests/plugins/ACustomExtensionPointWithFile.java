package net.lecousin.framework.core.tests.plugins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.util.UnprotectedStringBuffer;

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
	public ISynchronizationPoint<Exception> loadPluginConfiguration(IO.Readable io,
		ApplicationClassLoader libraryClassLoader, ISynchronizationPoint<?>... startOn) {
		Task<UnprotectedStringBuffer, IOException> task = IOUtil.readFullyAsString(io, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL);
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		task.getOutput().listenInlineSP(() -> {
			pluginContent = task.getResult().asString();
			sp.unblock();
		}, sp);
		return sp;
	}
	
}

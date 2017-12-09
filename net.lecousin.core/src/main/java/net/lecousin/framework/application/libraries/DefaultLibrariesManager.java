package net.lecousin.framework.application.libraries;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.VersionSpecification;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.provider.IOProviderFromName;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Default implementation of LibrariesManager.
 * This implementation is not capable to load libraries dynamically, and only uses those in the class path.
 * It loads extension points and plugins from libraries present in the class path.
 */
public class DefaultLibrariesManager implements LibrariesManager {

	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	@Override
	public ApplicationClassLoader start(Application app) {
		this.app = app;
		acl = new DefaultApplicationClassLoader(app);
		new Start().start();
		return acl;
	}
	
	private Application app;
	private ApplicationClassLoader acl;
	private SynchronizationPoint<Exception> started = new SynchronizationPoint<>();
	
	private class Start extends Task.Cpu<Void, NoException> {
		private Start() {
			super("Start DefaultLibrariesManager", Task.PRIORITY_IMPORTANT);
		}
		
		@Override
		public Void run() {
			app.getDefaultLogger().info("Starting DefaultLibrariesManager with classpath = " + System.getProperty("java.class.path"));
			
			ExtensionPointsLoader extensionpoints = new ExtensionPointsLoader();
			extensionpoints.start();
			
			LinkedList<ISynchronizationPoint<Exception>> tasks = new LinkedList<>();
			tasks.add(extensionpoints.getOutput());
			for (CustomExtensionPoint custom : ExtensionPoints.getCustomExtensionPoints()) {
				String path = custom.getPluginConfigurationFilePath();
				if (path == null) continue;
				CustomExtensionPointLoader loader = new CustomExtensionPointLoader(custom, path);
				tasks.getLast().listenAsync(loader, false);
				tasks.add(loader.getOutput());
			}
			
			PluginsLoader plugins = new PluginsLoader();
			tasks.getLast().listenAsync(plugins, false);
			tasks.add(plugins.getOutput());
			tasks.getLast().listenAsync(new Task.Cpu<Void, NoException>("Finalize libraries loading", Task.PRIORITY_NORMAL) {
				@Override
				public Void run() {
					for (ISynchronizationPoint<Exception> task : tasks) {
						if (!task.isSuccessful()) {
							started.error(task.getError());
							return null;
						}
					}
					ExtensionPoints.allPluginsLoaded();
					started.unblock();
					return null;
				}
			}, true);
			
			return null;
		}
	}
	
	@Override
	public ISynchronizationPoint<Exception> onLibrariesLoaded() {
		return started;
	}
	
	@Override
	public boolean canLoadNewLibraries() {
		return false;
	}
	
	@Override
	public AsyncWork<Library, Exception> loadNewLibrary(
		String groupId, String artifactId, VersionSpecification version, boolean optional,
		byte priority, WorkProgress progress, long work
	) {
		return new AsyncWork<>(null, new Exception("DefaultLibrariesManager cannot load new libraries"));
	}

	@SuppressWarnings("resource")
	@Override
	public IO.Readable getResourceFrom(ClassLoader cl, String path, byte priority) {
		if (cl instanceof IOProviderFromName.Readable)
			try { return ((IOProviderFromName.Readable)cl).provideReadableIO(path, priority); }
			catch (IOException e) {
				app.getDefaultLogger().info("Resource not found: " + path, e);
				return null;
			}
		URL url = cl.getResource(path);
		if (url == null) {
			app.getDefaultLogger().info("Resource not found: " + path);
			return null;
		}
		InputStream in;
		try { in = url.openStream(); }
		catch (Exception e) {
			app.getDefaultLogger().error("Unable to open resource " + path, e);
			return null;
		}
		TaskManager tm = null;
		if ("file".equals(url.getProtocol()))
			tm = Threading.getDrivesTaskManager().getTaskManager(url.getFile());
		if (tm == null)
			tm = Threading.getCPUTaskManager();
		return new IOFromInputStream(in, path, tm, priority);
	}
	
	@Override
	public IO.Readable getResource(String groupId, String artifactId, String path, byte priority) {
		return getResourceFrom(getClass(), path, priority);
	}
	
	@Override
	public Library getLibrary(ClassLoader cl) {
		return null;
	}
	
	@Override
	public Library getLibrary(String groupId, String artifactId) {
		return null;
	}
	
	@Override
	public List<File> getLibrariesLocations() {
		String[] paths = System.getProperty("java.class.path").split(";");
		ArrayList<File> list = new ArrayList<>(paths.length);
		for (String path : paths) {
			path = path.trim();
			if (path.length() == 0) continue;
			File f = new File(path);
			if (!f.exists()) continue;
			list.add(f);
		}
		return list;
	}
	
	private class CustomExtensionPointLoader extends Task.Cpu<Void, Exception> {
		public CustomExtensionPointLoader(CustomExtensionPoint ep, String filePath) {
			super("Loading libraries' file " + filePath + " for extension point " + ep.getClass().getName(), Task.PRIORITY_NORMAL);
			this.ep = ep;
			this.filePath = filePath;
		}
		
		private CustomExtensionPoint ep;
		private String filePath;
		
		@Override
		public Void run() throws Exception {
			Enumeration<URL> urls = acl.getResources(filePath);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				@SuppressWarnings("resource")
				InputStream input = url.openStream();
				@SuppressWarnings("resource")
				IOFromInputStream io = new IOFromInputStream(
					input, url.toString(), Threading.getCPUTaskManager(), Task.PRIORITY_IMPORTANT);
				ep.loadPluginConfiguration(io, acl).block(0);
			}
			return null;
		}
	}
	
	private class ExtensionPointsLoader extends Task.Cpu<Void, Exception> {
		public ExtensionPointsLoader() {
			super("Loading extensionpoints files for libraries", Task.PRIORITY_NORMAL);
		}
		
		@Override
		public Void run() throws Exception {
			Enumeration<URL> urls = acl.getResources("META-INF/net.lecousin/extensionpoints");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				@SuppressWarnings("resource")
				InputStream input = url.openStream();
				@SuppressWarnings("resource")
				IOFromInputStream io = new IOFromInputStream(
					input, url.toString(), Threading.getCPUTaskManager(), Task.PRIORITY_IMPORTANT);
				@SuppressWarnings("resource")
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryExtensionPointsFile load = new LoadLibraryExtensionPointsFile(stream, acl);
				load.startOn(stream.canStartReading(), false);
				load.getOutput().block(0);
			}
			return null;
		}
	}
	
	private class PluginsLoader extends Task.Cpu<Void, Exception> {
		public PluginsLoader() {
			super("Loading plugins files for libraries", Task.PRIORITY_NORMAL);
		}
		
		@Override
		public Void run() throws Exception {
			Enumeration<URL> urls = acl.getResources("META-INF/net.lecousin/plugins");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				@SuppressWarnings("resource")
				InputStream input = url.openStream();
				@SuppressWarnings("resource")
				IOFromInputStream io = new IOFromInputStream(
					input, url.toString(), Threading.getCPUTaskManager(), Task.PRIORITY_IMPORTANT);
				@SuppressWarnings("resource")
				BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
				LoadLibraryPluginsFile load = new LoadLibraryPluginsFile(stream, acl);
				load.startOn(stream.canStartReading(), false);
				load.getOutput().block(0);
			}
			return null;
		}
	}
	
}

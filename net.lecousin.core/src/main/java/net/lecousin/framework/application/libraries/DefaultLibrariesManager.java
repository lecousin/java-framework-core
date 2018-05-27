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
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.plugins.CustomExtensionPoint;
import net.lecousin.framework.plugins.ExtensionPoints;

/**
 * Default implementation of LibrariesManager.
 * This implementation is not capable to load libraries dynamically, and only uses those in the class path.
 * It loads extension points and plugins from libraries present in the class path.
 */
public class DefaultLibrariesManager implements LibrariesManager {

	public DefaultLibrariesManager(File[] additionalClassPath) {
		this.additionalClassPath = additionalClassPath;
	}
	
	public DefaultLibrariesManager() {
		this(null);
	}
	
	@SuppressWarnings("unchecked")
	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	@Override
	public DefaultApplicationClassLoader start(Application app) {
		this.app = app;
		acl = new DefaultApplicationClassLoader(app, additionalClassPath);
		new Start().start();
		return acl;
	}
	
	private Application app;
	private DefaultApplicationClassLoader acl;
	private SynchronizationPoint<Exception> started = new SynchronizationPoint<>();
	private File[] additionalClassPath;
	private ArrayList<File> classPath;
	
	private class Start extends Task.Cpu<Void, NoException> {
		private Start() {
			super("Start DefaultLibrariesManager", Task.PRIORITY_IMPORTANT);
		}
		
		@Override
		public Void run() {
			String cp = System.getProperty("java.class.path");
			app.getDefaultLogger().info("Starting DefaultLibrariesManager with classpath = " + cp);
			URL[] addcp = acl.getURLs();
			for (int i = 0; i < addcp.length; ++i)
				app.getDefaultLogger().info(" - additional library: " + addcp[i].toString());
			
			String[] paths = cp.split(System.getProperty("path.separator"));
			classPath = new ArrayList<>(paths.length);
			for (String path : paths) {
				path = path.trim();
				if (path.length() == 0) continue;
				File f = new File(path);
				if (!f.exists()) continue;
				classPath.add(f);
			}
			if (additionalClassPath != null)
				for (File f : additionalClassPath)
					classPath.add(f);
			classPath.trimToSize();
			additionalClassPath = null;
			
			SynchronizationPoint<Exception> sp = loadExtensionPoints();
			
			LinkedList<ISynchronizationPoint<Exception>> tasks = new LinkedList<>();
			tasks.add(sp);
			sp.listenAsync(new Start2(tasks), true);
			return null;
		}
	}
	
	private class Start2 extends Task.Cpu<Void, NoException> {
		private Start2(LinkedList<ISynchronizationPoint<Exception>> tasks) {
			super("Start DefaultLibrariesManager - step 2", Task.PRIORITY_IMPORTANT);
			this.tasks = tasks;
		}
		
		private LinkedList<ISynchronizationPoint<Exception>> tasks;
		
		@Override
		public Void run() {
			for (CustomExtensionPoint custom : ExtensionPoints.getCustomExtensionPoints()) {
				String path = custom.getPluginConfigurationFilePath();
				if (path == null) continue;
				CustomExtensionPointLoader loader = new CustomExtensionPointLoader(custom, path);
				tasks.getLast().listenAsync(loader, false);
				tasks.add(loader.getOutput());
			}
			
			SynchronizationPoint<Exception> plugins = new SynchronizationPoint<>();
			tasks.getLast().listenAsync(new Task.Cpu.FromRunnable("Load plugins from libraries", Task.PRIORITY_NORMAL, () -> {
				loadPlugins(plugins);
			}), false);
			tasks.add(plugins);
			tasks.getLast().listenAsync(new Task.Cpu<Void, NoException>("Finalize libraries loading", Task.PRIORITY_NORMAL) {
				@Override
				public Void run() {
					ExtensionPoints.allPluginsLoaded();
					started.unblock();
					return null;
				}
			}, false);
			
			JoinPoint.fromSynchronizationPoints(tasks).listenInline(
				() -> { /* ok */ },
				(error) -> { started.error(error); },
				(cancel) -> { started.cancel(cancel); }
			);
			
			return null;
		}
	}
	
	@Override
	public ISynchronizationPoint<Exception> onLibrariesLoaded() {
		return started;
	}
	
	@SuppressWarnings("resource")
	@Override
	public IO.Readable getResource(String path, byte priority) {
		URL url = acl.getResource(path);
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
			tm = Threading.getUnmanagedTaskManager();
		return new IOFromInputStream(in, path, tm, priority);
	}
	
	@Override
	public List<File> getLibrariesLocations() {
		return classPath;
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
			app.getDefaultLogger().info("Loading plugin files for custom extension point " + ep.getClass().getName() + ": " + filePath);
			Enumeration<URL> urls = acl.getResources(filePath);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				app.getDefaultLogger().info(" - Plugin file found: " + url.toString());
				@SuppressWarnings("resource")
				InputStream input = url.openStream();
				@SuppressWarnings("resource")
				IOFromInputStream io = new IOFromInputStream(
					input, url.toString(), Threading.getUnmanagedTaskManager(), Task.PRIORITY_IMPORTANT);
				ep.loadPluginConfiguration(io, acl).blockThrow(0);
			}
			return null;
		}
	}
	
	private SynchronizationPoint<Exception> loadExtensionPoints() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Enumeration<URL> urls;
		try { urls = acl.getResources("META-INF/net.lecousin/extensionpoints"); }
		catch (IOException e) {
			sp.error(e);
			return sp;
		}
		loadExtensionPoints(urls, sp);
		return sp;
	}
	
	@SuppressWarnings("resource")
	private void loadExtensionPoints(Enumeration<URL> urls, SynchronizationPoint<Exception> sp) {
		if (!urls.hasMoreElements()) {
			sp.unblock();
			return;
		}
		URL url = urls.nextElement();
		InputStream input;
		try { input = url.openStream(); }
		catch (IOException e) {
			sp.error(e);
			return;
		}
		IOFromInputStream io = new IOFromInputStream(
			input, url.toString(), Threading.getUnmanagedTaskManager(), Task.PRIORITY_IMPORTANT);
		BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
		LoadLibraryExtensionPointsFile load = new LoadLibraryExtensionPointsFile(stream, acl);
		load.start().listenInline(() -> {
			loadExtensionPoints(urls, sp);
		}, sp);
	}

	private void loadPlugins(SynchronizationPoint<Exception> sp) {
		Enumeration<URL> urls;
		try { urls = acl.getResources("META-INF/net.lecousin/plugins"); }
		catch (IOException e) {
			sp.error(e);
			return;
		}
		loadPlugins(urls, sp);
	}
	
	@SuppressWarnings("resource")
	private void loadPlugins(Enumeration<URL> urls, SynchronizationPoint<Exception> sp) {
		if (!urls.hasMoreElements()) {
			sp.unblock();
			return;
		}
		URL url = urls.nextElement();
		InputStream input;
		try { input = url.openStream(); }
		catch (IOException e) {
			sp.error(e);
			return;
		}
		IOFromInputStream io = new IOFromInputStream(
			input, url.toString(), Threading.getUnmanagedTaskManager(), Task.PRIORITY_IMPORTANT);
		BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.UTF_8, 256, 32);
		LoadLibraryPluginsFile load = new LoadLibraryPluginsFile(stream, acl);
		load.start().listenInline(() -> {
			loadPlugins(urls, sp);
		}, sp);
	}
}

package net.lecousin.framework.application.libraries.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
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

	/** Constructor with additional files to load. */
	public DefaultLibrariesManager(File[] additionalClassPath) {
		this.additionalClassPath = additionalClassPath;
	}
	
	/** Constructor. */
	public DefaultLibrariesManager() {
		this(null);
	}
	
	@Override
	public DefaultApplicationClassLoader start(Application app) {
		this.app = app;
		acl = new DefaultApplicationClassLoader(app, additionalClassPath);
		new Start().start();
		return acl;
	}
	
	private Application app;
	private DefaultApplicationClassLoader acl;
	private Async<LibraryManagementException> started = new Async<>();
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
				if (path.isEmpty()) continue;
				File f = new File(path);
				if (!f.exists()) continue;
				classPath.add(f);
			}
			if (additionalClassPath != null)
				Collections.addAll(classPath, additionalClassPath);
			classPath.trimToSize();
			additionalClassPath = null;
			
			Async<Exception> sp = loadExtensionPoints();
			
			LinkedList<IAsync<Exception>> tasks = new LinkedList<>();
			tasks.add(sp);
			sp.thenStart(new Start2(tasks), true);
			return null;
		}

		private Async<Exception> loadExtensionPoints() {
			Async<Exception> sp = new Async<>();
			Enumeration<URL> urls;
			try { urls = acl.getResources("META-INF/net.lecousin/extensionpoints"); }
			catch (IOException e) {
				sp.error(e);
				return sp;
			}
			loadExtensionPoints(urls, sp);
			return sp;
		}
		
		private void loadExtensionPoints(Enumeration<URL> urls, Async<Exception> sp) {
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
			load.start().onDone(() -> loadExtensionPoints(urls, sp), sp);
			stream.closeAfter(sp);
		}
	}
	
	private class Start2 extends Task.Cpu<Void, NoException> {
		private Start2(LinkedList<IAsync<Exception>> tasks) {
			super("Start DefaultLibrariesManager - step 2", Task.PRIORITY_IMPORTANT);
			this.tasks = tasks;
		}
		
		private LinkedList<IAsync<Exception>> tasks;
		
		@Override
		public Void run() {
			for (CustomExtensionPoint custom : ExtensionPoints.getCustomExtensionPoints()) {
				String path = custom.getPluginConfigurationFilePath();
				if (path == null) continue;
				CustomExtensionPointLoader loader = new CustomExtensionPointLoader(custom, path);
				tasks.getLast().thenStart(loader, false);
				tasks.add(loader.getOutput());
			}
			
			Async<Exception> plugins = new Async<>();
			tasks.getLast().thenStart(
				new Task.Cpu.FromRunnable("Load plugins from libraries", Task.PRIORITY_NORMAL, () -> loadPlugins(plugins)), false);
			tasks.add(plugins);
			tasks.getLast().thenStart(new Task.Cpu<Void, NoException>("Finalize libraries loading", Task.PRIORITY_NORMAL) {
				@Override
				public Void run() {
					ExtensionPoints.allPluginsLoaded();
					started.unblock();
					return null;
				}
			}, false);
			
			JoinPoint.from(tasks).onDone(
				() -> { /* ok */ },
				error -> started.error(new LibraryManagementException("Error loading librairies", error)),
				cancel -> started.cancel(cancel)
			);
			
			return null;
		}
		
		private void loadPlugins(Async<Exception> sp) {
			Enumeration<URL> urls;
			try { urls = acl.getResources("META-INF/net.lecousin/plugins"); }
			catch (IOException e) {
				sp.error(e);
				return;
			}
			loadPlugins(urls, sp);
		}
		
		private void loadPlugins(Enumeration<URL> urls, Async<Exception> sp) {
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
			load.start().onDone(() -> loadPlugins(urls, sp), sp);
			stream.closeAfter(sp);
		}
	}
	
	@Override
	public IAsync<LibraryManagementException> onLibrariesLoaded() {
		return started;
	}
	
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
				InputStream input = url.openStream();
				IOFromInputStream io = new IOFromInputStream(
					input, url.toString(), Threading.getUnmanagedTaskManager(), Task.PRIORITY_IMPORTANT);
				ep.loadPluginConfiguration(io, acl).blockThrow(0);
			}
			return null;
		}
	}

	@Override
	public void scanLibraries(
		String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		List<File> files = getLibrariesLocations();
		for (File f : files) {
			if (f.isDirectory())
				scanDirectoryLibrary(app.getClassLoader(), f, rootPackage,
					includeSubPackages, packageFilter, classFilter, classScanner);
			else
				scanJarLibrary(app.getClassLoader(), f, rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
		}
	}
	
	/** Scan a directory looking for class files. */
	public static void scanDirectoryLibrary(
		ApplicationClassLoader classLoader, File dir, String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		String pkgPath = rootPackage.replace('.', '/');
		File rootDir = new File(dir, pkgPath);
		if (!rootDir.exists()) return;
		scanClasses(classLoader, rootDir, rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
	}
	
	private static void scanClasses(
		ApplicationClassLoader classLoader, File dir, String pkgName, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		File[] files = dir.listFiles();
		if (files == null) return;
		boolean filtered = packageFilter != null && !packageFilter.test(pkgName);
		for (File f : files) {
			if (f.isDirectory()) {
				if (!includeSubPackages) continue;
				scanClasses(classLoader, f, pkgName + '.' + f.getName(), true, packageFilter, classFilter, classScanner);
			} else if (!filtered && f.getName().endsWith(".class")) {
				String name = pkgName + '.' + f.getName().substring(0, f.getName().length() - 6);
				if (classFilter == null || classFilter.test(name)) {
					try {
						Class<?> cl = classLoader.loadClass(name);
						classScanner.accept(cl);
					} catch (Exception t) {
						// ignore
					}
				}
			}
		}
	}
	
	/** Scan a JAR looking for class files. */
	public static void scanJarLibrary(
		ApplicationClassLoader classLoader, File file, String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		try (ZipFile jar = new ZipFile(file)) {
			scanJarLibrary(classLoader, jar, rootPackage, includeSubPackages, packageFilter, classFilter, classScanner);
		} catch (Exception t) {
			// ignore
		}
	}
	
	/** Scan a JAR looking for class files. */
	public static void scanJarLibrary(
		ApplicationClassLoader classLoader, ZipFile jar, String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner
	) {
		String pkgPath = rootPackage.length() > 0 ? rootPackage.replace('.', '/') + '/' : "";
		Enumeration<? extends ZipEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			ZipEntry f = entries.nextElement();
			if (f.isDirectory()) continue;
			
			String name = f.getName();
			if (!name.startsWith(pkgPath) ||
				!name.endsWith(".class"))
				continue;
			name = name.substring(0, name.length() - 6);
			name = name.replace('/', '.');
			int i = name.lastIndexOf('.');
			String pkg = i > 0 ? name.substring(0, i) : "";
			if (includeSubPackages || pkg.equals(rootPackage)) {
				if (packageFilter != null && !packageFilter.test(pkg)) continue;
				if (classFilter != null && !classFilter.test(name)) continue;
				try {
					Class<?> cl = classLoader.loadClass(name);
					classScanner.accept(cl);
				} catch (Exception t) {
					// ignore
				}
			}
		}
	}
}

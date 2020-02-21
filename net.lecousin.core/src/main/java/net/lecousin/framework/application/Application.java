package net.lecousin.framework.application;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.application.libraries.classpath.DefaultLibrariesManager;
import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskMonitoring;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.tasks.LoadPropertiesFileTask;
import net.lecousin.framework.concurrent.tasks.SavePropertiesFileTask;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.locale.LocalizedProperties;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.log.appenders.Appender;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.ObjectUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.SystemEnvironment;

/**
 * Class holding application information such as properties, logging...
 */
public final class Application {
	
	public static final String PROPERTY_LOGGING_CONFIGURATION_URL = "net.lecousin.logging.configuration.url";
	public static final String PROPERTY_INSTALLATION_DIRECTORY = "net.lecousin.application.install.directory";
	public static final String PROPERTY_CONFIG_DIRECTORY = "net.lecousin.application.config.directory";
	public static final String PROPERTY_LOG_DIRECTORY = "net.lecousin.application.log.directory";
	public static final String PROPERTY_TMP_DIRECTORY = "net.lecousin.application.tmpdir";
	
	public static final String PROPERTY_LANGUAGE_TAG = "net.lecousin.framework.locale.language";
	
	private Application(
		Artifact artifact,
		String[] commandLineArguments,
		Map<String,String> properties,
		boolean debugMode,
		ThreadFactory threadFactory,
		LibrariesManager librariesManager
	) {
		this.startTime = System.nanoTime();
		this.artifact = artifact;
		this.commandLineArguments = commandLineArguments;
		if (properties != null)
			this.properties = new Hashtable<>(properties);
		else
			this.properties = new Hashtable<>();
		this.properties.put("groupId", artifact.getGroupId());
		this.properties.put("artifactId", artifact.getArtifactId());
		this.properties.put("version", artifact.getVersion().toString());
		this.debugMode = debugMode;
		this.threadFactory = threadFactory;
		this.librariesManager = librariesManager;
		console = new Console(this);
		if (debugMode) TaskMonitoring.checkLocksOfBlockingTasks = true;
	}
	
	private long startTime;
	private Artifact artifact;
	private String[] commandLineArguments;
	private Hashtable<String,String> properties;
	private boolean debugMode;
	private ThreadFactory threadFactory;
	private LibrariesManager librariesManager;
	private ApplicationClassLoader appClassLoader;
	private Console console;
	private Properties preferences = null;
	private Locale locale;
	private LocalizedProperties localizedProperties;
	private String[] languageTag;
	private LoggerFactory loggerFactory;
	private Map<Class<?>, Object> instances = new HashMap<>();
	private Map<String, Object> data = new HashMap<>();
	private LinkedList<Pair<Integer, Object>> toClose = new LinkedList<>();
	private ArrayList<Thread> toInterrupt = new ArrayList<>();
	private boolean stopping = false;
	
	public long getStartTime() {
		return startTime;
	}
	
	public String getGroupId() {
		return artifact.getGroupId();
	}
	
	public String getArtifactId() {
		return artifact.getArtifactId();
	}
	
	public Version getVersion() {
		return artifact.getVersion();
	}
	
	public String getFullName() {
		return artifact.toString();
	}
	
	public List<String> getCommandLineArguments() {
		return Arrays.asList(commandLineArguments);
	}
	
	public boolean isDebugMode() {
		return debugMode;
	}
	
	public boolean isReleaseMode() {
		return !debugMode;
	}
	
	/** Return the property value or null.
	 * If it is not specified in application specific properties, it will return the system property if it exists.
	 */
	public String getProperty(String name) {
		if (properties.containsKey(name))
			return properties.get(name);
		return System.getProperty(name);
	}
	
	/** Set a property for this application. */
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
	
	public Map<String,String> getApplicationSpecificProperties() {
		return properties;
	}
	
	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}
	
	public Console getConsole() {
		return console;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	/** Set the new application language / locale. */
	public void setLocale(Locale l) {
		locale = l;
		String lt = l.toLanguageTag();
		languageTag = lt.split("-");
		setPreference(PROPERTY_LANGUAGE_TAG, lt);
	}
	
	public LocalizedProperties getLocalizedProperties() {
		return localizedProperties;
	}
	
	public String[] getLanguageTag() {
		return languageTag;
	}
	
	public LoggerFactory getLoggerFactory() {
		return loggerFactory;
	}
	
	public Logger getDefaultLogger() {
		return loggerFactory.getRoot();
	}
	
	public LibrariesManager getLibrariesManager() {
		return librariesManager;
	}
	
	/** Return the application class loader. */
	public ApplicationClassLoader getClassLoader() {
		return appClassLoader;
	}
	
	/** Get a resource from the class loader as an IO.Readable. */
	public IO.Readable getResource(String filename, byte priority) {
		IOProvider.Readable provider = appClassLoader.getIOProvider(filename);
		if (provider == null)
			return null;
		try {
			return provider.provideIOReadable(priority);
		} catch (IOException e) {
			return null;
		}
	}
	
	/** Register an instance to close on application shutdown. */
	public void toClose(int priority, Closeable c) {
		toClose(priority, (Object)c);
	}
	
	/** Register an instance to close on application shutdown. */
	public void toClose(int priority, AsyncCloseable<?> c) {
		toClose(priority, (Object)c);
	}
	
	private void toClose(int priority, Object c) {
		synchronized (toClose) { toClose.add(new Pair<>(Integer.valueOf(priority), c)); }
	}
	
	/** For debugging purpose only, return the list of resources to close when the application will shutdown. */
	public List<Object> getResourcesToClose() {
		LinkedList<Object> list = new LinkedList<>();
		synchronized (toClose) {
			for (Pair<Integer, Object> p : toClose)
				list.add(p.getValue2());
		}
		return list;
	}

	/** Unregister an instance to close on application shutdown. */
	public void closed(Closeable c) {
		closed((Object)c);
	}
	
	/** Unregister an instance to close on application shutdown. */
	public void closed(AsyncCloseable<?> c) {
		closed((Object)c);
	}
	
	private void closed(Object c) {
		synchronized (toClose) {
			for (Iterator<Pair<Integer, Object>> it = toClose.descendingIterator(); it.hasNext(); )
				if (it.next().getValue2() == c) {
					it.remove();
					break;
				}
		}
	}
	
	/** Register a thread that must be interrupted on application shutdown. */
	public void toInterruptOnShutdown(Thread t) {
		synchronized (toInterrupt) { toInterrupt.add(t); }
	}

	/** Unregister a thread that must be interrupted on application shutdown. */
	public void interrupted(Thread t) {
		synchronized (toInterrupt) { toInterrupt.remove(t); }
	}
	
	public boolean isStopping() {
		return stopping;
	}
	
	/** Return the singleton stored for this application. */
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> clazz) {
		synchronized (instances) {
			return (T)instances.get(clazz);
		}
	}
	
	/** Set the singleton stored for this application. */
	@SuppressWarnings("unchecked")
	public <T> T setInstance(Class<T> clazz, T instance) {
		synchronized (instances) {
			return (T)instances.put(clazz, instance);
		}
	}
	
	/** Remove the singleton stored for this application. */
	@SuppressWarnings("unchecked")
	public <T> T removeInstance(Class<T> clazz) {
		synchronized (instances) {
			return (T)instances.remove(clazz);
		}
	}
	
	/** Get a data associated with this application. */
	public Object getData(String name) {
		return data.get(name);
	}
	
	/** Set a data associated with this application and return previously associated data with this name. */
	public Object setData(String name, Object value) {
		return data.put(name, value);
	}
	
	/** Remove a data associated with this application. */
	public Object removeData(String name) {
		return data.remove(name);
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	@SuppressWarnings({
		"squid:S3776", // complexity: we do not want to split into sub-methods
		"squid:S5304", // environment variables are only printed
	})
	public static IAsync<ApplicationBootstrapException> start(
		Artifact artifact,
		String[] commandLineArguments,
		Map<String,String> properties,
		boolean debugMode,
		ThreadFactory threadFactory,
		LibrariesManager librariesManager,
		Appender defaultLogAppender
	) {
		Application app = new Application(artifact, commandLineArguments, properties, debugMode, threadFactory, librariesManager);
		
		// check properties
		String dir = app.getProperty(PROPERTY_CONFIG_DIRECTORY);
		if (dir == null)
			app.setProperty(PROPERTY_CONFIG_DIRECTORY,
				app.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME)
				+ "/.lc.apps/" + app.getGroupId() + "/" + app.getArtifactId() + "/cfg");
		dir = app.getProperty(PROPERTY_LOG_DIRECTORY);
		if (dir == null)
			app.setProperty(PROPERTY_LOG_DIRECTORY,
				app.getProperty(SystemEnvironment.SYSTEM_PROPERTY_USER_HOME)
				+ "/.lc.apps/" + app.getGroupId() + "/" + app.getArtifactId() + "/log");

		if (app.isDebugMode()) {
			Console c = app.getConsole();
			c.out("---- Application " + artifact.toString() + " ----");
			c.out("Application arguments:");
			for (String arg : app.commandLineArguments)
				c.out(" - " + arg);
			c.out("Environment variables:");
			for (Map.Entry<String,String> var : System.getenv().entrySet())
				c.out(" - " + var.getKey() + "=" + var.getValue());
			c.out("JVM Properties:");
			for (Map.Entry<Object,Object> prop : System.getProperties().entrySet())
				c.out(" - " + prop.getKey() + "=" + prop.getValue());
			c.out("Application Properties:");
			for (Map.Entry<String,String> prop : app.getApplicationSpecificProperties().entrySet())
				c.out(" - " + prop.getKey() + "=" + prop.getValue());
			c.out("-----------------------------------------");
		}
		
		LCCore.initEnvironment();
		
		// init logging
		app.loggerFactory = new LoggerFactory(app, defaultLogAppender);

		// init LCCore with this application
		LCCore.start(app);

		JoinPoint<Exception> loading = new JoinPoint<>();
		
		// load preferences
		IAsync<Exception> loadPref = app.loadPreferences();
		loading.addToJoin(loadPref);
		
		// init locale
		Task.Cpu<Void, NoException> loadLocale = new Task.Cpu<Void, NoException>("Initialize localization", Task.PRIORITY_RATHER_IMPORTANT) {
			@Override
			public Void run() {
				String lang = app.getPreference(PROPERTY_LANGUAGE_TAG);
				if (lang == null) lang = app.getProperty(PROPERTY_LANGUAGE_TAG);
				if (lang != null) {
					app.locale = Locale.forLanguageTag(lang);
				} else {
					app.locale = Locale.getDefault();
				}
				app.languageTag = app.locale.toLanguageTag().split("-");
				app.localizedProperties = new LocalizedProperties(app);
				return null;
			}
		};
		loadLocale.startOn(loadPref, true);
		loading.addToJoin(loadLocale.getOutput());
		
		loading.start();
		Async<ApplicationBootstrapException> sp = new Async<>();
		loading.onDone(() -> {
			app.appClassLoader = librariesManager.start(app);
			librariesManager.onLibrariesLoaded().onDone(sp,
				error -> new ApplicationBootstrapException("Error loading libraries", error));
		});
		
		return sp;
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	public static IAsync<ApplicationBootstrapException> start(Artifact artifact, boolean debugMode) {
		return start(artifact, new String[0], debugMode);
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	public static IAsync<ApplicationBootstrapException> start(Artifact artifact, String[] args, boolean debugMode) {
		return start(artifact, args, null, debugMode, Executors.defaultThreadFactory(), new DefaultLibrariesManager(), null);
	}

	/** Stop this application and release resources. */
	@SuppressWarnings({
		"squid:S106", // we use System.out because the application is shutting down and logging system won't be available
		"squid:S2142", // ignore InterruptedException because we are shutting down
		"squid:S3776", // complexity: we do not want to split into sub-methods
	})
	public void stop() {
		System.out.println("Stopping application");
		stopping = true;

		System.out.println(" * Closing resources (" + toClose.size() + ")");
		List<Pair<AsyncCloseable<?>,IAsync<?>>> closing = new LinkedList<>();
		ArrayList<Pair<Integer, Object>> list = new ArrayList<>(toClose);
		list.sort((c1, c2) -> c1.getValue1().intValue() - c2.getValue1().intValue());
		toClose.clear();
		for (Pair<Integer, Object> p : list) {
			try {
				System.out.println("     - " + p.getValue2());
			} catch (Exception e) {
				// ignore
			}
			Object o = p.getValue2();
			if (o instanceof Closeable)
				try { ((Closeable)o).close(); } catch (Exception t) {
					System.err.println("Error closing resource " + o);
					t.printStackTrace(System.err);
				}
			else if (o instanceof AsyncCloseable) {
				closing.add(new Pair<>((AsyncCloseable<?>)o, ((AsyncCloseable<?>)o).closeAsync()));
			}
		}
		
		System.out.println(" * Stopping threads");
		for (Thread t : new ArrayList<>(toInterrupt)) {
			if (!t.isAlive()) continue;
			System.out.println("     - " + t);
			t.interrupt();
		}

		long start = System.currentTimeMillis();
		boolean allClosed = false;
		do {
			for (Iterator<Pair<AsyncCloseable<?>,IAsync<?>>> it = closing.iterator(); it.hasNext(); ) {
				Pair<AsyncCloseable<?>,IAsync<?>> s = it.next();
				if (s.getValue2().isDone()) {
					try {
						System.out.println(" * Closed: " + s.getValue1());
					} catch (Exception e) {
						// ignore
					}
					it.remove();
				}
			}
			if (closing.isEmpty())
				allClosed = true;
			else {
				try {
					Thread.sleep(100);
					if (System.currentTimeMillis() - start > 15000) {
						System.out.println("Ressources are still closing, but we don't wait more than 15 seconds.");
						allClosed = true;
					}
				}
				catch (InterruptedException e) { allClosed = true; }
			}
		} while (!allClosed);
		
		console.close();

		// in a multi-app environment, we may need to close remaining application's threads here
		
		System.out.println("Application stopped.");
	}
	
	
	/** Return a preference. */
	public String getPreference(String name) {
		if (preferences == null)
			loadPreferences().block(0);
		return preferences.getProperty(name);
	}
	
	/** Set a preference. */
	public void setPreference(String name, String value) {
		if (preferences.containsKey(name) && ObjectUtil.equalsOrNull(value, preferences.get(name)))
			return; // no change
		preferences.put(name, value);
		savePreferences();
	}
	
	private IAsync<Exception> loadingPreferences = null;
	
	/** Load preferences. */
	public synchronized IAsync<Exception> loadPreferences() {
		if (loadingPreferences != null) return loadingPreferences;
		File f = new File(getProperty(PROPERTY_CONFIG_DIRECTORY));
		f = new File(f, "preferences");
		if (!f.exists()) {
			getDefaultLogger().info("No preferences file");
			preferences = new Properties();
			loadingPreferences = new Async<>(true);
			return loadingPreferences;
		}
		getDefaultLogger().info("Loading preferences from " + f.getAbsolutePath());
		loadingPreferences = LoadPropertiesFileTask.loadPropertiesFile(
			f, StandardCharsets.UTF_8, Task.PRIORITY_IMPORTANT,
			false,
			props -> preferences = props
		);
		return loadingPreferences;
	}

	IAsync<IOException> savingPreferences = null;
	
	private synchronized void savePreferences() {
		if (savingPreferences != null && !savingPreferences.isDone()) {
			// we need to save again once done
			savingPreferences.onDone(this::savePreferences);
			return;
		}
		File f = new File(getProperty(PROPERTY_CONFIG_DIRECTORY));
		if ((!f.exists() || !f.isDirectory()) && !f.mkdirs())
			loggerFactory.getRoot().warn("Unable to create directory to save preferences: " + f.getAbsolutePath());
		f = new File(f, "preferences");
		getDefaultLogger().info("Saving preferences to " + f.getAbsolutePath());
		savingPreferences = SavePropertiesFileTask.savePropertiesFile(preferences, f, StandardCharsets.UTF_8, Task.PRIORITY_RATHER_LOW);
	}
	
}

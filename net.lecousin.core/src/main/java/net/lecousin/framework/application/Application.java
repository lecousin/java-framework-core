package net.lecousin.framework.application;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.lecousin.framework.application.libraries.DefaultLibrariesManager;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskMonitoring;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.LoadPropertiesFileTask;
import net.lecousin.framework.concurrent.tasks.SavePropertiesFileTask;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.provider.IOProviderFromName;
import net.lecousin.framework.locale.LocalizedProperties;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.ObjectUtil;
import net.lecousin.framework.util.Pair;

/**
 * Class holding application information such as properties, logging...
 */
public final class Application {
	
	public static final String PROPERTY_LOGGING_CONFIGURATION_URL = "net.lecousin.logging.configuration.url";
	public static final String PROPERTY_INSTALLATION_DIRECTORY = "net.lecousin.application.install.directory";
	public static final String PROPERTY_CONFIG_DIRECTORY = "net.lecousin.application.config.directory";
	public static final String PROPERTY_LOG_DIRECTORY = "net.lecousin.application.log.directory";
	
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
	private ApplicationClassLoader rootClassLoader;
	private Console console;
	private Properties preferences = null;
	private Locale locale;
	private LocalizedProperties localizedProperties;
	private String[] languageTag;
	private LoggerFactory loggerFactory;
	private Map<Class<?>, Object> instances = new HashMap<>();
	private ArrayList<Closeable> toCloseSync = new ArrayList<>();
	private ArrayList<AsyncCloseable<?>> toCloseAsync = new ArrayList<>();
	private boolean stopping = false;
	
	public long getStartTime() {
		return startTime;
	}
	
	public String getGroupId() {
		return artifact.groupId;
	}
	
	public String getArtifactId() {
		return artifact.artifactId;
	}
	
	public Version getVersion() {
		return artifact.version;
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
	
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public Map<String,String> getApplicationSpecificProperties() {
		return properties;
	}
	
	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}
	
	public Console getConsole() {
		return console;
	}
	
	@SuppressFBWarnings("UG_SYNC_SET_UNSYNC_GET")
	public Locale getLocale() {
		return locale;
	}
	
	/** Set the new application language / locale. */
	public synchronized void setLocale(Locale l) {
		locale = l;
		String lt = l.toLanguageTag();
		languageTag = lt.split("-");
		setPreference(PROPERTY_LANGUAGE_TAG, lt);
	}
	
	public LocalizedProperties getLocalizedProperties() {
		return localizedProperties;
	}
	
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public String[] getLanguageTag() {
		return languageTag;
	}
	
	public LoggerFactory getLoggerFactory() {
		return loggerFactory;
	}
	
	public Logger getDefaultLogger() {
		return loggerFactory.getDefault();
	}
	
	public LibrariesManager getLibrariesManager() {
		return librariesManager;
	}
	
	public ApplicationClassLoader getClassLoader() {
		return rootClassLoader;
	}
	
	/** Get a resource from the class loader as an IO.Readable. */
	@SuppressWarnings("resource")
	public IO.Readable getResource(String filename, byte priority) {
		if (rootClassLoader instanceof IOProviderFromName.Readable)
			try {
				return ((IOProviderFromName.Readable)rootClassLoader).provideReadableIO(filename, priority);
			} catch (IOException e) {
				return null;
			}
		InputStream in = rootClassLoader.getResourceAsStream(filename);
		if (in == null)
			return null;
		return new IOFromInputStream(in, filename, Threading.getUnmanagedTaskManager(), priority);
	}
	
	/** Register an instance to close on application shutdown. */
	public void toClose(Closeable c) {
		synchronized (toCloseSync) { toCloseSync.add(c); }
	}
	
	/** Register an instance to close on application shutdown. */
	public void toClose(AsyncCloseable<?> c) {
		synchronized (toCloseAsync) { toCloseAsync.add(c); }
	}

	/** Unregister an instance to close on application shutdown. */
	public void closed(Closeable c) {
		synchronized (toCloseSync) { toCloseSync.remove(c); }
	}
	
	/** Unregister an instance to close on application shutdown. */
	public void closed(AsyncCloseable<?> c) {
		synchronized (toCloseAsync) { toCloseAsync.remove(c); }
	}
	
	public boolean isStopping() {
		return stopping;
	}
	
	/** Return the singleton stored for this application. */
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> clazz) {
		return (T)instances.get(clazz);
	}
	
	/** Set the singleton stored for this application. */
	public <T> void setInstance(Class<T> clazz, T instance) {
		instances.put(clazz, instance);
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	public static ISynchronizationPoint<Exception> start(
		Artifact artifact,
		String[] commandLineArguments,
		Map<String,String> properties,
		boolean debugMode,
		ThreadFactory threadFactory,
		LibrariesManager librariesManager
	) {
		Application app = new Application(artifact, commandLineArguments, properties, debugMode, threadFactory, librariesManager);
		
		// check properties
		String dir = app.getProperty(PROPERTY_CONFIG_DIRECTORY);
		if (dir == null)
			app.setProperty(PROPERTY_CONFIG_DIRECTORY,
				app.getProperty("user.home") + "/.lc.apps/" + app.getGroupId() + "/" + app.getArtifactId() + "/cfg");
		dir = app.getProperty(PROPERTY_LOG_DIRECTORY);
		if (dir == null)
			app.setProperty(PROPERTY_LOG_DIRECTORY,
				app.getProperty("user.home") + "/.lc.apps/" + app.getGroupId() + "/" + app.getArtifactId() + "/log");

		if (app.isDebugMode()) {
			@SuppressWarnings("resource")
			Console c = app.getConsole();
			c.out("---- Application " + artifact.toString() + " ----");
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
		
		// init logging
		app.loggerFactory = new LoggerFactory(app);
		
		// init LCCore with this application
		LCCore.start(app);

		JoinPoint<Exception> loading = new JoinPoint<>();
		
		// load preferences
		ISynchronizationPoint<IOException> loadPref = app.loadPreferences();
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
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		loading.listenInline(() -> {
			app.rootClassLoader = librariesManager.start(app);
			librariesManager.onLibrariesLoaded().listenInline(sp);
		});
		
		return sp;
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	public static ISynchronizationPoint<Exception> start(Artifact artifact, boolean debugMode) {
		return start(artifact, new String[0], debugMode);
	}
	
	/** Method to call at the beginning of the application, typically in the main method. */
	public static ISynchronizationPoint<Exception> start(Artifact artifact, String[] args, boolean debugMode) {
		return start(artifact, args, null, debugMode, Executors.defaultThreadFactory(), new DefaultLibrariesManager());
	}

	/** Stop this application and release resources. */
	public void stop() {
		System.out.println("Stopping application");
		stopping = true;

		System.out.println(" * Closing resources");
		for (Closeable c : new ArrayList<>(toCloseSync)) {
			System.out.println("     - " + c);
			try { c.close(); } catch (Throwable t) {
				System.err.println("Error closing resource " + c);
				t.printStackTrace(System.err);
			}
		}

		List<Pair<AsyncCloseable<?>,ISynchronizationPoint<?>>> closing = new LinkedList<>();
		for (AsyncCloseable<?> s : new ArrayList<>(toCloseAsync)) {
			System.out.println(" * Closing " + s);
			closing.add(new Pair<>(s, s.closeAsync()));
		}
		toCloseAsync.clear();
		long start = System.currentTimeMillis();
		do {
			for (Iterator<Pair<AsyncCloseable<?>,ISynchronizationPoint<?>>> it = closing.iterator(); it.hasNext(); ) {
				Pair<AsyncCloseable<?>,ISynchronizationPoint<?>> s = it.next();
				if (s.getValue2().isUnblocked()) {
					System.out.println(" * Closed: " + s.getValue1());
					it.remove();
				}
			}
			if (closing.isEmpty()) break;
			try { Thread.sleep(100); }
			catch (InterruptedException e) { break; }
			if (System.currentTimeMillis() - start > 15000) {
				System.out.println("Ressources are still closing, but we don't wait more than 15 seconds.");
				break;
			}
		} while (true);
		
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
	
	private ISynchronizationPoint<IOException> loadingPreferences = null;
	
	/** Load preferences. */
	public synchronized ISynchronizationPoint<IOException> loadPreferences() {
		if (loadingPreferences != null) return loadingPreferences;
		File f = new File(getProperty(PROPERTY_CONFIG_DIRECTORY));
		f = new File(f, "preferences");
		if (!f.exists()) {
			getDefaultLogger().info("No preferences file");
			preferences = new Properties();
			return loadingPreferences = new SynchronizationPoint<>(true);
		}
		getDefaultLogger().info("Loading preferences from " + f.getAbsolutePath());
		loadingPreferences = LoadPropertiesFileTask.loadPropertiesFile(
			f, StandardCharsets.UTF_8, Task.PRIORITY_IMPORTANT,
			new Listener<Properties>() {
				@Override
				public void fire(Properties props) {
					preferences = props;
				}
			}
		);
		return loadingPreferences;
	}

	ISynchronizationPoint<IOException> savingPreferences = null;
	
	private synchronized void savePreferences() {
		if (savingPreferences != null && !savingPreferences.isUnblocked()) {
			// we need to save again once done
			savingPreferences.listenInline(() -> { savePreferences(); });
			return;
		}
		File f = new File(getProperty(PROPERTY_CONFIG_DIRECTORY));
		if (!f.exists() || !f.isDirectory())
			if (!f.mkdirs())
				loggerFactory.getDefault().warn("Unable to create directory to save preferences: " + f.getAbsolutePath());
		f = new File(f, "preferences");
		getDefaultLogger().info("Saving preferences to " + f.getAbsolutePath());
		savingPreferences = SavePropertiesFileTask.savePropertiesFile(preferences, f, StandardCharsets.UTF_8, Task.PRIORITY_RATHER_LOW);
	}
	
}

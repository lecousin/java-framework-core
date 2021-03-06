package net.lecousin.framework.application.libraries.classloader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.collections.CompoundCollection;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFrom;
import net.lecousin.framework.util.Pair;

/**
 * Abstract class loader.
 */
public abstract class AbstractClassLoader extends ClassLoader implements ApplicationClassLoader, IOProviderFrom.Readable<String> {

	static {
		ClassLoader.registerAsParallelCapable();
	}

	/** Constructor. */
	public AbstractClassLoader(AppClassLoader appClassLoader) {
		this.appClassLoader = appClassLoader;
	}
	
	protected AppClassLoader appClassLoader;
	
	@Override
	public Application getApplication() {
		return appClassLoader.getApplication();
	}
	
	/** Return a description of this class loader. */
	public abstract String getDescription();
	
	/** Load the content of a file. */
	protected abstract byte[] loadFile(String name) throws IOException;
	
	/** Load a resource as IO.Readable. */
	protected abstract IO.Readable loadResourceAsIO(String name, Priority priority) throws IOException;
	
	/** Search a resource. */
	protected abstract URL loadResourceURL(String name);
	
	protected abstract Object getResourcePointer(String path);
	
	protected abstract IO.Readable openResourcePointer(Object pointer, Priority priority) throws IOException;
	
	protected abstract void scan(String rootPackage, boolean includeSubPackages,
		Predicate<String> packageFilter, Predicate<String> classFilter, Consumer<Class<?>> classScanner);
	
	private List<AbstractClassLoader> subLoaders = null;
	
	/** Add a class loader from a resource contained by this class loader, for example an inner jar file. */
	public final void addSubLoader(AbstractClassLoader loader) {
		if (subLoaders == null) subLoaders = new ArrayList<>();
		subLoaders.add(loader);
	}
	
	private static HashMap<String,Pair<Thread,JoinPoint<NoException>>> classLoadingSP = new HashMap<>();
	
	/** Get the synchronized object for loading the given class. */
	public static IAsync<NoException> getClassLoadingSP(String name) {
		synchronized (classLoadingSP) {
			Pair<Thread,JoinPoint<NoException>> p = classLoadingSP.get(name);
			if (p == null) {
				JoinPoint<NoException> jp = new JoinPoint<>();
				jp.addToJoin(1);
				jp.start();
				classLoadingSP.put(name, new Pair<Thread,JoinPoint<NoException>>(Thread.currentThread(), jp));
				return null;
			}
			if (p.getValue1() == Thread.currentThread()) {
				p.getValue2().addToJoin(1);
				return null;
			}
			return p.getValue2();
		}
	}
	
	/** Release the synchronized object for loading the given class. */
	public static void releaseClassLoadingSP(String name) {
		synchronized (classLoadingSP) {
			Pair<Thread,JoinPoint<NoException>> sp = classLoadingSP.get(name);
			JoinPoint<NoException> jp = sp.getValue2();
			jp.joined();
			if (jp.isDone())
				classLoadingSP.remove(name);
		}
	}
	
	@Override
	protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		IAsync<NoException> sp = getClassLoadingSP(name);
		while (sp != null) {
			//sp.block(0); it was good because we can launch new tasks, but it may cause blocking everything
			sp.blockPause(15000);
			sp = getClassLoadingSP(name);
		}
        try {
	        // First, check if the class has already been loaded
	        Class<?> c = isLoaded(name);
	        if (c == null)
		        c = appClassLoader.loadClassFrom(name, this);
	        if (resolve)
	        	resolveClass(c);
	        return c;
		} finally {
			releaseClassLoadingSP(name);
		}
	}
	
	final Class<?> isLoaded(String name) {
		Class<?> c = findLoadedClass(name);
		if (c != null) return c;
		if (subLoaders == null) return null;
		for (AbstractClassLoader sub : subLoaders) {
			c = sub.isLoaded(name);
			if (c != null) return c;
		}
		return null;
	}
	
	final Class<?> loadClassInLibrary(String name) throws IOException {
		IAsync<NoException> sp = getClassLoadingSP(name);
		while (sp != null) {
			//sp.block(0);
			sp.blockPause(15000);
			sp = getClassLoadingSP(name);
		}
        try {
			Class<?> c = findLoadedClass(name);
			if (c != null) return c;
			try {
				byte[] file = loadFile(name.replace('.', '/') + ".class");
				return defineClass(name, file, 0, file.length);
			} catch (FileNotFoundException e) {
				if (subLoaders == null) throw e;
				for (AbstractClassLoader sub : subLoaders) {
					try {
						return sub.loadClassInLibrary(name);
					} catch (FileNotFoundException e2) {
						// not found
					}
				}
				throw e;
			}
		} finally {
			releaseClassLoadingSP(name);
		}
	}
	
	private Pair<Object, AbstractClassLoader> getPointer(String path) {
		Object pointer = getResourcePointer(path);
		if (pointer != null)
			return new Pair<>(pointer, this);
		if (subLoaders != null)
			for (AbstractClassLoader sub : subLoaders) {
				pointer = sub.getResourcePointer(path);
				if (pointer != null)
					return new Pair<>(pointer, sub);
			}
		return null;
	}
	
	@Override
	public final IOProvider.Readable get(String path) {
		Pair<Object, AbstractClassLoader> pointer = getPointer(path);
		if (pointer == null) return null;
		return new IOProvider.Readable() {
			@Override
			public IO.Readable provideIOReadable(Priority priority) throws IOException {
				return pointer.getValue2().openResourcePointer(pointer.getValue1(), priority);
			}

			@Override
			public String getDescription() {
				return path;
			}
		};
	}
	
	/** Open a resource. */
	public final IO.Readable open(String path, Priority priority) throws IOException {
		Pair<Object, AbstractClassLoader> pointer = getPointer(path);
		if (pointer == null) throw new FileNotFoundException(path);
		return pointer.getValue2().openResourcePointer(pointer.getValue1(), priority);
	}

	/** Search for a resource. */
	public final URL getResourceURL(String name) {
		URL url = loadResourceURL(name);
		if (url == null && subLoaders != null)
			for (AbstractClassLoader sub : subLoaders) {
				url = sub.getResourceURL(name);
				if (url != null) break;
			}
		return url;
	}
	
	/** Search for all resources with the same path. */
	public final Iterable<URL> getResourcesURL(String name) {
		URL url = loadResourceURL(name);
		if (subLoaders == null)
			return url != null ? Collections.singletonList(url) : null;
		CompoundCollection<URL> list = new CompoundCollection<>();
		if (url != null) list.addSingleton(url);
		for (AbstractClassLoader sub : subLoaders) {
			Iterable<URL> urls = sub.getResourcesURL(name);
			if (urls != null) list.add(urls);
		}
		return list;
	}
	
	@Override
	public URL getResource(String name) {
		return appClassLoader.getResourceFrom(name, this);
	}
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return appClassLoader.getResources(name);
	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		return appClassLoader.getResourceAsStreamFrom(name, this);
	}
	
	@Override
	public IOProvider.Readable getIOProvider(String filename) {
		return appClassLoader.getIOProviderFrom(filename, this);
	}
	
}

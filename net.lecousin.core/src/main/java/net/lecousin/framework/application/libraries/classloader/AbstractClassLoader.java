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

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.ApplicationClassLoader;
import net.lecousin.framework.collections.CompoundCollection;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.provider.IOProviderFromName;
import net.lecousin.framework.util.Pair;

public abstract class AbstractClassLoader extends ClassLoader implements ApplicationClassLoader, IOProviderFromName.Readable {

	static {
		ClassLoader.registerAsParallelCapable();
	}

	public AbstractClassLoader(AppClassLoader appClassLoader) {
		this.appClassLoader = appClassLoader;
	}
	
	protected AppClassLoader appClassLoader;
	
	@Override
	public Application getApplication() {
		return appClassLoader.getApplication();
	}
	
	public abstract String getDescription();
	
	protected abstract byte[] loadFile(String name) throws IOException;
	protected abstract IO.Readable _getResourceAsIO(String name, byte priority) throws IOException;
	protected abstract URL _getResourceURL(String name);

	private List<AbstractClassLoader> subLoaders = null;
	public final void addSubLoader(AbstractClassLoader loader) {
		if (subLoaders == null) subLoaders = new ArrayList<>();
		subLoaders.add(loader);
	}
	
	private static HashMap<String,Pair<Thread,JoinPoint<NoException>>> classLoadingSP = new HashMap<>();
	public static ISynchronizationPoint<NoException> getClassLoadingSP(String name) {
		synchronized (classLoadingSP) {
			Pair<Thread,JoinPoint<NoException>> p = classLoadingSP.get(name);
			if (p == null) {
				JoinPoint<NoException> jp = new JoinPoint<NoException>();
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
	public static void releaseClassLoadingSP(String name) {
		synchronized (classLoadingSP) {
			Pair<Thread,JoinPoint<NoException>> sp = classLoadingSP.get(name);
			JoinPoint<NoException> jp = sp.getValue2();
			jp.joined();
			if (jp.isUnblocked())
				classLoadingSP.remove(name);
		}
	}
	
	@Override
	protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		ISynchronizationPoint<NoException> sp = getClassLoadingSP(name);
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
		ISynchronizationPoint<NoException> sp = getClassLoadingSP(name);
		while (sp != null) {
			//sp.block(0);
			sp.blockPause(15000);
			sp = getClassLoadingSP(name);
		}
        try {
			Class<?> c = findLoadedClass(name);
			if (c != null) return c;
			try {
				byte[] file = loadFile(name.replace('.', '/')+".class");
				return defineClass(name, file, 0, file.length);
			} catch (FileNotFoundException e) {
				if (subLoaders == null) throw e;
				for (AbstractClassLoader sub : subLoaders) {
					try {
						return sub.loadClassInLibrary(name);
					} catch (FileNotFoundException e2) {}
				}
				throw e;
			}
		} finally {
			releaseClassLoadingSP(name);
		}
	}
	
	@Override
	public final IO.Readable provideReadableIO(String name, byte priority) throws IOException {
		try { return _getResourceAsIO(name, priority); }
		catch (FileNotFoundException e) {
			if (subLoaders == null) throw e;
			for (AbstractClassLoader sub : subLoaders) {
				try {
					return sub.provideReadableIO(name, priority);
				} catch (FileNotFoundException e2) {}
			}
			throw e;
		}
	}

	public final URL getResourceURL(String name) {
		URL url = _getResourceURL(name);
		if (url == null && subLoaders != null)
			for (AbstractClassLoader sub : subLoaders) {
				url = sub.getResourceURL(name);
				if (url != null) break;
			}
		return url;
	}
	
	public final Iterable<URL> getResourcesURL(String name) {
		URL url = _getResourceURL(name);
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
	
}

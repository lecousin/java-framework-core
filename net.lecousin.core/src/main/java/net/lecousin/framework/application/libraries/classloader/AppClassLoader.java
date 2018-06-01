package net.lecousin.framework.application.libraries.classloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.CompoundCollection;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.provider.FileIOProvider;
import net.lecousin.framework.io.provider.IOProvider;

/**
 * Used to aggregate class loaders for an application.
 */
public class AppClassLoader {
	
	/** Constructor. */
	public AppClassLoader(Application app) {
		this.app = app;
	}
	
	private Application app;
	private ArrayList<AbstractClassLoader> libs = new ArrayList<>();
	
	public Application getApplication() {
		return app;
	}
	
	/** Add a library. */
	public AbstractClassLoader add(File location, Collection<String> exportedJars) {
		AbstractClassLoader cl;
		if (location.isDirectory())
			cl = new DirectoryClassLoader(this, location);
		else
			cl = new ZipClassLoader(this, new FileIOProvider(location));
		if (exportedJars != null)
			for (String jar : exportedJars)
				cl.addSubLoader(new ZipClassLoader(this, new InnerJARProvider(cl, jar)));
		synchronized (libs) {
			libs.add(cl);
		}
		return cl;
	}
	
	private static class InnerJARProvider implements IOProvider.Readable {
		public InnerJARProvider(AbstractClassLoader parent, String jar) {
			this.parent = parent;
			this.jar = jar;
		}
		
		private AbstractClassLoader parent;
		private String jar;
		
		@Override
		public String getDescription() {
			return parent.getDescription() + "/" + jar;
		}
		
		@Override
		public IO.Readable provideIOReadable(byte priority) throws IOException {
			return parent.loadResourceAsIO(jar, priority);
		}
	}
	
	/** Load a class, starting to search in a specific library (typically the one trying to load the class). */
	public Class<?> loadClassFrom(String name, AbstractClassLoader first) throws ClassNotFoundException {
		// try on the system one
		try { return AppClassLoader.class.getClassLoader().loadClass(name); }
		catch (ClassNotFoundException e) { /* not found. */ }
		Class<?> c = null;
		// check it is not loaded on one of the library
		for (int i = 0; i < libs.size(); i++) {
    		AbstractClassLoader cl = libs.get(i);
    		c = cl.isLoaded(name);
    		if (c != null) return c;
    	}
    	// try with the first one
    	if (first != null) {
    		try {
    			c = first.loadClassInLibrary(name);
    			if (c != null) return c;
    		} catch (FileNotFoundException e) {
    			// not found
    		} catch (IOException e) {
    			throw new ClassNotFoundException(e.getMessage());
    		}
    	}
    	// then, try on other libraries
    	for (int i = 0; i < libs.size(); i++) {
    		AbstractClassLoader cl = libs.get(i);
    		if (cl == first) continue;
    		try {
    			c = cl.loadClassInLibrary(name);
    			if (c != null) return c;
    		} catch (FileNotFoundException e) {
    			// not found
    		} catch (IOException e) {
    			throw new ClassNotFoundException(e.getMessage());
    		}
    	}
		StringBuilder msg = new StringBuilder(512);
		msg.append("Class not found: ").append(name);
		msg.append("\r\nFrom class loader: ").append(first);
		msg.append("\r\nlibraries currently registered:");
		for (int i = 0; i < libs.size(); i++)
			msg.append("\r\n - ").append(libs.get(i).toString());
		throw new ClassNotFoundException(msg.toString());
	}

	/** Load a resource. */
	@SuppressWarnings("resource")
	public IO.Readable getResourceIO(String name, byte priority) {
		if (name.length() == 0) return null;
		if (name.charAt(0) == '/') name = name.substring(1);
		IO.Readable io = null;
    	for (int i = 0; i < libs.size(); i++) {
    		AbstractClassLoader cl = libs.get(i);
   			try { io = cl.get(name).provideIOReadable(priority); }
   			catch (IOException e) { /* ignore */ }
    		if (io != null) return io;
    	}
       	// then try on the core one
        InputStream in = AppClassLoader.class.getClassLoader().getResourceAsStream(name);
        if (in == null) return null;
        return new IOFromInputStream(in, name, Threading.getUnmanagedTaskManager(), priority);
	}
	
	/** Search a resource. */
	public URL getResourceURL(String name) {
		if (name.length() == 0) return null;
		if (name.charAt(0) == '/') name = name.substring(1);
    	for (int i = 0; i < libs.size(); i++) {
    		AbstractClassLoader cl = libs.get(i);
    		URL url = cl.loadResourceURL(name);
    		if (url != null) return url;
    	}
    	// then try on the core one
    	return AppClassLoader.class.getClassLoader().getResource(name);
	}
	
	/** Load a resource, looking first into the given library. */
	@SuppressWarnings("resource")
	public InputStream getResourceAsStreamFrom(String name, AbstractClassLoader first) {
		if (name.length() == 0) return null;
		if (name.charAt(0) == '/') name = name.substring(1);
		IO.Readable io = null;
    	// try with the first one
    	if (first != null)
   			try { io = first.get(name).provideIOReadable(Task.PRIORITY_RATHER_IMPORTANT); }
    		catch (IOException e) { /* not there */ }
    	// then, try on other libraries
    	if (io == null) {
        	for (int i = 0; i < libs.size(); i++) {
        		AbstractClassLoader cl = libs.get(i);
        		if (cl == first) continue;
       			try { io = cl.get(name).provideIOReadable(Task.PRIORITY_RATHER_IMPORTANT); }
       			catch (IOException e) { /* not there */ }
        		if (io != null) break;
        	}
        }
        if (io != null)
        	return IOAsInputStream.get(io, true);
       	// then try on the core one
        return AppClassLoader.class.getClassLoader().getResourceAsStream(name);
	}
	
	/** Search a resource, looking first into the given library. */
	public URL getResourceFrom(String name, AbstractClassLoader first) {
		if (name.length() == 0) return null;
		if (name.charAt(0) == '/') name = name.substring(1);
		URL url = null;
    	// try with the first one
    	if (first != null)
   			url = first.getResourceURL(name);
    	// then, try on other libraries
    	if (url == null) {
        	for (int i = 0; i < libs.size(); i++) {
        		AbstractClassLoader cl = libs.get(i);
        		if (cl == first) continue;
       			url = cl.getResourceURL(name);
        		if (url != null) break;
        	}
        }
        if (url == null)
        	url = AppClassLoader.class.getClassLoader().getResource(name);
        return url;
	}

	/** Search for resources. */
	public Enumeration<URL> getResources(String name) throws IOException {
		if (name.length() == 0) return null;
		if (name.charAt(0) == '/') name = name.substring(1);
		CompoundCollection<URL> list = new CompoundCollection<>();
    	for (int i = 0; i < libs.size(); i++) {
    		AbstractClassLoader cl = libs.get(i);
    		Iterable<URL> urls = cl.getResourcesURL(name);
   			if (urls != null) list.add(urls);
    	}
    	list.add(AppClassLoader.class.getClassLoader().getResources(name));
        return list.enumeration();
	}
	
}

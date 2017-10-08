package net.lecousin.framework.protocols;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL Stream Handler looking for resources in the classpath, using the getResource method on the class loader.
 */
public class ClasspathURLStreamHandler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		URL url = ClasspathURLStreamHandler.class.getClassLoader().getResource(u.getPath());
		if (url == null) throw new IOException("Resource not found in classpath: " + u.getPath());
		return url.openConnection();
	}
	
}

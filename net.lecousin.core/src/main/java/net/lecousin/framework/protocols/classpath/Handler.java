package net.lecousin.framework.protocols.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.util.ReadableAsURLConnection;

/**
 * URL Stream Handler looking for resources in the classpath,
 * using first the method {@link Application#getResource(String, net.lecousin.framework.concurrent.threads.Task.Priority)},
 * then the getResource method on the class loader.
 */
public class Handler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		IO.Readable io = LCCore.isStarted() ? LCCore.getApplication().getResource(u.getPath(), Task.Priority.NORMAL) : null;
		if (io != null) return new ReadableAsURLConnection(io, u, true);
		URL url = Handler.class.getClassLoader().getResource(u.getPath());
		if (url == null) throw new IOException("Resource not found in classpath: " + u.getPath());
		return url.openConnection();
	}
	
}

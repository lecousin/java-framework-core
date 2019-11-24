package net.lecousin.framework.protocols.string;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.util.ReadableAsURLConnection;

/**
 * URL Stream Handler for string protocol.
 */
public class Handler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		String encoding = u.getHost();
		String str = u.getPath().substring(1);
		byte[] bytes = str.getBytes(encoding);
		ByteArrayIO io = new ByteArrayIO(bytes, "string protocol");
		return new ReadableAsURLConnection(io, u, true);
	}
	
}

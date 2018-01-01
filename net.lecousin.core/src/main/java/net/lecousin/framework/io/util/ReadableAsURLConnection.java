package net.lecousin.framework.io.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;

public class ReadableAsURLConnection extends URLConnection {

	public ReadableAsURLConnection(IO.Readable io, URL url) {
		super(url);
		this.io = io;
	}
	
	private IO.Readable io;
	
	@Override
	public void connect() {
	}
	
	@Override
	public long getContentLengthLong() {
		if (!(io instanceof IO.KnownSize)) return -1;
		try { return ((IO.KnownSize)io).getSizeSync(); }
		catch (Throwable t) { return -1; }
	}
	
	@Override
	public InputStream getInputStream() {
		return IOAsInputStream.get(io);
	}
	
}

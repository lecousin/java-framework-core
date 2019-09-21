package net.lecousin.framework.io.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOAsInputStream;

/** Implementation of URLConnection using a IO.Readable. */
public class ReadableAsURLConnection extends URLConnection {

	/** Constructor. */
	public ReadableAsURLConnection(IO.Readable io, URL url, boolean closeAsync) {
		super(url);
		this.io = io;
		this.closeAsync = closeAsync;
	}
	
	private IO.Readable io;
	private boolean closeAsync;
	
	@Override
	public void connect() {
		// nothing to do
	}
	
	@Override
	public long getContentLengthLong() {
		if (!(io instanceof IO.KnownSize)) return -1;
		try { return ((IO.KnownSize)io).getSizeSync(); }
		catch (Exception t) { return -1; }
	}
	
	@Override
	public InputStream getInputStream() {
		return IOAsInputStream.get(io, closeAsync);
	}
	
}

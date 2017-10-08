package net.lecousin.framework.concurrent.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

/**
 * Task saving properties to a file.
 */
public class SavePropertiesFileTask extends Task.Cpu<Void,IOException> {
	
	/** Save properties to a file. */
	@SuppressWarnings("resource")
	public static ISynchronizationPoint<IOException> savePropertiesFile(Properties properties, File file, Charset charset, byte priority) {
		FileIO.ReadWrite io = new FileIO.ReadWrite(file, priority);
		AsyncWork<Void,IOException> resize = io.setSizeAsync(0);
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		resize.listenInline(new Runnable() {
			@Override
			public void run() {
				if (resize.hasError()) {
					result.error(resize.getError());
					io.closeAsync();
					return;
				}
				savePropertiesFile(properties, io, charset, priority, true).listenInline(result);
			}
		});
		return result;
	}
	
	/** Save properties to a Writable IO. */
	@SuppressWarnings("resource")
	public static ISynchronizationPoint<IOException> savePropertiesFile(
		Properties properties, IO.Writable output, Charset charset, byte priority, boolean closeIOAtEnd
	) {
		BufferedWritableCharacterStream stream = new BufferedWritableCharacterStream(output, charset, 4096);
		return savePropertiesFile(properties, stream, priority, closeIOAtEnd);
	}
	
	/** Save properties to a writable character stream. */
	public static ISynchronizationPoint<IOException> savePropertiesFile(
		Properties properties, ICharacterStream.Writable.Buffered output, byte priority, boolean closeStreamAtEnd
	) {
		SavePropertiesFileTask task = new SavePropertiesFileTask(properties, output, priority, closeStreamAtEnd);
		task.start();
		return task.getSynch();
	}

	/** Constructor. */
	public SavePropertiesFileTask(Properties properties, ICharacterStream.Writable.Buffered output, byte priority, boolean closeStreamAtEnd) {
		super("Save properties file", priority);
		this.properties = properties;
		this.output = output;
		this.closeStreamAtEnd = closeStreamAtEnd;
	}
	
	private Properties properties;
	private ICharacterStream.Writable.Buffered output;
	private boolean closeStreamAtEnd;
	
	private final char[] equal = { '=' };
	private final char[] newLine = { '\n' };
	
	@Override
	public Void run() throws IOException {
		try {
			for (Map.Entry<Object,Object> p : properties.entrySet()) {
				output.write(p.getKey().toString());
				output.write(equal);
				output.write(p.getValue().toString());
				output.write(newLine);
			}
			return null;
		} finally {
			if (closeStreamAtEnd) try { output.close(); } catch (Throwable t) { /* ignore */ }
		}
	}
	
}

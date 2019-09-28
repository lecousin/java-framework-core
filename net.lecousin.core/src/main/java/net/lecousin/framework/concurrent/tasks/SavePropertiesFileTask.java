package net.lecousin.framework.concurrent.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

/**
 * Task saving properties to a file.
 */
public class SavePropertiesFileTask extends Task.Cpu<Void,IOException> {
	
	/** Save properties to a file. */
	@SuppressWarnings("squid:S2095") // io is closed
	public static IAsync<IOException> savePropertiesFile(Properties properties, File file, Charset charset, byte priority) {
		FileIO.ReadWrite io = new FileIO.ReadWrite(file, priority);
		AsyncSupplier<Void,IOException> resize = io.setSizeAsync(0);
		Async<IOException> result = new Async<>();
		resize.onDone(() -> {
			if (resize.hasError()) {
				result.error(resize.getError());
				io.closeAsync();
				return;
			}
			savePropertiesFile(properties, io, charset, priority, true).onDone(result);
		});
		return result;
	}
	
	/** Save properties to a Writable IO. */
	public static IAsync<IOException> savePropertiesFile(
		Properties properties, IO.Writable output, Charset charset, byte priority, boolean closeIOAtEnd
	) {
		BufferedWritableCharacterStream stream = new BufferedWritableCharacterStream(output, charset, 4096);
		return savePropertiesFile(properties, stream, priority, closeIOAtEnd);
	}
	
	/** Save properties to a writable character stream. */
	public static IAsync<IOException> savePropertiesFile(
		Properties properties, ICharacterStream.Writable.Buffered output, byte priority, boolean closeStreamAtEnd
	) {
		SavePropertiesFileTask task = new SavePropertiesFileTask(properties, output, priority, closeStreamAtEnd);
		task.start();
		return task.getOutput();
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
	
	@Override
	public Void run() throws IOException, CancelException {
		try {
			for (Map.Entry<Object,Object> p : properties.entrySet()) {
				output.writeSync(p.getKey().toString());
				output.writeSync('=');
				output.writeSync(p.getValue().toString());
				output.writeSync('\n');
			}
			output.flush().blockThrow(0);
			return null;
		} finally {
			if (closeStreamAtEnd) try { output.close(); } catch (Exception t) { /* ignore */ }
		}
	}
	
}

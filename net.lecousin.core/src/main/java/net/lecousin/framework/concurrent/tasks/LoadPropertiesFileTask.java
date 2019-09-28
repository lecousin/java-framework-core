package net.lecousin.framework.concurrent.tasks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.PropertiesReader;
import net.lecousin.framework.util.UnprotectedStringBuffer;

/**
 * Task to read a file of properties.
 */
public class LoadPropertiesFileTask extends PropertiesReader<Properties> {

	/** Load properties from a file. */
	public static AsyncSupplier<Properties, Exception> loadPropertiesFile(
		File file, Charset charset, byte priority, boolean closeSynchronous, Consumer<Properties> onDone
	) {
		FileIO.ReadOnly input = new FileIO.ReadOnly(file, priority);
		return loadPropertiesFile(
			input, charset, priority,
			closeSynchronous ? IO.OperationType.SYNCHRONOUS : IO.OperationType.ASYNCHRONOUS,
			onDone);
	}

	/** Load properties from a Readable IO. */
	public static AsyncSupplier<Properties, Exception> loadPropertiesFile(
		IO.Readable input, Charset charset, byte priority, IO.OperationType closeInputAtEnd, Consumer<Properties> onDone
	) {
		if (!(input instanceof IO.Readable.Buffered))
			input = new PreBufferedReadable(input, 512, priority, 1024, priority, 16);
		return loadPropertiesFile((IO.Readable.Buffered)input, charset, priority, closeInputAtEnd, onDone);
	}

	/** Load properties from a Buffered IO. */
	public static AsyncSupplier<Properties, Exception> loadPropertiesFile(
		IO.Readable.Buffered input, Charset charset, byte priority, IO.OperationType closeInputAtEnd, Consumer<Properties> onDone
	) {
		BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(input, charset, 512, 32);
		return loadPropertiesFile(cs, priority, closeInputAtEnd, onDone);
	}

	/** Load properties from a character stream. */
	public static AsyncSupplier<Properties, Exception> loadPropertiesFile(
		BufferedReadableCharacterStream stream, byte priority, IO.OperationType closeStreamAtEnd, Consumer<Properties> onDone
	) {
		LoadPropertiesFileTask task = new LoadPropertiesFileTask(stream, priority, closeStreamAtEnd, onDone);
		return task.start();
	}

	/** Constructor. */
	public LoadPropertiesFileTask(
		BufferedReadableCharacterStream stream, byte priority, IO.OperationType closeStreamAtEnd, Consumer<Properties> onDone
	) {
		super("Load properties file", stream, priority, closeStreamAtEnd);
		this.onDone = onDone;
	}
	
	private Properties properties = new Properties();
	private Consumer<Properties> onDone;
	
	@Override
	protected void processProperty(UnprotectedStringBuffer key, UnprotectedStringBuffer value) {
		properties.setProperty(key.asString(), value.asString());
	}
	
	@Override
	protected Properties generateResult() {
		if (onDone != null) onDone.accept(properties);
		return properties;
	}
	
}

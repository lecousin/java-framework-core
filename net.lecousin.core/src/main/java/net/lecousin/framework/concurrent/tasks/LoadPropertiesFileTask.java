package net.lecousin.framework.concurrent.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.event.Listener;
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
	@SuppressWarnings("resource")
	public static AsyncWork<Properties, IOException> loadPropertiesFile(File file, Charset charset, byte priority, Listener<Properties> onDone) {
		FileIO.ReadOnly input = new FileIO.ReadOnly(file, priority);
		return loadPropertiesFile(input, charset, priority, IO.OperationType.SYNCHRONOUS, onDone);
	}

	/** Load properties from a Readable IO. */
	public static AsyncWork<Properties, IOException> loadPropertiesFile(
		IO.Readable input, Charset charset, byte priority, IO.OperationType closeInputAtEnd, Listener<Properties> onDone
	) {
		if (!(input instanceof IO.Readable.Buffered))
			input = new PreBufferedReadable(input, 512, priority, 1024, priority, 16);
		return loadPropertiesFile((IO.Readable.Buffered)input, charset, priority, closeInputAtEnd, onDone);
	}

	/** Load properties from a Buffered IO. */
	@SuppressWarnings("resource")
	public static AsyncWork<Properties, IOException> loadPropertiesFile(
		IO.Readable.Buffered input, Charset charset, byte priority, IO.OperationType closeInputAtEnd, Listener<Properties> onDone
	) {
		BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(input, charset, 512, 32);
		return loadPropertiesFile(cs, priority, closeInputAtEnd, onDone);
	}

	/** Load properties from a character stream. */
	public static AsyncWork<Properties, IOException> loadPropertiesFile(
		BufferedReadableCharacterStream stream, byte priority, IO.OperationType closeStreamAtEnd, Listener<Properties> onDone
	) {
		LoadPropertiesFileTask task = new LoadPropertiesFileTask(stream, priority, closeStreamAtEnd, onDone);
		task.startOn(stream.canStartReading(), false);
		return task.getOutput();
	}

	/** Constructor. */
	public LoadPropertiesFileTask(
		BufferedReadableCharacterStream stream, byte priority, IO.OperationType closeStreamAtEnd, Listener<Properties> onDone
	) {
		super("Load properties file", stream, priority, closeStreamAtEnd);
		this.onDone = onDone;
	}
	
	private Properties properties = new Properties();
	private Listener<Properties> onDone;
	
	@Override
	protected void processProperty(UnprotectedStringBuffer key, UnprotectedStringBuffer value) {
		properties.setProperty(key.asString(), value.asString());
	}
	
	@Override
	protected Properties generateResult() {
		if (onDone != null) onDone.fire(properties);
		return properties;
	}
	
}

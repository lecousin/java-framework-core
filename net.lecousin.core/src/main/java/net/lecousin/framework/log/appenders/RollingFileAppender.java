package net.lecousin.framework.log.appenders;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.util.LimitAsyncOperations;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerConfigurationException;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.text.StringUtil;

/** Log appender that appends to a file with a maximum size. Once a file is full,
 * a new file is created and the extension ".1" is added to the log file.
 * The extension of previous log files are also incremented.
 * If the number of log files reached a maximum number, the oldest one is removed.
 */
public class RollingFileAppender implements Appender, Closeable {

	/** Constructor. */
	public RollingFileAppender(LoggerFactory factory, String path, Level level, LogPattern pattern, long maxSize, int maxFiles) {
		this.factory = factory;
		this.file = new File(path);
		this.level = level;
		this.pattern = pattern;
		this.maxSize = maxSize;
		this.maxFiles = maxFiles;
		factory.getApplication().toClose(100000, this);
	}
	
	/** Constructor. */
	@SuppressWarnings("squid:S2589") // false positive, path is not always null
	public RollingFileAppender(
		LoggerFactory factory, XMLStreamReader reader, @SuppressWarnings({"unused","squid:S1172"}) Map<String,Appender> appenders
	) throws LoggerConfigurationException, XMLStreamException {
		this.factory = factory;
		String levelStr = null;
		String patternStr = null;
		String path = null;
		String size = null;
		String files = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("level".equals(attrName))
				levelStr = attrValue;
			else if ("pattern".equals(attrName))
				patternStr = attrValue;
			else if ("path".equals(attrName))
				path = attrValue;
			else if ("size".equals(attrName))
				size = attrValue;
			else if ("files".equals(attrName))
				files = attrValue;
			else if (!"name".equals(attrName) && !"class".equals(attrName))
				throw new LoggerConfigurationException("Unknown attribute " + attrName);
		}

		if (levelStr == null) throw new LoggerConfigurationException("Missing attribute level on rolling file Appender");
		try { this.level = Level.valueOf(levelStr); }
		catch (Exception t) { throw new LoggerConfigurationException("Invalid level " + levelStr); }
		
		if (patternStr == null) throw new LoggerConfigurationException("Missing attribute pattern on rolling file Appender");
		this.pattern = new LogPattern(patternStr);
		
		if (path == null) throw new LoggerConfigurationException("Missing attribute path on rolling file Appender");
		this.file = new File(path);

		if (size == null) throw new LoggerConfigurationException("Missing attribute size on rolling file Appender");
		try { maxSize = StringUtil.parseSize(size); }
		catch (ParseException e) { throw new LoggerConfigurationException("Invalid rolling file size: " + size, e); }
		if (maxSize <= 0)
			throw new LoggerConfigurationException("Invalid rolling file size: " + size);

		if (files == null) throw new LoggerConfigurationException("Missing attribute files on rolling file Appender");
		try { maxFiles = Integer.parseInt(files); }
		catch (NumberFormatException e) { throw new LoggerConfigurationException("Invalid maximum number of rolling files: " + files); }
		if (maxFiles <= 0)
			throw new LoggerConfigurationException("Invalid maximum number of rolling files: " + files);
		
		reader.next();
		do {
			if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
				break;
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				throw new LoggerConfigurationException("Unexpected inner element " + reader.getLocalName());
			}
			reader.next();
		} while (reader.hasNext());

		factory.getApplication().toClose(100000, this);
	}
	
	private LoggerFactory factory;
	private File file;
	private FileIO.WriteOnly output = null;
	private long maxSize;
	private int maxFiles;
	private LogPattern pattern;
	private Level level;
	private boolean closed = false;
	private LimitAsyncOperations<FileLogOperation, Void, IOException> opStack = new LimitAsyncOperations<>(100, FileLogOperation::execute, null);
	
	private static interface FileLogOperation {
		AsyncSupplier<Void, IOException> execute();
	}
	
	private class OpenLogFile implements FileLogOperation {
		@Override
		public AsyncSupplier<Void, IOException> execute() {
			AsyncSupplier<Void, IOException> result = new AsyncSupplier<>();
			Task.file(file, "Open log file", Task.Priority.RATHER_LOW, t -> {
				if (!file.exists()) {
					File dir = file.getParentFile();
					if (!dir.exists() && !dir.mkdirs())
						return error("Cannot create log directory: " + dir.getAbsolutePath(), null, result);
					try {
						if (!file.createNewFile())
							return cannotCreateLogFile(file, null, result);
					} catch (Exception e) {
						return cannotCreateLogFile(file, e, result);
					}
				}
				output = new FileIO.WriteOnly(file, Task.Priority.RATHER_LOW);
				output.seekAsync(SeekType.FROM_END, 0).onDone(() -> 
					output.writeAsync(
						ByteBuffer.wrap(("\nStart logging with max size = " + maxSize
							+ " and max files = " + maxFiles + "\n\n")
						.getBytes(StandardCharsets.UTF_8)))
					.onDone(() -> result.unblockSuccess(null), result),
				result);
				return null;
			}).start();
			return result;
		}
	}
	
	private class AppendLog implements FileLogOperation {
		
		public AppendLog(ByteBuffer log) {
			this.log = log;
		}
		
		private ByteBuffer log;
		
		@Override
		public AsyncSupplier<Void, IOException> execute() {
			AsyncSupplier<Void, IOException> result = new AsyncSupplier<>();
			long fileSize;
			try {
				fileSize = output.getSizeSync();
			} catch (IOException e) {
				result.error(e);
				return result;
			}
			if (fileSize >= maxSize) {
				output.closeAsync().thenStart(Task.file(file, "Roll log file", Task.Priority.RATHER_LOW, task -> {
					File dir = file.getParentFile();
					File f = new File(dir, file.getName() + '.' + maxFiles);
					try {
						Files.deleteIfExists(f.toPath());
					} catch (IOException e) {
						return error("Unable to remove log file " + f.getAbsolutePath(), e, result);
					}
					for (int i = maxFiles - 1; i >= 1; --i) {
						f = new File(dir, file.getName() + '.' + i);
						if (f.exists() && !f.renameTo(new File(dir, file.getName() + '.' + (i + 1))))
							return error("Unable to rename log file " + f.getAbsolutePath(), null, result);
					}
					f = new File(dir, file.getName() + ".1");
					if (!file.renameTo(f))
						return error("Cannot rename log file from " + file.getAbsolutePath()
							+ " to " + f.getAbsolutePath(), null, result);
					try {
						if (!file.createNewFile())
							return cannotCreateLogFile(file, null, result);
					} catch (Exception t) {
						return cannotCreateLogFile(file, t, result);
					}
					output = new FileIO.WriteOnly(file, Task.Priority.RATHER_LOW);
					output.writeAsync(log).onDone(() -> result.unblockSuccess(null), result);
					return null;
				}), result);
				return result;
			}
			output.writeAsync(log).onDone(() -> result.unblockSuccess(null), result);
			return result;
		}
	}
	
	private boolean first = true;
	
	@Override
	public synchronized void append(Log log) {
		if (closed) return;
		try {
			if (first) {
				opStack.write(new OpenLogFile());
				first = false;
			}
			StringBuilder msg = pattern.generate(log);
			msg.append('\n');
			opStack.write(new AppendLog(ByteBuffer.wrap(msg.toString().getBytes(StandardCharsets.UTF_8))));
		} catch (Exception e) {
			factory.getApplication().getConsole().err(new Exception("Error logging in file " + file.getAbsolutePath(), e));
			try { if (output != null) output.close(); }
			catch (Exception t) { /* ignore */ }
			output = null;
		}
	}

	@Override
	public int level() {
		return level.ordinal();
	}

	@Override
	public boolean needsThreadName() {
		return pattern.needsThreadName();
	}

	@Override
	public boolean needsLocation() {
		return pattern.needsLocation();
	}
	
	@Override
	public synchronized void close() throws IOException {
		closed = true;
		if (!first) {
			try {
				opStack.flush().blockThrow(0);
				if (output != null)
					output.close();
			} catch (Exception e) {
				throw IO.error(e);
			}
			output = null;
			first = true;
		}
	}
	
	@Override
	public IAsync<?> flush() {
		return opStack.flush();
	}
	
	private Void error(String message, Throwable cause, AsyncSupplier<Void, IOException> result) {
		IOException error = new IOException(message, cause);
		factory.getApplication().getConsole().err(error);
		result.error(error);
		return null;
	}
	
	private Void cannotCreateLogFile(File file, Throwable cause, AsyncSupplier<Void, IOException> result) {
		return error("Cannot create log file: " + file.getAbsolutePath(), cause, result);
	}
}

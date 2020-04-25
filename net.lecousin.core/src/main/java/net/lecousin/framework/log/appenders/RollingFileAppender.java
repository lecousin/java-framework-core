package net.lecousin.framework.log.appenders;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.util.LimitAsyncOperations;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.log.LogFilter;
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
public class RollingFileAppender extends Appender implements Closeable {
	
	private LoggerFactory factory;
	private File file;
	private FileIO.WriteOnly output = null;
	private long maxSize;
	private int maxFiles;
	private LogPattern pattern;
	private boolean closed = false;
	private LimitAsyncOperations<FileLogOperation, Void, IOException> opStack = new LimitAsyncOperations<>(100, FileLogOperation::execute, null);

	/** Constructor. */
	public RollingFileAppender(
		LoggerFactory factory, String path, Level level, LogPattern pattern, long maxSize, int maxFiles, List<LogFilter> filters
	) {
		super(level, filters);
		this.factory = factory;
		this.file = new File(path);
		this.pattern = pattern;
		this.maxSize = maxSize;
		this.maxFiles = maxFiles;
		factory.getApplication().toClose(100000, this);
	}
	
	/** Constructor. */
	@SuppressWarnings("squid:S2589") // false positive, path is not always null
	public RollingFileAppender(LoggerFactory factory, XMLStreamReader reader, Map<String,Appender> appenders)
	throws LoggerConfigurationException {
		super(factory, reader, appenders);
		factory.getApplication().toClose(100000, this);
	}
	
	@Override
	protected void init(LoggerFactory factory, Map<String, Appender> appenders) {
		this.factory = factory;
		this.maxSize = -1;
		this.maxFiles = -1;
		super.init(factory, appenders);
	}
	
	@Override
	protected boolean configureAttribute(String name, String value) throws LoggerConfigurationException {
		if ("pattern".equals(name)) {
			this.pattern = new LogPattern(value);
			return true;
		}
		if ("path".equals(name)) {
			this.file = new File(value);
			return true;
		}
		if ("size".equals(name)) {
			try { maxSize = StringUtil.parseSize(value); }
			catch (ParseException e) { throw new LoggerConfigurationException("Invalid rolling file size: " + value, e); }
			if (maxSize <= 0)
				throw new LoggerConfigurationException("Invalid rolling file size: " + value);
			return true;
		}
		if ("files".equals(name)) {
			try { maxFiles = Integer.parseInt(value); }
			catch (NumberFormatException e) {
				throw new LoggerConfigurationException("Invalid maximum number of rolling files: " + value);
			}
			if (maxFiles <= 0)
				throw new LoggerConfigurationException("Invalid maximum number of rolling files: " + value);
			return true;
		}
		return super.configureAttribute(name, value);
	}
	
	@Override
	protected void checkAttributes() throws LoggerConfigurationException {
		super.checkAttributes();
		if (pattern == null) throw new LoggerConfigurationException("Missing attribute pattern on console Appender");
		if (file == null) throw new LoggerConfigurationException("Missing attribute path on rolling file Appender");
		if (maxSize < 0) throw new LoggerConfigurationException("Missing attribute size on rolling file Appender");
		if (maxFiles < 0) throw new LoggerConfigurationException("Missing attribute files on rolling file Appender");
	}
	
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
	public boolean needsThreadName() {
		return pattern.needsThreadName();
	}

	@Override
	public boolean needsLocation() {
		return pattern.needsLocation();
	}
	
	@Override
	public String[] neededContexts() {
		return pattern.neededContexts();
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

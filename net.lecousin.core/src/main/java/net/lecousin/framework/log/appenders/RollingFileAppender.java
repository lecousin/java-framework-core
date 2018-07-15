package net.lecousin.framework.log.appenders;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.util.LimitAsyncOperations;
import net.lecousin.framework.concurrent.util.LimitAsyncOperations.Executor;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.util.StringUtil;

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
		factory.getApplication().toClose(this);
	}
	
	/** Constructor. */
	public RollingFileAppender(
		LoggerFactory factory, XMLStreamReader reader, @SuppressWarnings("unused") Map<String,Appender> appenders
	) throws Exception {
		this.factory = factory;
		String level = null;
		String pattern = null;
		String path = null;
		String size = null;
		String files = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			String attrName = reader.getAttributeLocalName(i);
			String attrValue = reader.getAttributeValue(i);
			if ("level".equals(attrName))
				level = attrValue;
			else if ("pattern".equals(attrName))
				pattern = attrValue;
			else if ("path".equals(attrName))
				path = attrValue;
			else if ("size".equals(attrName))
				size = attrValue;
			else if ("files".equals(attrName))
				files = attrValue;
			else if (!"name".equals(attrName) && !"class".equals(attrName))
				throw new Exception("Unknown attribute " + attrName);
		}

		if (level == null) throw new Exception("Missing attribute level on rolling file Appender");
		try { this.level = Level.valueOf(level); }
		catch (Throwable t) { throw new Exception("Invalid level " + level); }
		
		if (pattern == null) throw new Exception("Missing attribute pattern on rolling file Appender");
		this.pattern = new LogPattern(pattern);
		
		if (path == null) throw new Exception("Missing attribute path on rolling file Appender");
		this.file = new File(path);

		if (size == null) throw new Exception("Missing attribute size on rolling file Appender");
		try { maxSize = StringUtil.parseSize(size); }
		catch (NumberFormatException e) { throw new Exception("Invalid rolling file size: " + size); }
		if (maxSize <= 0)
			throw new Exception("Invalid rolling file size: " + size);

		if (files == null) throw new Exception("Missing attribute files on rolling file Appender");
		try { maxFiles = Integer.parseInt(files); }
		catch (NumberFormatException e) { throw new Exception("Invalid maximum number of rolling files: " + files); }
		if (maxFiles <= 0)
			throw new Exception("Invalid maximum number of rolling files: " + files);
		
		reader.next();
		do {
			if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
				break;
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				throw new Exception("Unexpected inner element " + reader.getLocalName());
			}
			reader.next();
		} while (reader.hasNext());

		factory.getApplication().toClose(this);
	}
	
	private LoggerFactory factory;
	private File file;
	private FileIO.WriteOnly output = null;
	private long maxSize;
	private int maxFiles;
	private LogPattern pattern;
	private Level level;
	private boolean closed = false;
	private LimitAsyncOperations<FileLogOperation, Void, Exception> opStack =
		new LimitAsyncOperations<FileLogOperation, Void, Exception>(100, new Executor<FileLogOperation, Void, Exception>() {

		@Override
		public AsyncWork<Void, Exception> execute(FileLogOperation data) {
			return data.execute();
		}
		
	});
	
	private static interface FileLogOperation {
		AsyncWork<Void, Exception> execute();
	}
	
	private class OpenLogFile implements FileLogOperation {
		@Override
		public AsyncWork<Void, Exception> execute() {
			AsyncWork<Void, Exception> result = new AsyncWork<>();
			new Task.OnFile<Void, NoException>(file, "Open log file", Task.PRIORITY_RATHER_LOW) {
				@Override
				public Void run() {
					if (!file.exists()) {
						File dir = file.getParentFile();
						if (!dir.exists())
							if (!dir.mkdirs()) {
								Exception error = new Exception("Cannot create log directory: "
									+ dir.getAbsolutePath());
								factory.getApplication().getConsole().err(error);
								result.error(error);
								return null;
							}
						try {
							if (!file.createNewFile()) {
								Exception error = new Exception("Cannot create log file: " + file.getAbsolutePath());
								factory.getApplication().getConsole().err(error);
								result.error(error);
								return null;
							}
						} catch (Exception e) {
							Exception error = new Exception("Cannot create log file: " + file.getAbsolutePath(), e);
							factory.getApplication().getConsole().err(error);
							result.error(error);
							return null;
						}
					}
					output = new FileIO.WriteOnly(file, Task.PRIORITY_RATHER_LOW);
					output.seekAsync(SeekType.FROM_END, 0).listenInlineSP(() -> {
						output.writeAsync(
							ByteBuffer.wrap(("\nStart logging with max size = " + maxSize
								+ " and max files = " + maxFiles + "\n\n")
							.getBytes(StandardCharsets.UTF_8)))
						.listenInlineSP(() -> {
							result.unblockSuccess(null);
						}, result);
					}, result);
					return null;
				}
			}.start();
			return result;
		}
	}
	
	private class AppendLog implements FileLogOperation {
		
		public AppendLog(ByteBuffer log) {
			this.log = log;
		}
		
		private ByteBuffer log;
		
		@Override
		public AsyncWork<Void, Exception> execute() {
			AsyncWork<Void, Exception> result = new AsyncWork<>();
			long fileSize;
			try {
				fileSize = output.getSizeSync();
			} catch (Exception e) {
				result.error(e);
				return result;
			}
			if (fileSize >= maxSize) {
				output.closeAsync().listenAsync(new Task.OnFile<Void, NoException>(file, "Roll log file", Task.PRIORITY_RATHER_LOW) {
					@Override
					public Void run() {
						File dir = file.getParentFile();
						File f = new File(dir, file.getName() + '.' + maxFiles);
						if (f.exists())
							if (!f.delete()) {
								result.error(new Exception("Unable to remove log file " + f.getAbsolutePath()));
								return null;
							}
						for (int i = maxFiles - 1; i >= 1; --i) {
							f = new File(dir, file.getName() + '.' + i);
							if (f.exists())
								if (!f.renameTo(new File(dir, file.getName() + '.' + (i + 1)))) {
									result.error(new Exception("Unable to rename log file "
										+ f.getAbsolutePath()));
									return null;
								}
						}
						f = new File(dir, file.getName() + ".1");
						if (!file.renameTo(f)) {
							Exception error = new Exception("Cannot rename log file from " + file.getAbsolutePath()
								+ " to " + f.getAbsolutePath());
							factory.getApplication().getConsole().err(error);
							result.error(error);
							return null;
						}
						try {
							if (!file.createNewFile()) {
								Exception error = new Exception("Cannot create log file: " + file.getAbsolutePath());
								factory.getApplication().getConsole().err(error);
								result.error(error);
								return null;
							}
						} catch (Throwable t) {
							Exception error = new Exception("Cannot create log file: " + file.getAbsolutePath(), t);
							factory.getApplication().getConsole().err(error);
							result.error(error);
							return null;
						}
						output = new FileIO.WriteOnly(file, Task.PRIORITY_RATHER_LOW);
						output.writeAsync(log).listenInlineSP(() -> { result.unblockSuccess(null); }, result);
						return null;
					}
				}, result);
				return result;
			}
			output.writeAsync(log).listenInlineSP(() -> { result.unblockSuccess(null); }, result);
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
			factory.getApplication().getConsole().err("Error logging in file " + file.getAbsolutePath() + ": " + e.getMessage());
			factory.getApplication().getConsole().err(e);
			try { if (output != null) output.close(); }
			catch (Throwable t) { /* ignore */ }
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
		if (first) {
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
	public ISynchronizationPoint<Exception> flush() {
		return opStack.flush();
	}
	
}

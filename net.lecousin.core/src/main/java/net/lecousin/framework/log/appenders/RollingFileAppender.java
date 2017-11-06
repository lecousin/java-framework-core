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
import net.lecousin.framework.io.FileIO;
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
	
	@Override
	public synchronized void append(Log log) {
		if (closed) return;
		try {
			if (output == null) {
				if (!file.exists()) {
					File dir = file.getParentFile();
					if (!dir.exists())
						if (!dir.mkdirs()) {
							factory.getApplication().getConsole()
								.err("Cannot create log directory: " + dir.getAbsolutePath());
							return;
						}
					if (!file.createNewFile()) {
						factory.getApplication().getConsole().err("Cannot create log file: " + file.getAbsolutePath());
						return;
					}
				}
				output = new FileIO.WriteOnly(file, Task.PRIORITY_RATHER_LOW);
				output.seekSync(SeekType.FROM_END, 0);
				output.writeSync(
					ByteBuffer.wrap(("\nStart logging with max size = " + maxSize + " and max files = " + maxFiles + "\n\n")
					.getBytes(StandardCharsets.UTF_8)));
			}
			if (output.getSizeSync() >= maxSize) {
				output.close();
				File dir = file.getParentFile();
				File f = new File(dir, file.getName() + '.' + maxFiles);
				if (f.exists()) if (!f.delete()) return;
				for (int i = maxFiles - 1; i >= 1; --i) {
					f = new File(dir, file.getName() + '.' + i);
					if (f.exists())
						if (!f.renameTo(new File(dir, file.getName() + '.' + (i + 1))))
							return;
				}
				f = new File(dir, file.getName() + ".1");
				if (!file.renameTo(f)) {
					factory.getApplication().getConsole()
						.err("Cannot rename log file from " + file.getAbsolutePath() + " to " + f.getAbsolutePath());
					return;
				}
				if (!file.createNewFile()) {
					factory.getApplication().getConsole().err("Cannot create log file: " + file.getAbsolutePath());
					return;
				}
				output = new FileIO.WriteOnly(file, Task.PRIORITY_RATHER_LOW);
			}
			StringBuilder msg = pattern.generate(log);
			msg.append('\n');
			output.writeSync(ByteBuffer.wrap(msg.toString().getBytes(StandardCharsets.UTF_8)));
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
		if (output != null) {
			output.close();
			output = null;
		}
	}
	
}

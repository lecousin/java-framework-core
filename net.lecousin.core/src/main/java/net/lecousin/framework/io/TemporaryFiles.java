package net.lecousin.framework.io;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectory;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;

/** Utility class to create temporary files. */
public final class TemporaryFiles {
	
	/** Get the instance for the current application. */
	public static TemporaryFiles get() {
		return get(LCCore.getApplication());
	}
	
	/** Get the instance for the given application. */
	public static TemporaryFiles get(Application app) {
		TemporaryFiles instance = app.getInstance(TemporaryFiles.class);
		if (instance == null) {
			File dir = null;
			String path = app.getProperty(Application.PROPERTY_TMP_DIRECTORY);
			if (path != null) {
				dir = new File(path);
				if (!dir.exists() && !dir.mkdirs())
					dir = null;
			}
			if (dir == null) {
				path = app.getProperty("java.io.tmpdir");
				if (path != null) {
					dir = new File(path);
					if (!dir.exists() && !dir.mkdirs())
						dir = null;
				}
			}
			instance = new TemporaryFiles(app, dir);
		}
		return instance;
	}
	
	private TemporaryFiles(Application app, File dir) {
		this.app = app;
		this.tempDir = dir;
		try {
			prefix = InetAddress.getLocalHost().getHostName();
		} catch (Exception t) {
			prefix = System.getenv("COMPUTERNAME");
			if (prefix == null || prefix.isEmpty())
				prefix = System.getenv("HOSTNAME");
			if (prefix == null)
				prefix = "";
		}
		prefix = prefix.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	}
	
	private Application app;
	private File tempDir;
	private String prefix;
	
	/** Set the directory into which create temporary files. */
	public void setTemporaryDirectory(File dir) throws IOException {
		if (dir != null && (!dir.exists() || !dir.isDirectory()) && !dir.mkdirs())
			throw new IOException("Unable to create temporary directory: " + dir.getAbsolutePath());
		tempDir = dir;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	/** Create a temporary file. */
	public File createFileSync(String prefix, String suffix) throws IOException {
		File tmp;
		if (tempDir == null)
			tmp = File.createTempFile(this.prefix + prefix, suffix);
		else
			tmp = File.createTempFile(this.prefix + prefix, suffix, tempDir);
		tmp.deleteOnExit();
		return tmp;
	}
	
	/** Create a temporary file. */
	public AsyncSupplier<File, IOException> createFileAsync(String prefix, String suffix) {
		return Task.file(tempDir, "Create temporary file", Priority.NORMAL,
			(Task<File, IOException> t) -> createFileSync(prefix, suffix)).start().getOutput();
	}
	
	/** Create and open a temporary file. */
	public FileIO.ReadWrite createAndOpenFileSync(String prefix, String suffix) throws IOException {
		File f = createFileSync(prefix, suffix);
		return new FileIO.ReadWrite(f, Task.Priority.NORMAL);
	}

	/** Create and open a temporary file. */
	public AsyncSupplier<FileIO.ReadWrite, IOException> createAndOpenFileAsync(String prefix, String suffix) {
		return Task.file(tempDir, "Create temporary file", Priority.NORMAL,
			(Task<FileIO.ReadWrite, IOException> t) -> createAndOpenFileSync(prefix, suffix)).start().getOutput();
	}
	
	/** Create a temporary directory. */
	public File createDirectorySync(String prefix) throws IOException {
		Path path = Files.createTempDirectory(tempDir.toPath(), this.prefix + prefix);
		File dir = path.toFile();
		app.toClose(1000, () -> RemoveDirectory.task(dir, null, 0, null, Priority.NORMAL, false).start().getOutput());
		return dir;
	}
	
	/** Create a temporary directory. */
	public AsyncSupplier<File, IOException> createDirectoryAsync(String prefix) {
		return Task.file(tempDir, "Create temporary directory", Priority.NORMAL,
			(Task<File, IOException> t) -> createDirectorySync(prefix)).start().getOutput();
	}
	
}

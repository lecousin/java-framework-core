package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.io.util.FileInfo;

/**
 * Task to retrieve a FileInfo from a file.
 */
public class GetFileInfo implements Executable<FileInfo, IOException> {

	/** Create task. */
	public static Task<FileInfo, IOException> task(File file, Priority priority) {
		return Task.file(file, "Get FileInfo on " + file.getAbsolutePath(), priority, new GetFileInfo(file), null);
	}
	
	/** Constructor. */
	public GetFileInfo(File file) {
		this.file = file;
	}
	
	private File file;
	
	@Override
	public FileInfo execute() throws IOException {
		FileInfo f = new FileInfo();
		f.file = file;
		f.path = file.toPath();
		BasicFileAttributes attr = Files.readAttributes(f.path, BasicFileAttributes.class);
		f.isDirectory = attr.isDirectory();
		f.isSymbolicLink = attr.isSymbolicLink();
		f.lastModified = attr.lastModifiedTime().toMillis();
		f.lastAccess = attr.lastAccessTime().toMillis();
		f.creation = attr.creationTime().toMillis();
		f.size = attr.size();
		return f;
	}
	
}

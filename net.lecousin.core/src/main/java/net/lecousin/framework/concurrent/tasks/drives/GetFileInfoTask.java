package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.util.FileInfo;

/**
 * Task to retrieve a FileInfo from a file.
 */
public class GetFileInfoTask extends Task.OnFile<FileInfo, IOException> {

	/** Constructor. */
	public GetFileInfoTask(File file, byte priority) {
		super(file, "Get FileInfo", priority);
		this.file = file;
	}
	
	private File file;
	
	@Override
	public FileInfo run() throws IOException {
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

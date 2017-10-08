package net.lecousin.framework.io.util;

import java.io.File;
import java.nio.file.Path;

/**
 * Contain information about a file, previously retrieved by a task.
 */
public class FileInfo {

	public File file;
	public Path path;
	public boolean isDirectory;
	public boolean isSymbolicLink;
	public long size;
	public long lastModified;
	public long lastAccess;
	public long creation;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FileInfo)) return false;
		FileInfo f = (FileInfo)obj;
		return file.equals(f.file);
	}
	
	@Override
	public int hashCode() {
		return file.hashCode();
	}
	
}

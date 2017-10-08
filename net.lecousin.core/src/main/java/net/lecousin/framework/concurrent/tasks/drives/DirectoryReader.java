package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.util.FileInfo;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Read the content of a directory (files and sub-directories).
 * For each element contained in the directory, information such as size or dates can be retrieved.
 */
public class DirectoryReader extends Task.OnFile<DirectoryReader.Result,AccessDeniedException> {
	
	/** Constructor. */
	public DirectoryReader(File dir, byte priority, Request request, WorkProgress progress) {
		super(dir, "Reading directory " + dir.getAbsolutePath(), priority);
		this.dir = dir;
		this.request = request;
		this.progress = progress;
		if (progress != null)
			WorkProgress.linkTo(progress, this);
	}

	/** Constructor. */
	public DirectoryReader(File dir, byte priority, Request request) {
		this(dir, priority, request, null);
	}
	
	private File dir;
	private Request request;
	private WorkProgress progress;
	
	public File getDirectory() { return dir; }
	
	/** Request specifying which information to retrieve on each element contained in the directory. */
	public static class Request {
		public boolean getLastModified = false;
		public boolean getLastAccess = false;
		public boolean getCreation = false;
		public boolean getIsSymbolicLink = false;
		public boolean getSize = false;
	}
	
	/** Result. */
	public static class Result {
		public int nbDirectories = 0;
		public int nbFiles = 0;
		public FileInfo[] files;
	}

	@Override
	public Result run() throws AccessDeniedException {
		File[] list = dir.listFiles();
		if (list == null)
			throw new AccessDeniedException("Directory " + dir.getAbsolutePath());
		if (progress != null) progress.progress(progress.getRemainingWork() / 2);
		Result result = new Result();
		int len = list.length;
		if (len == 0) {
			result.files = new FileInfo[0];
			return result;
		}
		result.files = new FileInfo[list.length];
		long work = progress != null ? progress.getRemainingWork() : 0;
		int nb = list.length;
		for (int i = 0; i < list.length; ++i) {
			long step = work / nb--;
			work -= step;
			FileInfo f = new FileInfo();
			result.files[i] = f;
			f.file = list[i];
			f.path = f.file.toPath();
			BasicFileAttributes attr;
			try { attr = Files.readAttributes(f.path, BasicFileAttributes.class); }
			catch (IOException e) {
				if (progress != null) progress.progress(step);
				getApplication().getDefaultLogger().error("Cannot get basic file attributes on " + f.file.getAbsolutePath(), e);
				continue;
			}
			if (attr.isDirectory()) {
				f.isDirectory = true;
				result.nbDirectories++;
			} else {
				f.isDirectory = false;
				result.nbFiles++;
				
				if (request != null && request.getSize)
					f.size = attr.size();
			}
			
			if (request != null) {
				if (request.getLastModified)
					f.lastModified = attr.lastModifiedTime().toMillis();
				if (request.getLastAccess)
					f.lastAccess = attr.lastAccessTime().toMillis();
				if (request.getCreation)
					f.creation = attr.creationTime().toMillis();
				if (request.getIsSymbolicLink)
					f.isSymbolicLink = attr.isSymbolicLink();
			}
			if (progress != null) progress.progress(step);
		}
		return result;
	}
	
	/** Task to list only sub-directories. */
	public static class ListSubDirectories extends Task.OnFile<ArrayList<File>,AccessDeniedException> {
		/** Constructor. */
		public ListSubDirectories(File dir, byte priority) {
			super(dir, "Listing sub directories", priority);
			this.dir = dir;
		}
		
		private File dir;
		
		@Override
		public ArrayList<File> run() throws AccessDeniedException {
			String[] names = dir.list();
			if (names == null) throw new AccessDeniedException("Directory " + dir.getAbsolutePath());
			ArrayList<File> result = new ArrayList<>();
			for (int i = 0; i < names.length; ++i) {
				File f = new File(dir, names[i]);
				if (f.isDirectory())
					result.add(f);
			}
			return result;
		}
	}
	
}

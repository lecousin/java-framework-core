package net.lecousin.framework.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.tasks.drives.DirectoryReader;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.util.FileInfo;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Utility class to go through a file system directory, calling methods when a directory or file is found.
 * <br/>
 * When a directory is found, the method directoryFound is called and can return an object that will
 * be passed to the subsequent calls to directoryFound and fileFound inside this directory.
 * When a file or directory is found inside the root directory, the object is the one given in the constructor.
 * <br/>
 * If the method directoryFound returns a null object, the directory won't be analyzed.
 * <br/>
 * The methods are called inside a CPU task so the drive task manager is made available as soon as possible.
 * 
 * @param <T> the type of object returned by directoryFound
 */
public abstract class DirectoryWalker<T> {

	/** Constructor. */
	public DirectoryWalker(File root, T rootObject, DirectoryReader.Request request) {
		this.root = root;
		this.rootObject = rootObject;
		this.request = request;
		this.taskManager = Threading.getDrivesTaskManager().getTaskManager(root);
	}
	
	private File root;
	private T rootObject;
	private DirectoryReader.Request request;
	private TaskManager taskManager;
	
	/** Start the directory analysis. */
	public IAsync<IOException> start(byte priority, WorkProgress progress, long work) {
		JoinPoint<IOException> jp = new JoinPoint<>();
		jp.addToJoin(1);
		processDirectory("", root, rootObject, jp, priority, progress, work);
		jp.start();
		return jp;
	}
	
	@SuppressWarnings("unused")
	protected void accessDenied(File dir, String path) {}
	
	protected abstract T directoryFound(T parent, FileInfo dir, String path);
	
	protected abstract void fileFound(T parent, FileInfo file, String path);
	
	private void processDirectory(String path, File dir, T object, JoinPoint<IOException> jp, byte priority, WorkProgress prog, long work) {
		DirectoryReader reader = new DirectoryReader(taskManager, dir, priority, request) {
			@Override
			public Result run() throws AccessDeniedException {
				if (prog != null) prog.setSubText(path);
				return super.run();
			}
		};
		reader.start();
		reader.getOutput().thenStart(new Task.Cpu<Void, NoException>("DirectoryWalker", priority) {
			@Override
			public Void run() {
				if (reader.hasError()) {
					accessDenied(dir, path);
					if (prog != null) prog.progress(work);
					jp.joined();
					return null;
				}
				DirectoryReader.Result result = reader.getResult();
				ArrayList<Triple<File, T, String>> dirs = new ArrayList<>(result.getNbDirectories());
				for (FileInfo f : result.getFiles()) {
					String p = path.length() == 0 ? f.file.getName() : path + '/' + f.file.getName();
					if (f.isDirectory) {
						T o = directoryFound(object, f, p);
						if (o != null)
							dirs.add(new Triple<>(f.file, o, p));
					} else {
						fileFound(object, f, p);
					}
				}
				jp.addToJoin(dirs.size());
				int steps = dirs.size() + 1;
				long step = work / steps--;
				long w = work - step;
				if (prog != null) prog.progress(step);
				for (Triple<File, T, String> t : dirs) {
					step = w / steps--;
					w -= step;
					processDirectory(t.getValue3(), t.getValue1(), t.getValue2(), jp, priority, prog, step);
				}
				jp.joined();
				return null;
			}
		}, true);
	}
	
}

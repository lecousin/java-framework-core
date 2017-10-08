package net.lecousin.framework.util;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.exception.NoException;

/**
 * Utility class to go through a file system directory, calling methods when a directory or file is found.
 * <br/>
 * When a directory is found, the method directoryFound is called and can return an object that will
 * be passed to the subsequent calls to directoryFound and fileFound inside this directory.
 * When a file or directory is found inside the root directory, the object is null.
 * <br/>
 * Two additional methods are called at the beginning (startDirectory) and at the end (endDirectory) of a directory.
 * If the startDirectory method returns false, nothing more is done for this directory.
 * <br/>
 * It is important to note that methods are called inside a drive task, so it can call methods to retrieve
 * information about the discovered file or directory, but must not do CPU consuming computing, else a
 * separate CPU task must be used. 
 * 
 * @param <T> the type of object returned by directoryFound
 */
public class DirectoryWalker<T> {

	/** Constructor. */
	public DirectoryWalker(File root) {
		this.root = root;
	}
	
	private File root;
	
	/** Start the directory analysis. */
	public ISynchronizationPoint<IOException> start(byte priority) {
		JoinPoint<IOException> jp = new JoinPoint<>();
		jp.addToJoin(1);
		new DirectoryReader(Threading.getDrivesTaskManager().getTaskManager(root), null, root, jp, priority).start();
		jp.start();
		return jp;
	}
	
	@SuppressWarnings("unused")
	protected boolean startDirectory(File dir, T dirObj) { return true; }
	
	@SuppressWarnings("unused")
	protected void endDirectory(File dir, T dirObj) {}
	
	@SuppressWarnings("unused")
	protected void accessDenied(File dir) {}
	
	@SuppressWarnings("unused")
	protected T directoryFound(T parent, File dir) { return null; }
	
	@SuppressWarnings("unused")
	protected void fileFound(T parent, File file) {}
	
	private class DirectoryReader extends Task.OnFile<Void, NoException> {
		public DirectoryReader(TaskManager manager, T object, File dir, JoinPoint<IOException> jp, byte priority) {
			super(manager, "Read directory content", priority);
			this.object = object;
			this.dir = dir;
			this.jp = jp;
		}
		
		private T object;
		private File dir;
		private JoinPoint<IOException> jp;
		
		@Override
		public Void run() {
			if (!startDirectory(dir, object)) {
				jp.joined();
				return null;
			}
			File[] list = dir.listFiles();
			if (list == null) {
				accessDenied(dir);
				jp.joined();
				return null;
			}
			if (list.length == 0) {
				jp.joined();
				return null;
			}
			for (File f : list) {
				if (f.isDirectory()) {
					T o = directoryFound(this.object, f);
					jp.addToJoin(1);
					new DirectoryReader(getTaskManager(), o, f, jp, getPriority()).start();
				} else {
					fileFound(this.object, f);
				}
			}
			endDirectory(dir, object);
			jp.joined();
			return null;
		}
	}
	
}

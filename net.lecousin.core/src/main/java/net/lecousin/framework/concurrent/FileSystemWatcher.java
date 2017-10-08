package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.log.Logger;

/**
 * Automatically launch and stop a thread to handle file system watching.
 * Because the available API is using blocking methods that unblock when an event occurs,
 * we need a dedicated thread to handle this.
 */
public final class FileSystemWatcher {
	
	private FileSystemWatcher() {
	}
	
	/** Interface to implement to receive events. */
	public static interface PathEventListener {
		/** Called when a new file has been created. */
		void fileCreated(Path parent, String childName);
		
		/** Called when a file has been remove. */
		void fileRemoved(Path parent, String childName);
		
		/** Called when a file has been modified. */
		void fileModified(Path parent, String childName);
	}

	/** Watch the given path. This will launch the thread if not yet launched. */
	public static Runnable watch(Path path, PathEventListener listener, WatchEvent.Kind<?>... kinds) throws IOException {
		synchronized (FileSystemWatcher.class) {
			Application app = LCCore.getApplication();
			Watcher watcher = app.getInstance(Watcher.class);
			if (watcher == null || watcher.stop) {
				watcher = new Watcher(app);
				app.setInstance(Watcher.class, watcher);
			}
			return watcher.watch(path, listener, kinds);
		}
	}
	
	/** Watch the given path. This will launch the thread if not yet launched. */
	public static Runnable watch(Path path, PathEventListener listener) throws IOException {
		return watch(path, listener,
			StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
	}
	
	private static class Watcher extends Thread implements Closeable {
		public Watcher(Application app) throws IOException {
			super("FileSystem Watcher");
			logger = app.getLoggerFactory().getLogger(FileSystemWatcher.class);
			service = FileSystems.getDefault().newWatchService();
			this.app = app;
			start();
			app.toClose(this);
		}
		
		private Application app;
		private WatchService service;
		private boolean stop = false;
		private Logger logger;
		
		private static class Listening {
			public Path path;
			public PathEventListener listener;
		}
		
		private static HashMap<WatchKey, Listening> keys = new HashMap<>();
		
		public Runnable watch(Path path, PathEventListener listener, WatchEvent.Kind<?>[] kinds) throws IOException {
			Listening listening = new Listening();
			listening.path = path;
			listening.listener = listener;
			WatchKey key;
			synchronized (keys) {
				keys.put(key = path.register(service, kinds), listening);
			}
			return new Runnable() {
				@Override
				public void run() {
					key.cancel();
					synchronized (FileSystemWatcher.class) {
						synchronized (keys) {
							keys.remove(key);
							if (keys.isEmpty()) close();
						}
					}
				}
			};
		}
		
		@Override
		public void run() {
			while (!stop) {
				WatchKey key;
	            try {
	                key = service.take();
	            } catch (InterruptedException x) {
	                break;
	            } catch (ClosedWatchServiceException x) {
	                break;
	            }
	            Listening l;
	            synchronized (keys) { l = keys.get(key); }
	            if (l == null) continue;
	            for (WatchEvent<?> event: key.pollEvents()) {
	                WatchEvent.Kind<?> kind = event.kind();
	                if (StandardWatchEventKinds.OVERFLOW.equals(kind))
	                	continue;
	                @SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>)event;
	                Path name = ev.context();
	                
	                if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
	                	if (logger.debug())
	                		logger.debug("File " + name.toString() + " created into " + l.path.toString());
	                	try { l.listener.fileCreated(l.path, name.toString()); }
	                	catch (Throwable t) {
	                		if (logger.error())
	                			logger.error("Exception thrown by FileSystemWatcher listener " + l.listener, t);
	                	}
	                } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
	                	if (logger.debug())
	                		logger.debug("File " + name.toString() + " removed from " + l.path.toString());
	                	try { l.listener.fileRemoved(l.path, name.toString()); }
	                	catch (Throwable t) {
	                		if (logger.error())
	                			logger.error("Exception thrown by FileSystemWatcher listener " + l.listener, t);
	                	}
	                } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
	                	if (logger.debug())
	                		logger.debug("File " + name.toString() + " modified into " + l.path.toString());
	                	try { l.listener.fileModified(l.path, name.toString()); }
	                	catch (Throwable t) {
	                		if (logger.error())
	                			logger.error("Exception thrown by FileSystemWatcher listener " + l.listener, t);
	                	}
	                }
	            }
	            
	            // reset key and remove from set if directory no longer accessible
	            boolean valid = key.reset();
	            if (!valid) {
	            	synchronized (keys) {
	            		keys.remove(key);
	            		if (keys.isEmpty()) close();
	            	}
	            }
			}
			app.closed(this);
			stop = true;
			try { service.close(); }
			catch (Throwable e) { /* ignore */ }
		}
		
		@Override
		public void close() {
			stop = true;
			try { service.close(); }
			catch (Throwable e) { /* ignore */ }
		}
	}
	
}

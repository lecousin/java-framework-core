package net.lecousin.framework.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.io.IOUtil;

/**
 * Task to create a directory.
 */
public class RenameFile implements Executable<Void,IOException> {
	
	/** Rename a file.
	 * It may do a copy then a delete, or a simple rename, depending if the source and destination are
	 * on the same drive or not.
	 */
	public static IAsync<IOException> rename(File source, File destination, Priority priority) {
		String partitionSource = Threading.getDrivesManager().getPartitionPath(source);
		String partitionDest = Threading.getDrivesManager().getPartitionPath(destination);

		if (Objects.equals(partitionSource, partitionDest))
			return new Task<>(Threading.getDrivesManager().getTaskManager(partitionSource), "Rename file", priority,
				new RenameFile(source, destination), null).start().getOutput();

		AsyncSupplier<Long, IOException> copy = IOUtil.copy(source, destination, priority, source.length(), null, 0, null);
		Async<IOException> result = new Async<>();
		copy.onDone(() -> RemoveFile.task(source, priority).start().getOutput().onDone(result), result);
		return result;
	}

	/** Constructor. */
	private RenameFile(File source, File destination) {
		this.source = source;
		this.destination = destination;
	}
	
	private File source;
	private File destination;
	
	@Override
	public Void execute(Task<Void, IOException> taskContext) throws IOException {
		if (destination.exists())
			throw new IOException("Unable to rename file " + source.getAbsolutePath()
				+ " into " + destination.getAbsolutePath() + " because the destination already exists");
		if (!source.renameTo(destination))
			throw new IOException("Unable to rename file " + source.getAbsolutePath() + " into " + destination.getAbsolutePath());
		return null;
	}
	
}

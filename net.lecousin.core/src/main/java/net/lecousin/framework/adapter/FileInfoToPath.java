package net.lecousin.framework.adapter;

import java.nio.file.Path;

import net.lecousin.framework.io.util.FileInfo;

/** Return the Path contained in a FileInfo. */
public class FileInfoToPath implements Adapter<FileInfo,Path> {

	@Override
	public boolean canAdapt(FileInfo input) {
		return input.path != null;
	}

	@Override
	public Path adapt(FileInfo input) {
		return input.path;
	}

	@Override
	public Class<FileInfo> getInputType() {
		return FileInfo.class;
	}

	@Override
	public Class<Path> getOutputType() {
		return Path.class;
	}
	
}

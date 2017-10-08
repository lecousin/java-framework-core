package net.lecousin.framework.adapter;

import java.io.File;

import net.lecousin.framework.io.util.FileInfo;

/** Return the File contained in a FileInfo. */
public class FileInfoToFile implements Adapter<FileInfo,File> {

	@Override
	public boolean canAdapt(FileInfo input) {
		return input.file != null;
	}

	@Override
	public File adapt(FileInfo input) {
		return input.file;
	}

	@Override
	public Class<FileInfo> getInputType() {
		return FileInfo.class;
	}

	@Override
	public Class<File> getOutputType() {
		return File.class;
	}
	
}

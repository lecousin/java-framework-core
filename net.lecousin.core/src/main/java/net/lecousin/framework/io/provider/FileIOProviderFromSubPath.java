package net.lecousin.framework.io.provider;

import java.io.File;

/** IOProviderFromName using a directory to provide FileIO. */
public class FileIOProviderFromSubPath implements IOProviderFrom.ReadWrite.Seekable.KnownSize.Resizable<String> {

	/** Constructor. */
	public FileIOProviderFromSubPath(File rootDir) {
		this.rootDir = rootDir;
	}
	
	private File rootDir;

	@Override
	public IOProvider.ReadWrite.Seekable.KnownSize.Resizable get(String subPath) {
		return new FileIOProvider(new File(rootDir, subPath));
	}
	
}

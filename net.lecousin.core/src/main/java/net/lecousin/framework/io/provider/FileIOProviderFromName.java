package net.lecousin.framework.io.provider;

import java.io.File;

import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Readable;

/** IOProviderFromName using a directory to provide FileIO. */
public class FileIOProviderFromName implements IOProviderFromName.Readable {

	/** Constructor. */
	public FileIOProviderFromName(File rootDir) {
		this.rootDir = rootDir;
	}
	
	private File rootDir;
	
	@Override
	public Readable provideReadableIO(String name, byte priority) {
		File f = new File(rootDir, name);
		if (!f.exists())
			return null;
		return new FileIO.ReadOnly(f, priority);
	}
	
}

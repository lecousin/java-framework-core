package net.lecousin.framework.io.provider;

import java.io.IOException;

import net.lecousin.framework.io.IO;

/** Provide an IO by name. */
public interface IOProviderFromName {

	/** Provide an IO.Readable by name. */
	public static interface Readable {
	
		/** Provide an IO.Readable by name. */
		public IO.Readable provideReadableIO(String name, byte priority) throws IOException;
		
		/** Provide an IO.Readable by name. */
		public static class SubPath implements Readable {
			
			/** Constructor. */
			public SubPath(Readable root, String subPath) {
				this.root = root;
				this.subPath = subPath;
			}
			
			protected Readable root;
			protected String subPath;
			
			@Override
			public net.lecousin.framework.io.IO.Readable provideReadableIO(String name, byte priority)
			throws IOException {
				return root.provideReadableIO(subPath + name, priority);
			}
			
		}
		
	}
	
}

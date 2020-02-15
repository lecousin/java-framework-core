package net.lecousin.framework.core.tests.util;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.drives.DirectoryReader;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.util.FileInfo;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.util.DirectoryWalker;

import org.junit.Assert;
import org.junit.Test;

public class TestDirectoryWalker extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		File root = TemporaryFiles.get().createDirectorySync("testWalker");
		File d1 = new File(root, "one");
		d1.mkdir();
		File d2 = new File(root, "two");
		d2.mkdir();
		File d1_1 = new File(d1, "one.one");
		d1_1.mkdir();
		File f1 = new File(root, "un");
		f1.createNewFile();
		File f2 = new File(d2, "deux");
		f2.createNewFile();
		
		MutableBoolean d1Found = new MutableBoolean(false);
		MutableBoolean d2Found = new MutableBoolean(false);
		MutableBoolean d1_1Found = new MutableBoolean(false);
		MutableBoolean f1Found = new MutableBoolean(false);
		MutableBoolean f2Found = new MutableBoolean(false);
		Mutable<Exception> error = new Mutable<>(null);
		
		DirectoryReader.Request request = new DirectoryReader.Request();
		request.readSize();
		DirectoryWalker<File> walker = new DirectoryWalker<File>(root, root, request) {
			
			@Override
			protected void fileFound(File parent, FileInfo file, String path) {
				if (file.file.equals(f1)) {
					if (parent != root)
						error.set(new Exception("parent of f1 must be root"));
					else
						f1Found.set(true);
					return;
				}
				if (file.file.equals(f2)) {
					if (parent != d2)
						error.set(new Exception("parent of f2 must be d2"));
					else
						f2Found.set(true);
					return;
				}
				error.set(new Exception("Unexpected file: " + path));
			}
			
			@Override
			protected File directoryFound(File parent, FileInfo dir, String path) {
				if (dir.file.equals(d1)) {
					if (parent != root)
						error.set(new Exception("parent of d1 must be root"));
					else
						d1Found.set(true);
					return d1;
				}
				if (dir.file.equals(d2)) {
					if (parent != root)
						error.set(new Exception("parent of d2 must be root"));
					else
						d2Found.set(true);
					return d2;
				}
				if (dir.file.equals(d1_1)) {
					if (parent != d1)
						error.set(new Exception("parent of d1_1 must be d1"));
					else
						d1_1Found.set(true);
					return d1_1;
				}
				error.set(new Exception("Unexpected directory: " + path));
				return dir.file;
			}
		};
		
		walker.start(Task.PRIORITY_NORMAL, null, 0).blockThrow(0);
		if (error.get() != null)
			throw error.get();
		Assert.assertTrue(d1Found.get());
		Assert.assertTrue(d2Found.get());
		Assert.assertTrue(d1_1Found.get());
		Assert.assertTrue(f1Found.get());
		Assert.assertTrue(f2Found.get());
	}
	
}

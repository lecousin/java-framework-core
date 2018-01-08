package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.math.FragmentedRangeInteger;
import net.lecousin.framework.math.RangeInteger;
import net.lecousin.framework.math.RangeLong;

public abstract class TestFragmented extends TestIO {

	public static class FragmentedFile {
		public byte[] testBuf;
		public int nbBuf;
		public File file;
		public int realSize;
		public List<RangeLong> fragments = new LinkedList<RangeLong>();
	}
	
	private static FragmentedFile[] files = null;
	
	private static synchronized void generateFragmentedFiles() throws IOException {
		if (files != null) return;
		List<Object[]> base = UsingTestData.generateTestCases(false);
		files = new FragmentedFile[base.size()];
		for (int i = 0; i < files.length; ++i) {
			files[i] = new FragmentedFile();
			Object[] b = base.get(i);
			files[i].testBuf = (byte[])b[0];
			files[i].nbBuf = ((Integer)b[1]).intValue();
			files[i].file = File.createTempFile("test", "lcfw_fragmented_" + files[i].nbBuf);
			files[i].file.deleteOnExit();
			if (files[i].nbBuf == 0) continue;
			generateFragmentedFile(files[i]);
		}
	}
	
	private static void generateFragmentedFile(FragmentedFile f) throws IOException {
		int size = f.testBuf.length * f.nbBuf;
		f.realSize = size * 3;
		System.out.println("Generating fragmented file of " + f.nbBuf + " buffers (size = " + size + ", real = " + f.realSize + ")");
		RandomAccessFile file = new RandomAccessFile(f.file, "rw");
		file.setLength(f.realSize);
		FragmentedRangeInteger free = new FragmentedRangeInteger(new RangeInteger(0, f.realSize - 1));
		int pos = 0;
		int maxWriteLen;
		if (size < 65536) maxWriteLen = 1024;
		else if (size < 128 * 1024) maxWriteLen = 4096;
		else if (size < 1024 * 1024) maxWriteLen = 16384;
		else maxWriteLen = 128 * 1024;
		while (pos < size) {
			int toWriteLen = size - pos;
			if (toWriteLen > maxWriteLen) toWriteLen = rand.nextInt(maxWriteLen - maxWriteLen / 10) + maxWriteLen / 10;
			write(f, file, pos, toWriteLen, free);
			pos += toWriteLen;
		}
		System.out.println(" => File generated with " + f.fragments.size() + " fragments");
		file.close();
	}
	private static void write(FragmentedFile f, RandomAccessFile file, int pos, int len, FragmentedRangeInteger free) throws IOException {
		do {
			int rangeIndex = rand.nextInt(free.size());
			RangeInteger range = free.get(rangeIndex);
			int rangePos = range.getLength() <= 100 ? 0 : rand.nextInt(range.getLength() - 100);
			int freeLen = range.getLength() - rangePos;
			if (freeLen > len) freeLen = len;
			RangeLong fragment = new RangeLong(range.min + rangePos, range.min + rangePos + freeLen - 1);
			f.fragments.add(fragment);
			file.seek(range.min + rangePos);
			do {
				int bufPos = pos % f.testBuf.length;
				int l = f.testBuf.length - bufPos;
				if (l > freeLen) l = freeLen;
				file.write(f.testBuf, bufPos, l);
				pos += l;
				len -= l;
				rangePos += l;
				freeLen -= l;
			} while (freeLen > 0 && len > 0);
			free.remove((int)fragment.min, (int)fragment.max);
		} while (len > 0);
	}
	
	public static synchronized List<Object[]> generateTestCases() throws IOException {
		if (files == null) generateFragmentedFiles();
		LinkedList<Object[]> list = new LinkedList<>();
		for (FragmentedFile f : files)
			list.add(new Object[] { f });
		return list;
	}
	
}

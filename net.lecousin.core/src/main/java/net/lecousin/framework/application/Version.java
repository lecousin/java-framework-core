package net.lecousin.framework.application;

import java.util.ArrayList;

/**
 * A version is composed of several numbers, separated by a dot,
 * and optionally can end with text such as 'beta', 'snapshot'...
 * It is built from a String and can be compared based on the numbers (but not the ending string).
 */
public class Version implements Comparable<Version> {

	/** Parse the given String to build a Version. */
	public Version(String s) {
		ArrayList<Integer> n = new ArrayList<Integer>();
		int val = 0;
		boolean hasChar = false;
		int i;
		for (i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				val = val * 10 + (c - '0');
				hasChar = true;
				continue;
			}
			if (c == '.') {
				n.add(Integer.valueOf(val));
				val = 0;
				hasChar = false;
				continue;
			}
			if (hasChar)
				n.add(Integer.valueOf(val));
			end = s.substring(i);
			break;
		}
		if (i == s.length()) n.add(Integer.valueOf(val));
		numbers = new int[n.size()];
		for (i = 0; i < n.size(); ++i)
			numbers[i] = n.get(i).intValue();
	}
	
	private int[] numbers;
	private String end;
	
	public int[] getNumbers() { return numbers; }
	
	@Override
	public int compareTo(Version o) {
		return compare(this.numbers, o.numbers);
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < numbers.length; ++i)
			hash = hash * 100 + numbers[i];
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Version)) return false;
		return compare(numbers, ((Version)obj).numbers) == 0;
	}
	
	/** Compare 2 arrays of integers containing versions. */
	public static int compare(int[] v1, int[] v2) {
		int i = 0;
		do {
			if (v1.length <= i) {
				if (v2.length <= i) return 0;
				return -1;
			} else 
				if (v2.length <= i) return 1;
			if (v1[i] < v2[i]) return -1;
			if (v1[i] > v2[i]) return 1;
			i++;
		} while (true);
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		toString(numbers, s);
		if (end != null)
			s.append(end);
		return s.toString();
	}
	
	/** Append the given version numbers to the given StringBuilder. */
	public static void toString(int[] v, StringBuilder s) {
		for (int i = 0; i < v.length; ++i) {
			if (i > 0) s.append('.');
			s.append(Integer.toString(v[i]));
		}
	}
	
}

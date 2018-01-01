package net.lecousin.framework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A path pattern is composed of elements separated by a / character.
 * Each element is a {@link WildcardFilePattern} that can contain * and ?.
 * In addition, each element may contain a double asterisks ** to specify any sub-directory structure.
 */
public class PathPattern implements IStringPattern {

	/** Constructor. */
	public PathPattern(String pattern) {
		do {
			int i = pattern.indexOf('/');
			if (i < 0) {
				patterns.add(new WildcardFilePattern(pattern));
				nbWildcards++;
				break;
			}
			String s = pattern.substring(0, i);
			if ("**".equals(s)) {
				patterns.add(null);
				nbAny++;
			} else {
				patterns.add(new WildcardFilePattern(s));
				nbWildcards++;
			}
			pattern = pattern.substring(i + 1);
		} while (pattern.length() > 0);
	}
	
	private ArrayList<WildcardFilePattern> patterns = new ArrayList<>();
	private int nbWildcards = 0;
	private int nbAny = 0;
	
	@Override
	public boolean matches(String path) {
		return matches(Arrays.asList(path.split("/")));
	}
	
	/** Return true if the given list of path elements is matching this pattern. */
	public boolean matches(List<String> path) {
		if (nbAny == 0 && path.size() != nbWildcards)
			return false;
		if (path.size() < nbWildcards)
			return false;
		return check(path, 0, 0, nbWildcards, nbAny);
	}
	
	/** Return true if the given list of path elements is matching this pattern. */
	public boolean matches(String[] path) {
		if (nbAny == 0 && path.length != nbWildcards)
			return false;
		if (path.length < nbWildcards)
			return false;
		return check(path, 0, 0, nbWildcards, nbAny);
	}
	
	private boolean check(List<String> path, int pathIndex, int pattIndex, int remainingWildcards, int remainingAny) {
		if (pattIndex >= patterns.size()) return false;
		WildcardFilePattern wc = patterns.get(pattIndex);
		if (wc != null) {
			if (!wc.matches(path.get(pathIndex)))
				return false;
			if (pathIndex == path.size() - 1)
				return true;
			return check(path, pathIndex + 1, pattIndex + 1, remainingWildcards - 1, remainingAny);
		}
		if (pattIndex == patterns.size() - 1)
			return true;
		int maxToSkip = path.size() - pathIndex - remainingWildcards;
		for (int i = 0; i <= maxToSkip; ++i)
			if (check(path, pathIndex + i, pattIndex + 1, remainingWildcards, remainingAny - 1))
				return true;
		return false;
	}
	
	private boolean check(String[] path, int pathIndex, int pattIndex, int remainingWildcards, int remainingAny) {
		if (pattIndex >= patterns.size()) return false;
		WildcardFilePattern wc = patterns.get(pattIndex);
		if (wc != null) {
			if (!wc.matches(path[pathIndex]))
				return false;
			if (pathIndex == path.length - 1)
				return true;
			return check(path, pathIndex + 1, pattIndex + 1, remainingWildcards - 1, remainingAny);
		}
		if (pattIndex == patterns.size() - 1)
			return true;
		int maxToSkip = path.length - pathIndex - remainingWildcards;
		for (int i = 0; i <= maxToSkip; ++i)
			if (check(path, pathIndex + i, pattIndex + 1, remainingWildcards, remainingAny - 1))
				return true;
		return false;
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	/** Return true if the given path elements match at least one of the given pattern. */
	public static boolean matches(Collection<PathPattern> patterns, List<String> path) {
		for (PathPattern pattern : patterns)
			if (pattern.matches(path))
				return true;
		return false;
	}
	
	/** Return true if the given path elements match at least one of the given pattern. */
	public static boolean matches(Collection<PathPattern> patterns, String[] path) {
		for (PathPattern pattern : patterns)
			if (pattern.matches(path))
				return true;
		return false;
	}
	
}

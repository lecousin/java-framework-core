package net.lecousin.framework.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;

import net.lecousin.framework.plugins.ExtensionPoint;

/**
 * Register available adapters.
 * It implements ExtensionPoint, meaning adapters can be automatically added by using the plug-in system.
 */
@SuppressWarnings("rawtypes")
public class AdapterRegistry implements ExtensionPoint<Adapter> {

	private static AdapterRegistry instance;
	
	/** Returns the instance of the adapter registry. */
	public static AdapterRegistry get() {
		if (instance == null) {
			instance = new AdapterRegistry();
			instance.adapters.add(new FileToIO.Writable());
			instance.adapters.add(new FileToIO.Readable());
			instance.adapters.add(new FileInfoToFile());
			instance.adapters.add(new FileInfoToPath());
		}
		return instance;
	}
	
	private AdapterRegistry() {
		// singleton
	}
	
	private ArrayList<Adapter> adapters = new ArrayList<>();
	
	@Override
	public void addPlugin(Adapter plugin) {
		synchronized (adapters) {
			adapters.add(plugin);
		}
	}
	
	@Override
	public void allPluginsLoaded() {
		// nothing to do
	}
	
	@Override
	public Collection<Adapter> getPlugins() {
		return adapters;
	}
	
	@Override
	public Class<Adapter> getPluginClass() {
		return Adapter.class;
	}
	
	/** Find an adapter that can adapt the given input into the given output type, and adapt it
	 * or return null if no available adapter can be found. 
	 */
	@SuppressWarnings("unchecked")
	public <Input, Output> Output adapt(Input input, Class<Output> outputType) throws AdapterException {
		Class<?> inputType = input.getClass();
		Adapter a = findAdapter(input, inputType, outputType);
		if (a == null) return null;
		return (Output)a.adapt(input);
	}
	
	/** Return true if an adapter can be found to adapt the given input into the given output type. */
	public boolean canAdapt(Object input, Class<?> outputType) {
		Class<?> inputType = input.getClass();
		Adapter a = findAdapter(input, inputType, outputType);
		return a != null;
	}
	
	/** Search for an adapter. */
	@SuppressWarnings("unchecked")
	public <Input, Output> Adapter<Input,Output> findAdapter(Object in, Class<Input> input, Class<Output> output) {
		ArrayList<Adapter> acceptInput = new ArrayList<>();
		ArrayList<Adapter> matching = new ArrayList<>();
		for (Adapter a : adapters) {
			if (!a.getInputType().isAssignableFrom(input) ||
				!a.canAdapt(in))
				continue;
			acceptInput.add(a);
			if (output.equals(a.getOutputType()))
				return a;
			if (output.isAssignableFrom(a.getOutputType()))
				matching.add(a);
		}
		if (acceptInput.isEmpty())
			return null;
		if (matching.size() == 1)
			return matching.get(0);
		if (!matching.isEmpty())
			return getBest(matching);
		LinkedList<LinkedList<Adapter>> paths = findPathsTo(input, acceptInput, output);
		LinkedList<Adapter> best = getBestPath(in, paths);
		if (best == null) return null;
		return new LinkedAdapter(best);
	}
	
	private static LinkedList<Adapter> getBestPath(Object in, LinkedList<LinkedList<Adapter>> paths) {
		LinkedList<Adapter> best = null;
		while (!paths.isEmpty()) {
			LinkedList<Adapter> path = paths.removeFirst();
			if (best != null && best.size() <= path.size()) continue;
			if (isPathValid(in, path))
				best = path;
		}
		return best;
	}
	
	@SuppressWarnings("unchecked")
	private static boolean isPathValid(Object in, LinkedList<Adapter> path) {
		Object o = in;
		// loop on each adapter and try to adapt, except for the last one
		ListIterator<Adapter> it = path.listIterator();
		while (it.nextIndex() < path.size() - 1) {
			Adapter a = it.next();
			if (!a.canAdapt(o))
				return false;
			try {
				o = a.adapt(o);
			} catch (Exception e) {
				return false;
			}
		}
		return it.next().canAdapt(o);
	}
	
	private static Adapter getBest(ArrayList<Adapter> list) {
		Adapter best = list.get(0);
		Class<?> bestType = best.getOutputType();
		for (int i = 1; i < list.size(); ++i) {
			Adapter a = list.get(i);
			Class<?> type = a.getOutputType();
			if (bestType.isAssignableFrom(type)) {
				bestType = type;
				best = a;
			}
		}
		return best;
	}
	
	private LinkedList<LinkedList<Adapter>> findPathsTo(Class<?> origin, ArrayList<Adapter> canUse, Class<?> target) {
		LinkedList<LinkedList<Adapter>> paths = new LinkedList<>();
		ArrayList<Class<?>> used = new ArrayList<>(1);
		used.add(origin);
		for (Adapter a : canUse) {
			Class<?> type = a.getOutputType();
			LinkedList<LinkedList<Adapter>> subPaths = findPaths(type, target, used);
			for (LinkedList<Adapter> subPath : subPaths) {
				subPath.addFirst(a);
				paths.add(subPath);
			}
		}
		return paths;
	}
	
	private LinkedList<LinkedList<Adapter>> findPaths(Class<?> from, Class<?> to, ArrayList<Class<?>> used) {
		LinkedList<LinkedList<Adapter>> paths = new LinkedList<>();
		ArrayList<Adapter> possible = getPossibleStarts(from, to, used, paths);
		if (possible.isEmpty()) return paths;
		ArrayList<Class<?>> newUsed = new ArrayList<>(used.size() + 1);
		newUsed.addAll(used);
		newUsed.add(from);
		for (Adapter a : possible) {
			LinkedList<LinkedList<Adapter>> subPaths = findPaths(a.getOutputType(), to, newUsed);
			for (LinkedList<Adapter> subPath : subPaths) {
				subPath.addFirst(a);
				paths.add(subPath);
			}
		}
		return paths;
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<Adapter> getPossibleStarts(Class<?> from, Class<?> to, ArrayList<Class<?>> used, LinkedList<LinkedList<Adapter>> paths) {
		ArrayList<Adapter> possible = new ArrayList<>();
		for (Adapter a : adapters) {
			if (!a.getInputType().isAssignableFrom(from)) continue;
			Class<?> out = a.getOutputType();
			if (to.isAssignableFrom(out)) {
				// we found it
				LinkedList<Adapter> list = new LinkedList<>();
				list.add(a);
				paths.add(list);
			} else {
				if (!containsOutputType(used, out))
					possible.add(a);
			}
		}
		return possible;
	}
	
	private static boolean containsOutputType(ArrayList<Class<?>> used, Class<?> out) {
		for (Class<?> c : used)
			if (c.isAssignableFrom(out) || out.isAssignableFrom(c))
				return true;
		return false;
	}
	
}

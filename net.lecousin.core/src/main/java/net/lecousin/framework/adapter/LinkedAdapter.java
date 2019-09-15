package net.lecousin.framework.adapter;

import java.util.LinkedList;

/**
 * Link several adapters to create a new one, adapting step by step the input to create the final output.
 */
@SuppressWarnings("rawtypes")
public class LinkedAdapter implements Adapter {

	/** Create a linked adapter. */
	@SuppressWarnings("squid:S1319") // we want to force LinkedList, not just a List
	public LinkedAdapter(LinkedList<Adapter> list) {
		this.list = list;
	}
	
	private LinkedList<Adapter> list;
	
	@Override
	public Class getInputType() {
		return list.getFirst().getInputType();
	}
	
	@Override
	public Class getOutputType() {
		return list.getLast().getOutputType();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean canAdapt(Object input) {
		return list.getFirst().canAdapt(input);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object adapt(Object input) throws AdapterException {
		Object o = input;
		for (Adapter a : list) {
			o = a.adapt(o);
			if (o == null) return null;
		}
		return o;
	}
	
}

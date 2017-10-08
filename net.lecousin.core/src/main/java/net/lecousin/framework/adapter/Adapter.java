package net.lecousin.framework.adapter;

import net.lecousin.framework.plugins.Plugin;

/**
 * An adapter allows to convert an object from one type to another.
 * When looking for an adapter, we may use successive adapters, so a type that needs to be
 * closed or released such as an IO, MUST NOT be an input of an adapter, but can be an output.
 * @param <Input> input type
 * @param <Output> output type
 */
public interface Adapter<Input,Output> extends Plugin {

	/** Return true if the given input can be transformed. */
	public boolean canAdapt(Input input);

	/** Convert the given input into the output type. */
	public Output adapt(Input input) throws Exception;
	
	/** Return the class of the input type. */
	public Class<Input> getInputType();
	
	/** Return the class of the output type. */
	public Class<Output> getOutputType();
	
}

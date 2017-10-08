package net.lecousin.framework.util;

/**
 * A factory that create instances based on a discriminator.
 * 
 * @param <InstantiationType> type of instances returned by this factory
 * @param <DiscriminatorType> type of discriminator to be given
 */
public interface Factory<InstantiationType,DiscriminatorType> {

	/** Create an instance for the given discriminator. */
	public InstantiationType create(DiscriminatorType discriminator);
	
}

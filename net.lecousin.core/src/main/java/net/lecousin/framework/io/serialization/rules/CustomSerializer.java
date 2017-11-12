package net.lecousin.framework.io.serialization.rules;

/** Customize the serialization of an attribute.
 * @param <SourceType> source type for serialization
 * @param <TargetType> target type for deserialization
 */
public interface CustomSerializer<SourceType,TargetType> {

	/** Source type. */
	Class<SourceType> sourceType();

	/** Target type. */
	Class<TargetType> targetType();
	
	/** Serialize an object of SourceType into a TargetType. */
	TargetType serialize(SourceType src);
	
	/** Deserialize an object of TargetType into a SourceType. */
	SourceType deserialize(TargetType src);
	
}

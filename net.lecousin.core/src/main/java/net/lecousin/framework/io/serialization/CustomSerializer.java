package net.lecousin.framework.io.serialization;

/** Customize the serialization. */
public interface CustomSerializer {

	/** Source type. */
	TypeDefinition sourceType();

	/** Target type. */
	TypeDefinition targetType();
	
	/** Serialize an object of SourceType into a TargetType. */
	Object serialize(Object src);
	
	/** Deserialize an object of TargetType into a SourceType. */
	Object deserialize(Object src);
	
}

package net.lecousin.framework.io.serialization;

import java.nio.charset.Charset;
import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

public abstract class AbstractDeserializer<Input> implements Deserializer {

	public static abstract class Text extends AbstractDeserializer<ICharacterStream.Readable.Buffered> {
		
		public Text(Charset charset, int bufferSize, int maxBuffers) {
			this.charset = charset;
			this.bufferSize = bufferSize;
			this.maxBuffers = maxBuffers;
		}
		
		private Charset charset;
		private int bufferSize;
		private int maxBuffers;
		
		@Override
		public <T> AsyncWork<T, Exception> deserialize(Class<T> type, IO.Readable input, List<SerializationRule> rules) {
			BufferedReadableCharacterStream in = new BufferedReadableCharacterStream(input, charset, bufferSize, maxBuffers);
			return deserializeObject(type, in, rules);
		}
	}
	
	public <T> AsyncWork<T, Exception> deserialize(Class<T> type, Input input, List<SerializationRule> rules) {
		
	}
	
}

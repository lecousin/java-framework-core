package net.lecousin.framework.io.serialization;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.io.text.BufferedWritableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

public abstract class AbstractSerializer<Output> implements Serializer {

	public static abstract class Text extends AbstractSerializer<ICharacterStream.Writable.Buffered> {
		
		public Text(Charset charset, int bufferSize) {
			this.charset = charset;
			this.bufferSize = bufferSize;
		}
		
		private Charset charset;
		private int bufferSize;
		
		@SuppressWarnings("resource")
		@Override
		public ISynchronizationPoint<Exception> serialize(Object object, IO.Writable output, List<SerializationRule> rules) {
			BufferedWritableCharacterStream out = new BufferedWritableCharacterStream(output, charset, bufferSize);
			SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
			ISynchronizationPoint<Exception> sp = serializeObject(object, out, rules);
			sp.listenInline(() -> {
				out.flush().listenInlineSP(result);
			}, result);
			return result;
		}
		
	}
	
	public ISynchronizationPoint<Exception> serializeObject(Object object, Output output, List<SerializationRule> rules) {
		if (object == null)
	}
	
}

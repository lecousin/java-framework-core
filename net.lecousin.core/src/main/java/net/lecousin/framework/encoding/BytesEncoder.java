package net.lecousin.framework.encoding;

import java.util.function.Function;

import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.util.Bytes;
import net.lecousin.framework.io.util.RawByteBuffer;

/** Encode bytes using a specific encoding. */
public interface BytesEncoder {
	
	/** Encode the given input into the output buffer.
	 * As much as possible input is encoded, but data may remain in the input buffer in case more data is expected
	 * or if the output buffer is full.
	 * @param input input buffer to encode
	 * @param output output buffer to fill with encoded bytes
	 * @param end true to signal no more data will come
	 */
	void encode(Bytes.Readable input, Bytes.Writable output, boolean end) throws EncodingException;
	
	/** Encode the given input into the output buffer.
	 * As much as possible input is encoded, but data may remain in the input buffer in case more data is expected
	 * or if the output buffer is full.
	 * @param input input buffer to encode
	 * @param offset input buffer offset to start encoding
	 * @param length number of bytes to encode from the input buffer
	 * @param output output buffer to fill with encoded bytes
	 * @param end true to signal no more data will come
	 * @return the number of bytes read from the input buffer
	 */
	default int encode(byte[] input, int offset, int length, Bytes.Writable output, boolean end) throws EncodingException {
		RawByteBuffer in = new RawByteBuffer(input, offset, length);
		encode(in, output, end);
		return in.currentOffset - offset;
	}
	
	/**
	 * Create a consumer that encodes received bytes and gives the encoded ones to the next consumer.
	 * @param <TError> type of error
	 * @param encodedConsumer consumer of encoded bytes
	 * @param errorConverter convert EncodingException into TError, or null if TError is compatible with EncodingException
	 * @return the consumer
	 */
	<TError extends Exception> AsyncConsumer<Bytes.Readable, TError>
	createEncoderConsumer(AsyncConsumer<Bytes.Readable, TError> encodedConsumer, Function<EncodingException, TError> errorConverter);
	
	/** Encoder that can determine the output size from the input size. */
	public interface KnownOutputSize extends BytesEncoder {

		/** Encode the given input and return the encoded bytes. */
		default byte[] encode(byte[] input, int offset, int length) throws EncodingException {
			return encode(new RawByteBuffer(input, offset, length));
		}
		
		/** Encode the given input and return the encoded bytes. */
		default byte[] encode(byte[] input) throws EncodingException {
			return encode(input, 0, input.length);
		}
	
		/** Encode the given input and return the encoded bytes. */
		byte[] encode(Bytes.Readable input) throws EncodingException;
		
	}
	
}

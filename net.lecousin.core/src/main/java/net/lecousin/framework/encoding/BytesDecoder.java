package net.lecousin.framework.encoding;

import java.util.function.Function;

import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.RawByteBuffer;

/** Decode bytes for a specific encoding. */
public interface BytesDecoder {

	/** Decode the given input into the output buffer.
	 * As much as possible input is decoded, but data may remain in the input buffer in case more data is expected
	 * or if the output buffer is full.
	 * @param input input buffer to decode
	 * @param output output buffer to fill with decoded bytes
	 * @param end true to signal no more data will come
	 */
	void decode(Bytes.Readable input, Bytes.Writable output, boolean end) throws EncodingException;
	
	/** Decode the given input into the output buffer.
	 * As much as possible input is decoded, but data may remain in the input buffer in case more data is expected
	 * or if the output buffer is full.
	 * @param input input buffer to decode
	 * @param offset input buffer offset to start decoding
	 * @param length number of bytes to decode from the input buffer
	 * @param output output buffer to fill with decoded bytes
	 * @param end true to signal no more data will come
	 * @return the number of bytes read from the input buffer
	 */
	default int decode(byte[] input, int offset, int length, Bytes.Writable output, boolean end) throws EncodingException {
		RawByteBuffer in = new RawByteBuffer(input, offset, length);
		decode(in, output, end);
		return in.currentOffset - offset;
	}
	
	/**
	 * Create a consumer that decodes received bytes and gives the decoded ones to the next consumer.
	 * @param <TError> type of error
	 * @param decodedConsumer consumer of decoded bytes
	 * @param errorConverter convert EncodingException into TError, or null if TError is compatible with EncodingException
	 * @return the consumer
	 */
	<TError extends Exception> AsyncConsumer<Bytes.Readable, TError>
	createDecoderConsumer(AsyncConsumer<Bytes.Readable, TError> decodedConsumer, Function<EncodingException, TError> errorConverter);

	/** Decoder that can determine the output size from the input size. */
	public interface KnownOutputSize extends BytesDecoder {
		
		/** Decode the given input and return decoded bytes. */
		default byte[] decode(byte[] input, int offset, int length) throws EncodingException {
			return decode(new RawByteBuffer(input, offset, length));
		}
		
		/** Decode the given input and return decoded bytes. */
		default byte[] decode(byte[] input) throws EncodingException {
			return decode(input, 0, input.length);
		}

		/** Decode the given input and return decoded bytes. */
		byte[] decode(Bytes.Readable input) throws EncodingException;
	}
	
}

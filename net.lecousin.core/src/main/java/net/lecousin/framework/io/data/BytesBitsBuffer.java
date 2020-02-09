package net.lecousin.framework.io.data;

/** Bytes as BitsBuffer. */
public interface BytesBitsBuffer extends BitsBuffer {

	/** Readable BytesBitBuffer. */
	interface Readable extends BytesBitsBuffer, BitsBuffer.Readable {
		
		/** Big-endian order. */
		public class BigEndian implements BytesBitsBuffer.Readable, BitsBuffer.BigEndian {
			
			/** Constructor. */
			public BigEndian(Bytes.Readable bytes) {
				this.bytes = bytes;
			}
			
			private Bytes.Readable bytes;
			private byte b;
			private int mask = 0;
			
			@Override
			public boolean get() {
				if (mask == 0) {
					b = bytes.get();
					mask = 0x80;
				}
				boolean bit = (b & mask) != 0;
				mask >>= 1;
				return bit;
			}
			
			@Override
			public boolean hasRemaining() {
				return mask != 0 || bytes.hasRemaining();
			}
			
			@Override
			public int remaining() {
				int r = bytes.remaining() * 8;
				switch (mask) {
				//case 0x80: return r + 8;
				case 0x40: return r + 7;
				case 0x20: return r + 6;
				case 0x10: return r + 5;
				case 0x08: return r + 4;
				case 0x04: return r + 3;
				case 0x02: return r + 2;
				case 0x01: return r + 1;
				default: return r + 0;
				}
			}
			
			@Override
			public void alignToNextByte() {
				mask = 0;
			}

		}
		
		/** Little-endian order. */
		public class LittleEndian implements BytesBitsBuffer.Readable, BitsBuffer.LittleEndian {
			
			/** Constructor. */
			public LittleEndian(Bytes.Readable bytes) {
				this.bytes = bytes;
			}
			
			private Bytes.Readable bytes;
			private byte b;
			private int mask = 0x100;
			
			@Override
			public boolean get() {
				if (mask == 0x100) {
					b = bytes.get();
					mask = 1;
				}
				boolean bit = (b & mask) != 0;
				mask <<= 1;
				return bit;
			}
			
			@Override
			public boolean hasRemaining() {
				return mask != 0x100 || bytes.hasRemaining();
			}
			
			@Override
			public int remaining() {
				int r = bytes.remaining() * 8;
				switch (mask) {
				case 0x80: return r + 1;
				case 0x40: return r + 2;
				case 0x20: return r + 3;
				case 0x10: return r + 4;
				case 0x08: return r + 5;
				case 0x04: return r + 6;
				case 0x02: return r + 7;
				//case 0x01: return r + 8;
				default: return r + 0;
				}
			}
			
			@Override
			public void alignToNextByte() {
				mask = 0x100;
			}

		}
		
	}

	/** Writable BytesBitBuffer. */
	interface Writable extends BytesBitsBuffer, BitsBuffer.Writable {
		
		/** Big-endian order. */
		public class BigEndian implements BytesBitsBuffer.Writable, BitsBuffer.BigEndian {
			
			/** Constructor. */
			public BigEndian(Bytes.Writable bytes) {
				this.bytes = bytes;
			}
			
			private Bytes.Writable bytes;
			
			private byte b = 0;
			private int bit = 7;
			
			@Override
			public boolean hasRemaining() {
				return bytes.hasRemaining();
			}

			@Override
			public int remaining() {
				return (bytes.remaining() - 1) * 8 + bit + 1;
			}

			@Override
			public void alignToNextByte(boolean fillBit) {
				if (bit == 7)
					return;
				if (fillBit) {
					for (int i = bit; i >= 0; --i)
						b |= 1 << i;
				}
				bit = 7;
				bytes.put(b);
				b = 0;
			}

			@Override
			public void put(boolean bit) {
				if (bit) {
					b |= 1 << this.bit;
				}
				if (--this.bit < 0) {
					bytes.put(b);
					this.bit = 7;
					b = 0;
				}
				
			}
		}
		
		/** Little-endian order. */
		public class LittleEndian implements BytesBitsBuffer.Writable, BitsBuffer.LittleEndian {
			
			/** Constructor. */
			public LittleEndian(Bytes.Writable bytes) {
				this.bytes = bytes;
			}
			
			private Bytes.Writable bytes;
			private byte b = 0;
			private int bit = 0;
			
			@Override
			public boolean hasRemaining() {
				return bytes.hasRemaining();
			}

			@Override
			public int remaining() {
				return (bytes.remaining() - 1) * 8 + (8 - bit);
			}

			@Override
			public void alignToNextByte(boolean fillBit) {
				if (bit == 0)
					return;
				if (fillBit) {
					for (int i = bit; i < 8; ++i)
						b |= 1 << i;
				}
				bytes.put(b);
				bit = 0;
				b = 0;
			}

			@Override
			public void put(boolean bit) {
				if (bit) {
					b |= 1 << this.bit;
				}
				if (++this.bit == 8) {
					bytes.put(b);
					this.bit = 0;
					b = 0;
				}
			}
		}
		
	}

}

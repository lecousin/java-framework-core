package net.lecousin.framework.io.data;

/** Read bits from a byte. */
public interface SingleByteBitsBuffer extends BitsBuffer {

	/** Readable single byte bits buffer. */
	interface Readable extends SingleByteBitsBuffer, BitsBuffer.Readable {
	
		/** Big endian order. */
		public class BigEndian implements SingleByteBitsBuffer.Readable, BitsBuffer.BigEndian {
			
			/** Constructor. */
			public BigEndian(byte b) {
				this.b = b;
			}
			
			private byte b;
			private int mask = 0x80;
			
			@Override
			public boolean get() {
				boolean bit = (b & mask) != 0;
				mask >>= 1;
				return bit;
			}
			
			@Override
			public boolean hasRemaining() {
				return mask != 0;
			}
			
			@Override
			public int remaining() {
				switch (mask) {
				case 0x80: return 8;
				case 0x40: return 7;
				case 0x20: return 6;
				case 0x10: return 5;
				case 0x08: return 4;
				case 0x04: return 3;
				case 0x02: return 2;
				case 0x01: return 1;
				default: return 0;
				}
			}
			
			@Override
			public void alignToNextByte() {
				mask = 0;
			}
			
		}
		
		/** Little endian order. */
		public class LittleEndian implements SingleByteBitsBuffer.Readable, BitsBuffer.LittleEndian {
			
			/** Constructor. */
			public LittleEndian(byte b) {
				this.b = b;
			}
	
			private byte b;
			private int mask = 1;
			
			@Override
			public boolean get() {
				boolean bit = (b & mask) != 0;
				mask <<= 1;
				return bit;
			}
			
			@Override
			public boolean hasRemaining() {
				return mask != 0x100;
			}
			
			@Override
			public int remaining() {
				switch (mask) {
				case 0x80: return 1;
				case 0x40: return 2;
				case 0x20: return 3;
				case 0x10: return 4;
				case 0x08: return 5;
				case 0x04: return 6;
				case 0x02: return 7;
				case 0x01: return 8;
				default: return 0;
				}
			}
			
			@Override
			public void alignToNextByte() {
				mask = 0x100;
			}
			
		}
	
	}
	
	/** Writable single byte bits buffer. */
	interface Writable extends SingleByteBitsBuffer, BitsBuffer.Writable {
		
		byte getByte();

		/** Big-endian order. */
		public class BigEndian implements SingleByteBitsBuffer.Writable, BitsBuffer.BigEndian {
			
			private byte b = 0;
			private int bit = 7;
			
			@Override
			public byte getByte() {
				return b;
			}

			@Override
			public boolean hasRemaining() {
				return bit >= 0;
			}

			@Override
			public int remaining() {
				return bit + 1;
			}

			@Override
			public void alignToNextByte(boolean fillBit) {
				if (bit == -1 || bit == 7)
					return;
				if (fillBit) {
					do {
						b |= 1 << bit--;
					} while (bit >= 0);
				} else {
					bit = -1;
				}
			}

			@Override
			public void put(boolean bit) {
				if (bit) {
					b |= 1 << this.bit;
				}
				this.bit--;
			}
			
		}

		/** Little-endian order. */
		public class LittleEndian implements SingleByteBitsBuffer.Writable, BitsBuffer.LittleEndian {
			
			private byte b = 0;
			private int bit = 0;
			
			@Override
			public byte getByte() {
				return b;
			}

			@Override
			public boolean hasRemaining() {
				return bit < 8;
			}

			@Override
			public int remaining() {
				return 8 - bit;
			}

			@Override
			public void alignToNextByte(boolean fillBit) {
				if (bit == 0 || bit == 8)
					return;
				if (fillBit) {
					do {
						b |= 1 << bit++;
					} while (bit < 8);
				} else {
					bit = 8;
				}
			}

			@Override
			public void put(boolean bit) {
				if (bit) {
					b |= 1 << this.bit;
				}
				this.bit++;
			}

		}
		
	}
	
}

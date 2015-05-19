package com.mashery.tools.httpconcurrentclient;


public abstract class BufferFactory {

		public abstract byte[] getBuffer();

		public abstract void releaseBuffer(byte[] buf);


	public static class DefaultBufferFactory extends BufferFactory {

		public static final BufferFactory INSTANCE = new DefaultBufferFactory(4096);

		private final int size;

		public DefaultBufferFactory(int size) {
			this.size = size;
		}

		@Override
		public byte[] getBuffer() {
			return new byte[size];
		}

		@Override
		public void releaseBuffer(byte[] buf) {
			// do nothing
		}
	}
}


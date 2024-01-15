package com.chaosinmotion.git.test.utils;

import java.io.IOException;

/**
 * Provides a random access interface across a byte array
 */
public class RandomAccessByteArray implements RandomAccess
{
	private int offset;
	private byte[] data;

	public RandomAccessByteArray(byte[] data)
	{
		this.data = data;
	}

	@Override
	public void seek(int offset) throws IOException
	{
		this.offset = offset;
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException
	{
		if ((off < 0) || (len < 0) || ((off + len) > buffer.length)) {
			throw new IndexOutOfBoundsException();
		}

		int remain = data.length - offset;
		if (remain <= 0) return -1;

		if (len > remain) len = remain;
		System.arraycopy(data, offset, buffer, off, len);
		offset += len;

		return len;
	}

	@Override
	public long length() throws IOException
	{
		return data.length;
	}

	@Override
	public void close() throws IOException
	{
		data = null;
	}
}

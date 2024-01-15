package com.chaosinmotion.git.test.utils;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This is a random access reader. This is a wrapper around a Java
 * RandomAccessFile object
 */
public class RandomAccessReader implements RandomAccess
{
	private RandomAccessFile file;

	public RandomAccessReader(RandomAccessFile file)
	{
		this.file = file;
	}

	@Override
	public void seek(int offset) throws IOException
	{
		file.seek(offset);
	}

	@Override
	public int read(byte[] buffer, int i, int rlen) throws IOException
	{
		return file.read(buffer, i, rlen);
	}

	@Override
	public long length() throws IOException
	{
		return file.length();
	}

	public void close() throws IOException
	{
		file.close();
	}
}

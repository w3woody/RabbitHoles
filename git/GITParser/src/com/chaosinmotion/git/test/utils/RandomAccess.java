package com.chaosinmotion.git.test.utils;

import java.io.IOException;

/**
 * This is a random access reader interface. This defines the common methods
 * we need when reading data and processing it with our delta class.
 *
 * We do this because we have a need to be able to perform random access
 * reading against an in-memory binary object, and it'd be nice to have a
 * common interface for that.
 */
public interface RandomAccess
{
	void seek(int offset) throws IOException;
	int read(byte[] buffer, int i, int rlen) throws IOException;
	long length() throws IOException;
	void close() throws IOException;
}

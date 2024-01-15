package com.chaosinmotion.git.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class Stream
{
	/**
	 * Read size-encoded integer. Basically the bottom 7 bits of each byte
	 * are part of the integer; the MSB indicates if there are more bits to
	 * shift in.
	 *
	 * This is used in the encoding of the file sizes in the delta data
	 *
	 * @param is The input stream to read from
	 * @return The integer read
	 * @throws IOException
	 */
	public static long readSizeEncoded(InputStream is) throws IOException
	{
		long ret = 0;
		int ch;
		int shift = 0;

		/*
		 *	Read the bytes and shift in the answer until we hit a byte
		 * 	that has an MSB of 0.
		 */

		for (;;) {
			ch = is.read();
			if (ch == -1) throw new IOException("Unexpected EOF");

			ret |= (long)(ch & 0x7f) << shift;
			shift += 7;
			if ((ch & 0x80) == 0) break;
		}
		return ret;
	}

	/**
	 * Read size-encoded integer. Basically the bottom 7 bits of each byte
	 * are part of the integer; the MSB indicates if there are more bits to
	 * shift in.
	 *
	 * This is used in the encoding of the object reference in the ofs-delta
	 * data object.
	 *
	 * @param is The input stream to read from
	 * @return The integer read
	 * @throws IOException
	 */
	public static long readSizeEncoded(RandomAccessFile is) throws IOException
	{
		long ret = 0;
		int ch;

		/*
		 *	Read the bytes and shift in the answer until we hit a byte
		 * 	that has an MSB of 0.
		 */

		ch = is.read();
		ret = (ch & 0x7f);
		while ((ch & 0x80) != 0) {
			ch = is.read();
			ret = ((ret + 1) << 7) | (ch & 0x7f);
		}

		return ret;
	}
}



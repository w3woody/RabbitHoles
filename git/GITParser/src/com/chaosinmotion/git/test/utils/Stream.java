package com.chaosinmotion.git.test.utils;

import java.io.*;

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
	public static long readAltSizeEncoded(RandomAccessFile is) throws IOException
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

	/**
	 * Fun question: how do we generate something that is readable by our
	 * alternate size encoded integer string? This is the best I could come
	 * up with: first, unspool the digits off the bottom, then write the
	 * bytes back to front...
	 *
	 * @param value
	 * @param result
	 * @throws IOException
	 */
	public static void writeSizeEncoded(long value, OutputStream result) throws IOException
	{
		// a 64-bit value should require no more than 10 bytes, so alloc 12 to be safe
		byte[] buffer = new byte[12];
		byte pos;

		/*
		 *	Get the bytes off the bottom
		 */

		pos = 0;
		buffer[pos++] = (byte)(0x7F & value);
		value >>>= 7;			// assume unsigned value
		while (value != 0) {
			--value;			// subtract so the next byte encodes 1..256
			buffer[pos++] = (byte)(0x7F & value);
			value >>>= 7;
		}

		/*
		 *	At this point our value is encoded in inverse order in the buffer.
		 * 	First, set the MSB for all but the *first* byte (which is written
		 * 	last)
		 */

		for (byte i = 1; i < pos; ++i) buffer[i] |= 0x80;

		/*
		 *	Next, reverse the order of our buffer
		 */

		byte i = 0, j = (byte)(pos-1);		// NOTE: pos is guaranteed to be >= 1
		while (i < j) {
			byte tmp = buffer[i];
			buffer[i] = buffer[j];
			buffer[j] = tmp;
			++i;
			--j;
		}

		/*
		 *	Now write the results, which should be decodable by the
		 * 	routine above.
		 */

		result.write(buffer, 0, pos);
	}

	/**
	 * This tests our encoding process.
	 * @throws IOException
	 */
	public static void testEncoding() throws IOException
	{
		long test = 123456L;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeSizeEncoded(test,baos);
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		// Read
		long ret = 0;
		int ch;

		/*
		 *	Read the bytes and shift in the answer until we hit a byte
		 * 	that has an MSB of 0.
		 */

		ch = bais.read();
		ret = (ch & 0x7f);
		while ((ch & 0x80) != 0) {
			ch = bais.read();
			ret = ((ret + 1) << 7) | (ch & 0x7f);
		}

		if (test != ret) throw new IOException("Encoding test failed");
	}
}



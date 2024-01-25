package com.chaosinmotion.git.test.protocol.parsing;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses the PKT-LINE protocol used by the GIT smart protocols.
 */

public class PktLineReader
{
	public static class Return
	{
		public final PktLineType code;
		public final byte[] data;

		public Return(PktLineType code, byte[] data)
		{
			this.code = code;
			this.data = data;
		}
	}

	private InputStream is;
	private byte[] header = new byte[4];	// For reading the header

	public PktLineReader(InputStream is)
	{
		this.is = is;
	}

	private static int hex(byte b) throws IOException
	{
		if ((b >= '0') && (b <= '9')) return b - '0';
		if ((b >= 'a') && (b <= 'f')) return 10 + (b - 'a');
		if ((b >= 'A') && (b <= 'F')) return 10 + (b - 'A');
		throw new IOException("Invalid hex character: " + b);
	}

	private int parseHeader() throws IOException
	{
		int ret = 0;

		for (int i = 0; i < header.length; ++i) {
			byte b = header[i];
			ret = (ret << 4) | hex(b);
		}
		return ret;
	}

	/**
	 * Read the next block of data
	 * @return The next block of data with controls, or null of at EOF
	 * @throws IOException
	 */
	public Return read() throws IOException
	{
		/*
		 *	read the next 4 bytes and parse as hex
		 */

		int len = is.read(header);
		if (len == -1) return null;		// EOF

		int size = parseHeader();		// The size of the next block, or a special code

		if (size == 0) return new Return(PktLineType.FLUSH, null);
		if (size == 1) return new Return(PktLineType.DELIM, null);
		if (size == 2) return new Return(PktLineType.END, null);
		if (size == 3) throw new IOException("Unknown header type");

		// Allocate and read a block
		size -= 4;
		byte[] data = new byte[size];
		int pos = 0;

		while (pos < size) {
			int rlen = size - pos;
			rlen = is.read(data,pos,rlen);
			if (rlen == -1) throw new IOException("Short read");
			pos += rlen;
		}

		return new Return(PktLineType.LINE, data);
	}
}


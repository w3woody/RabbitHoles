package com.chaosinmotion.git.test.protocol.parsing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes the PKT-LINE protocol used by the GIT smart protocols.
 */

public class PktLineWriter
{
	private boolean isClosed = false;
	private OutputStream os;
	private byte[] header = new byte[4];

	public PktLineWriter(OutputStream os)
	{
		this.os = os;
	}

	private static byte toHex(int val)
	{
		if (val < 10) return (byte)('0' + val);
		return (byte)('a' + (val - 10));
	}

	private void toSize(int size) throws IOException
	{
		if (size > 65520) throw new IOException("Packet too large");
		header[0] = toHex((size >> 12) & 0x0f);
		header[1] = toHex((size >> 8) & 0x0f);
		header[2] = toHex((size >> 4) & 0x0f);
		header[3] = toHex(size & 0x0f);
	}

	public void write(byte[] data, int offset, int len) throws IOException
	{
		toSize(len+4);
		os.write(header);
		os.write(data,offset,len);
	}

	public void write(byte[] data) throws IOException
	{
		write(data,0,data.length);
	}

	public void write(String data) throws IOException
	{
		write(data.getBytes(StandardCharsets.UTF_8));
	}

	public void write(PktLineType type) throws IOException
	{
		switch (type) {
			case FLUSH:
				toSize(0);
				os.write(header);
				break;

			case DELIM:
				toSize(1);
				os.write(header);
				break;

			case END:
				toSize(2);
				os.write(header);
				break;

			default:
				throw new IOException("Unknown type: " + type);
		}
	}

	public void close() throws IOException
	{
		if (isClosed) return;
		isClosed = true;

		os.close();
	}
}

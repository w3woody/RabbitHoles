package com.chaosinmotion.git.test.protocol.parsing;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the PKT-LINE protocol used by the GIT smart protocols.
 */

public class PktLineWriter
{
	private OutputStream os;
	private byte[] header = new byte[4];

	public PktLineWriter(OutputStream os)
	{
		this.os = os;
	}

	private void toSize(int size) throws IOException
	{
		if (size > 65520) throw new IOException("Packet too large");
		header[0] = (byte)((size >> 12) & 0x0f);
		header[1] = (byte)((size >> 8) & 0xff);
		header[2] = (byte)((size >> 4) & 0xff);
		header[3] = (byte)(size & 0xff);
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
}

package com.chaosinmotion.git.test.objects;

import com.chaosinmotion.git.test.utils.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The tree reader takes the **contents** of a tree object file (that is, the
 * data after the header, that would be returned by ObjectReader), and reads
 * out the tree records.
 *
 * Each tree record is stored with the format:
 *
 * (mode)(space)(name)(\0)(sha1)
 *
 * Mode is followed by a space, and represents the Unix file mode of the
 * specified file. (?)
 *
 * Name is a variable length string, terminated by a null byte.
 *
 * SHA1 is always 20 bytes.
 */
public class TreeReader
{
	public static class Record
	{
		public final String mode;
		public final String name;
		public final byte[] sha1;			// always a 20 byte array

		private Record(String mode, String name, byte[] sha1)
		{
			this.mode = mode;
			this.name = name;
			this.sha1 = sha1;
		}

		public String toString()
		{
			return String.format("%s %s %s", mode, name, Hex.toString(sha1));
		}
	}

	private InputStream in;
	private byte[] buffer = new byte[20];	// reusable scratch buffer

	public TreeReader(InputStream is)
	{
		in = is;
	}

	/**
	 * Reads the next entry, throwing an error if the entry is malformed.
	 * This will return null when there are no more entries.
	 * @return The next tree entry in the list
	 */
	public Record read() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int ch;

		/*
		 *	Mode is space separated
		 */

		while (-1 != (ch = in.read())) {
			if (ch == ' ') break;
			baos.write(ch);
		}
		String mode = baos.toString(StandardCharsets.UTF_8);	// always assume UTF-8

		/*
		 *	Name is null terminated
		 */
		baos.reset();
		while (-1 != (ch = in.read())) {
			if (ch == 0) break;
			baos.write(ch);
		}
		String name = baos.toString(StandardCharsets.UTF_8);	// always assume UTF-8

		/*
		 *	SHA1 is always 20 bytes.
		 */
		if (20 != in.read(buffer,0,20)) return null;

		return new Record(mode,name,buffer);
	}
}

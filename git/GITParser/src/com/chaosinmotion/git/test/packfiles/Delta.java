package com.chaosinmotion.git.test.packfiles;

import com.chaosinmotion.git.test.utils.Hex;
import com.chaosinmotion.git.test.utils.RandomAccess;
import com.chaosinmotion.git.test.utils.Stream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * This represents a delta operation from within a pack file that
 * transforms an input file to an output file.
 */
public class Delta
{
	private interface Op
	{
		void apply(RandomAccess in, OutputStream out) throws IOException;
	}

	private class CopyOp implements Op
	{
		final int offset;
		final int size;

		CopyOp(int offset, int size)
		{
			this.offset = offset;
			this.size = size;
		}

		public void apply(RandomAccess in, OutputStream out) throws IOException
		{
			in.seek(offset);
			byte[] buffer = new byte[4096];

			int pos = 0;
			int rlen;
			while (pos < size) {
				rlen = size - pos;
				if (rlen > buffer.length) rlen = buffer.length;

				int len = in.read(buffer, 0, rlen);
				if (len <= 0) throw new IOException("Unexpected EOF");
				out.write(buffer, 0, len);

				pos += rlen;
			}
		}

		public String toString()
		{
			return String.format("copy %d %d", offset, size);
		}
	}

	private class InsertOp implements Op
	{
		final byte[] data;

		InsertOp(byte[] data)
		{
			this.data = data;
		}

		// NOTE: Ignores the file we're modifying from
		public void apply(RandomAccess in, OutputStream out) throws IOException
		{
			out.write(data);
		}

		public String toString()
		{
			// Strictly for testing purposes. There is no reason to believe
			// in general that data is a string or string fragment.
			return "insert " + data.length + ": \"" + Hex.toCompactString(data) + "\"";
		}
	}

	/*
	 *	One of the two values below will be defined
	 */
	public final long offset;
	public final byte[] sha;
	public final long baseSize;
	public final long resultSize;
	private final Op[] oplist;

	/**
	 * Given the offset and the block of decompressed data from the PACK file
	 * this constructs the delta list for this object
	 */

	public Delta(long offset, byte[] data) throws IOException
	{
		this.offset = offset;
		this.sha = null;

		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		baseSize = Stream.readSizeEncoded(bais);
		resultSize = Stream.readSizeEncoded(bais);

		oplist = parseDelta(bais);
	}

	/**
	 * Given the offset and the block of decompressed data from the PACK file
	 * this constructs the delta list for this object
	 */

	public Delta(byte[] sha, byte[] data) throws IOException
	{
		this.offset = 0;
		this.sha = sha;

		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		baseSize = Stream.readSizeEncoded(bais);
		resultSize = Stream.readSizeEncoded(bais);

		oplist = parseDelta(bais);
	}

	/**
	 * Given an input stream this parses the delta data table.
	 * @param in The input stream to read from
	 * @return The list of operations to perform
	 */
	private Op[] parseDelta(InputStream in) throws IOException
	{

		/*
		 *	The rest of the data are a collection of delta commands as outlined
		 * 	in gitformat-pack.txt in the GIT source documentation.
		 *
		 *  Note too the encoding of the instructions are as in RFC-1951, with
		 *  the least-significant bit left-most. (Thus, 10000000 is 1.)
		 *
		 * 	That is, the copy command is:
		 *
		 * 	1xxxxxxx off1 off2 off3 off4 size1 size2 size3
		 *
		 * 	Where the x's indicate which of the elements of the off or size
		 * 	word are not zero. Note the encoding is from MSB to LSB, thus:
		 *
		 *  10000101 off1 off3
		 *
		 * 	Also note if a field is missing, the field is presumed to be
		 * 	populated with zero.
		 *
		 * 	If the size is 0, then the size is taken as 0x10000
		 *
		 * 	Then there is
		 *
		 *  0xxxxxxx [data]
		 *
		 * 	where xxx is the number of bytes (from 1 to 127) to copy from
		 * 	data.
		 *
		 * 	Finally 0x00 is a reserved instruction.
		 *
		 * 	We read until there are no more bytes to read.
		 */

		ArrayList<Op> list = new ArrayList<>();
		for (;;) {
			int ch = in.read();
			if (ch == -1) break;

			if ((ch & 0x80) != 0) {
				// Copy command
				int off = 0;
				int size = 0;

				if ((ch & 0x01) != 0) {
					off = in.read();
					if (off == -1) throw new IOException("Unexpected EOF");
				}
				if ((ch & 0x02) != 0) {
					off |= in.read() << 8;
					if (off == -1) throw new IOException("Unexpected EOF");
				}
				if ((ch & 0x04) != 0) {
					off |= in.read() << 16;
					if (off == -1) throw new IOException("Unexpected EOF");
				}
				if ((ch & 0x08) != 0) {
					off |= in.read() << 24;
					if (off == -1) throw new IOException("Unexpected EOF");
				}

				if ((ch & 0x10) != 0) {
					size = in.read();
					if (size == -1) throw new IOException("Unexpected EOF");
				}
				if ((ch & 0x20) != 0) {
					size |= in.read() << 8;
					if (size == -1) throw new IOException("Unexpected EOF");
				}
				if ((ch & 0x40) != 0) {
					size |= in.read() << 16;
					if (size == -1) throw new IOException("Unexpected EOF");
				}
				if (size == 0) size = 0x10000;

				list.add(new CopyOp(off, size));
			} else if (ch != 0) {
				// Insert command
				byte[] data = new byte[ch];
				if (ch != in.read(data)) {
					throw new IOException("Unexpected EOF");
				}
				list.add(new InsertOp(data));
			} else {
				// Ignore
			}
		}

		return list.toArray(new Op[list.size()]);
	}


	/**
	 * Given an input file that this delta represents, and the output
	 * stream to write to, this applies the delta and writes the data out.
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public void apply(RandomAccess in, OutputStream out) throws IOException
	{
		for (Op op : oplist) {
			op.apply(in, out);
		}
	}

	public void dump()
	{
		if (sha != null) {
			System.out.println("Delta: ref " + Hex.toString(sha));
		} else {
			System.out.println("Delta: ofs " + offset);
		}
		System.out.println("    baseSize: " + baseSize + " resultSize: " + resultSize);
		System.out.println();
		for (Op op : oplist) {
			System.out.println("    " + op);
		}
	}
}

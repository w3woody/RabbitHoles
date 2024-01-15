package com.chaosinmotion.git.test.utils;

/**
 * In order to understand what's going on I need a way to dump the contents
 * of a byte array as a readable hex stream.
 */
public class Hex
{
	public static void dump(byte[] buffer, int start, int len)
	{
		int rlen = buffer.length - start;
		if (rlen > len) len = rlen;
		if (len <= 0) {
			System.out.println("Empty buffer");
			return;
		}

		int nrows = (len + 15) / 16;
		int row = 0;
		int offset = start;
		while (row < nrows) {
			int rowlen = len;
			if (rowlen > 16) rowlen = 16;
			System.out.printf("%04x: ",offset);
			for (int i = 0; i < rowlen; i++) {
				System.out.printf("%02x ",buffer[offset+i]);
				if (i == 7) System.out.print(" ");
			}
			for (int i = rowlen; i < 16; i++) {
				System.out.print("   ");
				if (i == 7) System.out.print(" ");
			}
			System.out.print("  ");
			for (int i = 0; i < rowlen; i++) {
				char ch = (char)buffer[offset+i];
				if (ch < 32 || ch > 127) ch = '.';
				System.out.print(ch);
				if (i == 7) System.out.print(" ");
			}
			System.out.println();
			offset += rowlen;
			len -= rowlen;
			row++;
		}
	}

	public static void dump(byte[] buffer)
	{
		dump(buffer, 0, buffer.length);
	}

	/**
	 * Convert a byte array to a hex string
	 * @param buffer
	 * @return
	 */
	public static String toString(byte[] buffer)
	{
		StringBuilder sb = new StringBuilder();
		for (byte b: buffer) {
			sb.append(String.format("%02x",b));
		}
		return sb.toString();
	}

	public static String toCompactString(byte[] buffer)
	{
		StringBuilder sb = new StringBuilder();
		for (byte b: buffer) {
			if (b >= 32) { // implicit b <
				sb.append((char) b);
			} else if (b == '\n') {
				sb.append("\\n");
			} else if (b == '\r') {
				sb.append("\\r");
			} else if (b == '\t') {
				sb.append("\\t");
			} else {
				sb.append(String.format("\\x%02x", b));
			}
		}
		return sb.toString();
	}

	private static int toHex(char c)
	{
		if ((c >= 'a') && (c <= 'f')) return 10 + c - 'a';
		if ((c >= 'A') && (c <= 'F')) return 10 + c - 'A';
		if ((c >= '0') && (c <= '9')) return c - '0';
		return 0;		// should never happen
	}

	/**
	 * Convert a hex string to a byte array
	 * @param buffer
	 * @return
	 */

	public static byte[] toByteArray(String buffer)
	{
		int i,len = buffer.length();
		byte[] ret = new byte[len/2];

		int j = 0;
		for (i = 0; (i < len) && (j < ret.length); i += 2) {
			int hi = toHex(buffer.charAt(i));
			int lo = toHex(buffer.charAt(i+1));
			ret[j++] = (byte)((hi << 4) | lo);
		}

		return ret;
	}

	/**
	 * Compare two byte arrays
	 * @param sha1bytes
	 * @param sha1read
	 * @return
	 */
	public static boolean equals(byte[] sha1bytes, byte[] sha1read)
	{
		if (sha1bytes.length != sha1read.length) return false;
		for (int i = 0; i < sha1bytes.length; i++) {
			if (sha1bytes[i] != sha1read[i]) return false;
		}
		return true;
	}

	/**
	 * Lexically compare the two byte arrays for lexical order.
	 * @param a The first array
	 * @param b The second array
	 * @return
	 */
	public static int compare(byte[] a, byte[] b)
	{
		int pos = 0;

		while ((pos < a.length) && (pos < b.length)) {
			int cmp = (a[pos] & 0xff) - (b[pos] & 0xff);
			if (cmp != 0) return cmp;
			pos++;
		}
		if (pos < a.length) return 1;
		if (pos < b.length) return -1;
		return 0;
	}
}

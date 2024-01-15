package com.chaosinmotion.git.test.packfiles;

import com.chaosinmotion.git.test.common.ValidateResult;
import com.chaosinmotion.git.test.utils.Hex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This wraps a random access file in order to provide access to reading
 * the index file format on GIT. This supports both version 1 and version 2
 * index files.
 *
 * The index file description is here:
 *
 * https://shafiul.github.io//gitbook/7_the_packfile.html
 *
 * NOTE: fanout tables and offsets into the index file are zero-based indexes
 * into the array, rather than byte offsets into the table. This file also
 * loads the first fanout table into memory for quick access, meaning it's
 * useful to open this file and leave it open so long as we need to access
 * the pack file this indexes.
 */
public class IndexReader
{
	private RandomAccessFile file;
	private int version;
	private int[] fanout;	// a 256-byte table

	private int size;		  // Number of entries in the index
	private long offsetStart; // Version 1: Byte offset of offset/sha table. 2: sha list
	private long offsetCRC;   // Version 2: Byte offset of CRC table
	private long offsetPack;  // Version 2: Byte offset of pack offset table
	private long offset64Pack;// Version 2: Byte offset of 64-bit pack offset table

	public static class Record
	{
		public final byte[] sha1;
		public final long offset;
		public final int crc;

		Record(byte[] sha1, long offset, int crc)
		{
			this.sha1 = sha1;
			this.offset = offset;
			this.crc = crc;
		}
	}

	/**
	 * Internal method returning the min and max index bounds for searching
	 * in our arrays
	 */
	private static class Bounds
	{
		private final int min;
		private final int max;

		private Bounds(int min, int max)
		{
			this.min = min;
			this.max = max;
		}
	}

	/**
	 * Opens the file for access. This reads the header and the first fanout
	 * table into memory.
	 * @param f
	 */
	public IndexReader(File f) throws IOException
	{
		file = new RandomAccessFile(f, "r");	// We only need read access

		/*
		 *	Read the fanout table header. Note that RandomAccessFile supports
		 * 	DataInput, which helps us read the 4-byte integers that comprise
		 * 	the bulk of this file.
		 */

		int h = file.readInt();
		int v = file.readInt();

		fanout = new int[256];
		if (h == 0xff744f63) {		// "\377tOc"
			version = v;
			for (int i = 0; i < 256; ++i) {
				fanout[i] = file.readInt();
			}
		} else {
			version = 1;
			fanout[0] = h;
			fanout[1] = v;
			for (int i = 2; i < 256; ++i) {
				fanout[i] = file.readInt();
			}
		}

		/*
		 *	Preflight the calculated offsets for our tables
		 */

		size = fanout[255];			// The last entry in the fanout is the size
		if (version == 1) {
			// We only care about the offset to the offset/sha table, which is
			// a fixed offset from the start of the file
			offsetStart = 1024;		// 256 * 4 bytes per entry
		} else if (version == 2) {
			// Version 2: We have multiple tables we need the offsets for
			// Starting offset to sha list is

			offsetStart = 1032;		// 256 * 4 bytes per entry + 8 for header
			offsetCRC = offsetStart + (size * 20);
			offsetPack = offsetCRC + (size * 4);
			offset64Pack = offsetPack + (size * 4);
		} else {
			throw new IOException("Invalid index version");
		}
	}

	/**
	 * Get the version of the index file. Version 1 does not have CRC checksums
	 * for our compressed objects
	 * @return The version; 1 or 2
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * Close the random access file.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		file.close();
	}

	/**
	 * This validates the index file and the associated pack file. Note
	 * the provided filename can refer to either the index file or the
	 * pack file.
	 * @param f The file to validate
	 * @return Returns the validation result. Note that this fails if
	 * either of the files are not found.
	 * @throws IOException
	 */
	public static ValidateResult validate(File f) throws IOException,
			NoSuchAlgorithmException
	{
		File parent = f.getParentFile();
		String filename = f.getName();

		int ext = filename.lastIndexOf('.');
		String prefix;
		if (ext != -1) {
			prefix = filename.substring(0, ext);
		} else {
			prefix = filename;
		}

		File ixfile = new File(parent, prefix + ".idx");
		File pkfile = new File(parent, prefix + ".pack");
		if (!ixfile.exists() || !pkfile.exists()) {
			return ValidateResult.NOT_FOUND;
		}

		/*
		 *	Read the SHA1 values for the packfile and the ixfile.
		 */

		RandomAccessFile ix = new RandomAccessFile(ixfile, "r");
		RandomAccessFile pk = new RandomAccessFile(pkfile, "r");

		if (ix.length() < 40) {
			ix.close();
			pk.close();
			return ValidateResult.INVALID;
		}

		/*
		 *	Run checksum of the index file
		 */

		long ixlen = ix.length() - 40;
		byte[] idxsha1 = new byte[20];
		byte[] pksha1 = new byte[20];
		ix.seek(ixlen);
		ix.read(pksha1);			// Read packfile checksum
		ix.read(idxsha1);			// read index file checksum

		/*
		 *	Now rewind and run the SHA1 checksum on the index file
		 *
		 * 	We run the SHA1 checksum for everything up until the checksum
		 * 	suffix. (That is, read all but the last 20 bytes of the index file
		 * 	when calculating the SHA-1 checksum.)
		 */

		ix.seek(0);

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] buffer = new byte[1024];
		long pos = 0;
		int len;

		ixlen += 20;
		while (pos < ixlen) {
			long rlen = ixlen - pos;
			if (rlen > buffer.length) rlen = buffer.length;

			len = ix.read(buffer, 0, (int)rlen);
			if (len <= 0) break;
			md.update(buffer, 0, len);

			pos += len;
		}

		byte[] digest = md.digest();
		if (!Hex.equals(idxsha1, digest)) {
			ix.close();
			pk.close();
			return ValidateResult.INVALID;
		}

		/*
		 *	Now run the SHA1 checksum on the pack file, up until the packfile
		 * 	checksum suffix.
		 */

		long pklen = pk.length() - 20;
		md.reset();

		pos = 0;
		while (pos < pklen) {
			long rlen = pklen - pos;
			if (rlen > buffer.length) rlen = buffer.length;

			len = pk.read(buffer, 0, (int)rlen);
			if (len <= 0) break;
			md.update(buffer, 0, len);

			pos += len;
		}

		digest = md.digest();
		if (!Hex.equals(pksha1, digest)) {
			ix.close();
			pk.close();
			return ValidateResult.INVALID;
		}

		/*
		 *	Make sure the checksum of the packfile matches the checksum we
		 * 	read out of the index
		 */

		byte[] fpksha1 = new byte[20];
		pk.seek(pklen);
		pk.read(fpksha1);

		if (!Hex.equals(pksha1, fpksha1)) {
			ix.close();
			pk.close();
			return ValidateResult.INVALID;
		}

		System.out.println("Validated " + f.getName());
		System.out.println("Checksum: " + Hex.toString(pksha1));

		return ValidateResult.VALID;
	}

	/**
	 *  Get the specified offset into the pack file of the SHA1 file provided.
	 *  @param sha1 The SHA1 of the object to search for
	 *  @return The offset into the pack file, or -1 if not found
	 */

	public Record getRecord(String sha1) throws IOException
	{
		byte[] sha1bytes = Hex.toByteArray(sha1);

		return getRecord(sha1bytes);
	}

	public Record getRecord(byte[] sha1bytes) throws IOException
	{
		/*
		 *	How we access the information depends on the version of the index.
		 * 	Version 1 access is a bit easier.
		 */

		if (version == 1) {
			return getVersion1Record(sha1bytes);
		} else {
			return getVersion2Record(sha1bytes);
		}
	}

	/**
	 * Return the boundary indexes. Min is the minimum index; max is the index
	 * of the item *past* the last item in the range. If min == max, there are
	 * no items in the SHA array.
	 * @param sha1bytes
	 * @return
	 */
	private Bounds findSHA1Bounds(byte[] sha1bytes)
	{
		int min, max;
		int index = sha1bytes[0] & 0xff;

		if (index == 0) {
			min = 0;
		} else {
			min = fanout[index - 1];
		}
		max = fanout[index];

		return new Bounds(min, max);
	}

	/**
	 * Perform a binary search on the SHA1 array, returning the found index or
	 * -1 if not found. Please note that
	 * @param sha1Bytes
	 * @param min
	 * @param max
	 * @return
	 */
	private int binarySearch(byte[] sha1Bytes, int min, int max) throws
			IOException
	{
		byte[] sha1 = new byte[20];

		while (min < max) {
			int mid = (min + max) / 2;

			/*
			 *	Get the SHA1 key, depending on the version. This requires we
			 * 	read data from our file.
			 */

			long fileOffset;
			if (version == 1) {
				/*
				 *	Each item is 'offset' (4 bytes) + 'sha1' (20 bytes)
				 */

				fileOffset = 4 + offsetStart + (mid * 24);
			} else {

				/*
				 *	Each item is 'sha1' (20 bytes)
				 */
				fileOffset = offsetStart + (mid * 20);
			}
			file.seek(fileOffset);
			file.read(sha1);
			int cmp = Hex.compare(sha1Bytes, sha1);

			if (cmp < 0) {
				max = mid;
			} else if (cmp > 0) {
				min = mid + 1;
			} else {
				return mid;
			}
		}
		return -1;
	}

	/**
	 * Version 1: The SHA1 items are stored in a singular table. SHA1 records
	 * are sorted in order, so we can do a binary search on the table. We peel
	 * off the first byte to form an index into our fanout table.
	 * @param sha1bytes The SHA-1 of the object to search for
	 * @return The record, or null if not found
	 */
	private Record getVersion1Record(byte[] sha1bytes) throws IOException
	{
		Bounds bounds = findSHA1Bounds(sha1bytes);
		int index = binarySearch(sha1bytes, bounds.min, bounds.max);
		if (index == -1) return null;

		/*
		 *	We found the index. Now we need to read the offset and SHA
		 * 	from the file.
		 */

		long fileOffset = offsetStart + ((long)index * 24);
		file.seek(fileOffset);
		byte[] sha1 = new byte[20];
		long offset = file.readInt();
		file.read(sha1);

		// Sanity check: make sure we read the right index
		if (!Hex.equals(sha1bytes, sha1)) {
			throw new IOException("SHA1 mismatch");
		}

		return new Record(sha1, offset, 0);
	}

	/**
	 * Version 2: The SHA1 items are stored in multiple tables. Get the index
	 * and load the elements from the various table entries
	 * @param sha1bytes The SHA-1 of the object to search for
	 * @return The record, or null if not found
	 */

	private Record getVersion2Record(byte[] sha1bytes) throws IOException
	{
		Bounds bounds = findSHA1Bounds(sha1bytes);
		int index = binarySearch(sha1bytes, bounds.min, bounds.max);
		if (index == -1) return null;

		/*
		 *	We found the index. Our elements are pulled from three separate
		 * 	tables.
		 */

		long fileOffset = offsetStart + (long)index * 20;
		file.seek(fileOffset);
		byte[] sha1 = new byte[20];
		file.read(sha1);

		fileOffset = offsetCRC + (long)index * 4;
		file.seek(fileOffset);
		int crc = file.readInt();

		fileOffset = offsetPack + (long)index * 4;
		file.seek(fileOffset);
		long offset = file.readInt();

		/*
		 *	If the MSB of fileOffset is set, this is an index into our 8-byte
		 * 	offset table
		 */

		if ((offset & 0x80000000) != 0) {
			fileOffset = offset64Pack + (long)(offset & 0x7FFFFFFF) * 8;
			file.seek(fileOffset);
			offset = file.readLong();
		}

		// Sanity check: make sure we read the right index
		if (!Hex.equals(sha1bytes, sha1)) {
			throw new IOException("SHA1 mismatch");
		}

		return new Record(sha1, offset, crc);
	}

	/**
	 * This dumps all of the object records in the index file for debugging
	 * purposes.
	 */
	public void dump() throws IOException
	{
		byte[] sha1 = new byte[20];

		if (version == 1) {
			System.out.println("Version 1:");

			file.seek(offsetStart);
			for (int i = 0; i < size; ++i) {
				int offset = file.readInt();
				file.read(sha1);

				System.out.println(String.format("%d: %s %d", i, Hex.toString(sha1), offset));
			}
		} else {
			System.out.println("Version 2:");

			for (int i = 0; i < size; ++i) {
				file.seek(offsetPack + (long)i * 4);
				int off = file.readInt();
				long offset;

				if (off < 0) {
					file.seek(offset64Pack + (long)(off & 0x7FFFFFFF) * 8);
					offset = file.readLong();
				} else {
					offset = off;
				}

				file.seek(offsetStart + (long)i * 20);
				file.read(sha1);
				System.out.println(String.format("%d: %s %d", i, Hex.toString(sha1), offset));
			}
		}
	}

	/**
	 * Like dump, but returns an array of all the entries
	 * @return
	 */
	public Record[] getTOC() throws IOException
	{
		Record[] ret = new Record[size];

		if (version == 1) {
			file.seek(offsetStart);

			for (int i = 0; i < size; ++i) {
				int offset = file.readInt();
				byte[] sha1 = new byte[20];
				file.read(sha1);
				ret[i] = new Record(sha1, offset, 0);
			}

		} else {
			file.seek(offsetStart);
			for (int i = 0; i < size; ++i) {
				byte[] sha1 = new byte[20];
				file.read(sha1);
				ret[i] = new Record(sha1, 0, 0);
			}
			for (int i = 0; i < size; ++i) {
				int crc = file.readInt();
				ret[i] = new Record(ret[i].sha1, 0, crc);
			}
			for (int i = 0; i < size; ++i) {
				int offset = file.readInt();
				ret[i] = new Record(ret[i].sha1, offset, ret[i].crc);
			}
			for (int i = 0; i < size; ++i) {
				long offset = ret[i].offset;
				if (offset < 0) {
					file.seek(offset64Pack + (long)(offset & 0x7FFFFFFF) * 8);
					offset = file.readLong();
					ret[i] = new Record(ret[i].sha1, offset, ret[i].crc);
				}
			}
		}

		return ret;
	}
}

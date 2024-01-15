package com.chaosinmotion.git.test.packfiles;

import com.chaosinmotion.git.test.common.ObjectType;
import com.chaosinmotion.git.test.common.ValidateResult;
import com.chaosinmotion.git.test.utils.Hex;
import com.chaosinmotion.git.test.utils.Stream;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Pack reader, reading the .pack file format
 *
 * https://git-scm.com/docs/pack-format
 * https://shafiul.github.io//gitbook/7_the_packfile.html
 *
 * https://git-scm.com/docs/gitformat-pack
 */

public class PackReader
{
	private RandomAccessFile file;
	private int version;
	private int objectCount;

	/**
	 * This is the representation of the header of the packed object within
	 * this file.
	 */
	public static class ObjectHeader
	{
		/// This is the type of the object stored in this packfile
		public final ObjectType type;

		/// This is the offset (in bytes) from the start of the file to
		/// the header location in this object
		public final long headerPos;

		/// This is the offset (in bytes) from the start of the file
		/// to where the data is just past the header
		public final long dataPos;

		/// This is the decompressed size of the zlib compressed block of
		/// data associated with this object. For delta objects that size
		/// does not include the object index or object reference.
		public final long size;

		ObjectHeader(ObjectType type, long hpos, long dpos, long size)
		{
			this.type = type;
			this.headerPos = hpos;
			this.dataPos = dpos;
			this.size = size;
		}
	}

	/**
	 * Opens the pack file for reading via random access
	 * @param f The file reference
	 * @throws IOException
	 */
	public PackReader(File f) throws IOException
	{
		file = new RandomAccessFile(f, "r");

		/*
		 *	Read the header
		 */

		int h = file.readInt();
		if (h != 0x5041434b) {			// PACK
			throw new IOException("Invalid PACK file header");
		}

		version = file.readInt();
		objectCount = file.readInt();

		if ((version < 2) || (version > 3)) {
			throw new IOException("Unsupported pack file version " + version);
		}
	}

	public void close() throws IOException
	{
		file.close();
	}


	/**
	 * Validates the checksum of this pack file. This will read the entire
	 * file and validate the overall checksum of the file.
	 * @param f The file pointer to the pack file
	 * @return The validation state
	 * @throws IOException
	 */
	public static ValidateResult validate(File f) throws IOException,
			NoSuchAlgorithmException
	{
		if (!f.exists()) return ValidateResult.NOT_FOUND;

		/*
		 *	Random access file
		 */
		RandomAccessFile pk = new RandomAccessFile(f, "r");

		/*
		 *	Read the SHA 1 checksum at the end
		 */

		long flen = pk.length()-20;
		if (flen < 0) return ValidateResult.INVALID;

		pk.seek(flen);
		byte[] sha1 = new byte[20];
		pk.read(sha1);

		/*
		 *	Now read the entire file, and compute the SHA 1 checksum
		 */

		pk.seek(0);

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] buffer = new byte[1024];
		long pos = 0;
		int len;

		while (pos < flen) {
			long rlen = flen - pos;
			if (rlen > buffer.length) rlen = buffer.length;

			len = pk.read(buffer, 0, (int)rlen);
			if (len <= 0) break;
			md.update(buffer, 0, len);

			pos += len;
		}

		byte[] digest = md.digest();
		if (!Hex.equals(sha1, digest)) {
			pk.close();
			return ValidateResult.INVALID;
		}

		return ValidateResult.VALID;
	}

	/**
	 * This only reads the header of the pack file.
	 * @param headerOffset The offset to the start of the header pulled from
	 *                     the pack file or the delta offset in the OFS_DATA
	 *                     object
	 * @return The header containing the parsed size and offset, as well as
	 * the offset to the start of the data and the offset to the start of the
	 * header.
	 * @throws IOException
	 */
	public ObjectHeader readObjectHeader(long headerOffset) throws IOException
	{
		int ch;
		int shift;
		byte type;
		long size = 0;

		/*
		 *	Seek to the start of the header. This contains the type and size of
		 * 	the uncompressed data associated with this
		 */
		file.seek(headerOffset);

		/*
		 *	Read the first byte
		 */

		ch = file.read();
		if (ch == -1) throw new IOException("Unexpected EOF");

		type = (byte)(0x07 & (ch >> 4));
		size = (ch & 0x0f);

		/*
		 *	Read the rest of the bytes
		 */

		shift = 4;
		while ((ch & 0x80) != 0) {
			ch = file.read();
			if (ch == -1) throw new IOException("Unexpected EOF");
			size |= (long)(ch & 0x7f) << shift;
			shift += 7;
		}

		long offset = file.getFilePointer();

		return new ObjectHeader(ObjectType.fromByte(type), headerOffset, offset, size);
	}

	/**
	 * This reads the data associated with the object header. If the data
	 * is compressed, the uncompressed data is written to the output stream.
	 * If this is a delta record type, the raw delta data is written. This
	 * isn't terribly useful.
	 * @param header The header of the object to read
	 * @param os The output stream to write the data to
	 * @throws IOException
	 */
	public void readObjectData(ObjectHeader header, OutputStream os) throws DataFormatException, IOException
	{
		/*
		 *	Seek to the offset
		 */

		file.seek(header.dataPos);

		/*
		 *	Track where we are, so we can use the inflater to backtrack the
		 * 	file pointer
		 */

		if (header.type == ObjectType.OFSDelta) {
			copyData(header.size,os);
		} else if (header.type == ObjectType.REFDelta) {
			copyData(header.size,os);
		} else {
			decompressData(os);
		}
	}

	/**
	 * If this is of type OFS_DELTA or REF_DELTA, this will read the delta
	 * data and return it. The delta data can be used to transform the specified
	 * file to the desired destination file.
	 * @param header The header of the object to read
	 * @return The delta data representation of the object
	 * @throws IOException
	 */
	public Delta readDeltaData(ObjectHeader header) throws IOException,
			DataFormatException
	{
		file.seek(header.dataPos);

		if (header.type == ObjectType.OFSDelta) {
			// The offset is given relative to the current object
			long offset = header.headerPos - Stream.readSizeEncoded(file);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			decompressData(baos);
			baos.close();
			return new Delta(offset, baos.toByteArray());

		} else if (header.type == ObjectType.REFDelta) {
			byte[] sha = new byte[20];
			if (sha.length != file.read(sha)) {
				throw new IOException("Unexpected EOF");
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			decompressData(baos);
			baos.close();
			return new Delta(sha, baos.toByteArray());

		} else {
			throw new IOException("Invalid delta type");
		}
	}

	private void copyData(long size, OutputStream os) throws IOException
	{
		byte[] buffer = new byte[1024];
		long pos = 0;
		long rlen;

		while (pos < size) {
			rlen = size - pos;
			if (rlen > buffer.length) rlen = buffer.length;

			int len = file.read(buffer, 0, (int)rlen);
			if (len <= 0) break;
			os.write(buffer, 0, len);

			pos += rlen;
		}
	}

	private void decompressData(OutputStream os) throws IOException,
			DataFormatException
	{
		int rlen;
		long curOffset = file.getFilePointer();

		/*
		 *	Now read the data. If this is a base object type, we inflate the
		 * 	contents from our object. Note that we use the inflater object
		 * 	explicitly so we can rewind our file pointer stream to the end
		 * 	of the data block we just read.
		 */

		Inflater inflater = new Inflater();
		byte[] inBuffer = new byte[1024];
		byte[] outBuffer = new byte[1024];
		boolean done = false;

		/*
		 *	Preload the inflater with the first block of data
		 */
		int len = file.read(inBuffer);
		if (len <= 0) {
			inflater.end();
			throw new IOException("Unexpected EOF");
		}
		;
		inflater.setInput(inBuffer, 0, len);

		/*
		 *	Now inflate the data
		 */
		while (!done) {
			/*
			 *	Keep decompressing until we run out of stuff to decomparess.
			 */
			while (0 < (rlen = inflater.inflate(outBuffer))) {
				os.write(outBuffer, 0, rlen);
				if (inflater.finished()) {
					done = true;
					break;
				}
			}

			if (inflater.needsDictionary()) {
				inflater.end();
				throw new IOException("ZLib dictionary unexpectedly asked for in header");
			}

			if (inflater.needsInput()) {
				/*
				 *	Inflater requested more data.
				 */
				len = file.read(inBuffer);
				if (len <= 0) {
					inflater.end();
					throw new IOException("Unexpected EOF");
				};
				inflater.setInput(inBuffer, 0, len);
			}
		}

		/*
		 *	Now at this point we can interrogate the inflater to know how many
		 * 	bytes we read in total. This tells us the proper offset into our
		 * 	file.
		 */

		long bytesRead = inflater.getBytesRead();
		file.seek(curOffset + bytesRead);
	}
}

package com.chaosinmotion.git.test.packfiles;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;

/**
 * This contains code for parsing the multi-pack-index file. The
 * multi-pack-index helps speed up locating objects within a collection
 * of pack files. Support for this was added in GIT 2.21.
 *
 * Design notes: https://web.mit.edu/git/builds/git-v2.42.1/common/share/doc/git/technical/multi-pack-index.html
 */
public class MultiPackIndexReader
{
	/**
	 * Chunk format, from gitformat-chunk.txt
	 */
	private static class Chunk
	{
		private int id;
		private long offset;

		Chunk(int id, long offset)
		{
			this.id = id;
			this.offset = offset;
		}
	}

	private byte version;
	private byte objectID;
	private int numChunks;		// stored as a byte
	private int baseFiles;		// Currently always zero, stored as a byte
	private int numPackFiles;	// stored as a 32-bit integer
	private Chunk[] chunks;

	public MultiPackIndexReader(InputStream is) throws IOException
	{
		// TODO: Rewrite as memory mapped MappedByteBuffer
		// https://howtodoinjava.com/java/nio/memory-mapped-files-mappedbytebuffer/
		/*
		 *	Simplify reading network order integers
		 */
		DataInputStream dis = new DataInputStream(is);
		MappedByteBuffer mbb = null;

		/*
		 *	Read the header. (gitformat-pack.txt)
		 */

		int sig = dis.readInt();

		// See if 'MIDX'
		if (sig != 0x4D494458) throw new IOException("Invalid multi-pack-index signature");
		if (1 != (version = dis.readByte())) throw new IOException("Invalid multi-pack-index version");
		objectID = dis.readByte();
		if ((objectID < 1) || (objectID > 2)) throw new IOException("Invalid object ID version");
		numChunks = (int)(0xFF & dis.readByte());
		baseFiles = (int)(0xFF & dis.readByte());
		if (baseFiles != 0) throw new IOException("Unsupported object file count");
		numPackFiles = dis.readInt();

		/*
		 *	Read the chunks
		 */

		chunks = new Chunk[numChunks+1];
		for (int i = 0; i < numChunks; i++) {
			int id = dis.readInt();
			long offset = dis.readLong();
			chunks[i] = new Chunk(id, offset);
		}
		if (chunks[numChunks].id != 0) {
			// gitformat-chunk: last chunk should have ID == 0
			throw new IOException("Invalid chunk array ID");
		}
	}
}

package com.chaosinmotion.git.test.objects;

import com.chaosinmotion.git.test.common.ObjectType;
import com.chaosinmotion.git.test.common.ValidateResult;
import com.chaosinmotion.git.test.utils.Hex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.InflaterInputStream;

// https://stackoverflow.com/questions/22968856/what-is-the-file-format-of-a-git-commit-object-data-structure
// https://stackoverflow.com/questions/10986615/what-is-the-format-of-a-git-tag-object-and-how-to-calculate-its-sha/52193441#52193441
// https://git-scm.com/book/en/v2/Git-Internals-Git-Objects
// https://git-scm.com/book/en/v2/Git-Internals-Plumbing-and-Porcelain

// https://github.com/git/git/blob/011b648646fcf1f467336ac6bbf46145501c0f12/Documentation/technical/pack-format.txt#L39-L129
// https://github.com/git/git/tree/master/Documentation/gitformat-*.txt

/**
 * <p>This reads an object from the .git/objects directory. When supplied with
 * the SHA1 hash of the object, it will read the object from the file system
 * and return the object.</p>
 */
public class ObjectReader extends InputStream
{
	private InflaterInputStream iis;

	/*
	 *	Every object stored by GIT starts with a header:
	 *
	 * 	(type)(space)(length)(null)
	 *
	 * 	We parse this as we open the object.
	 */
	private ObjectType type;
	private long length;
	private long pos;

	/**
	 * Reads an object from the .git/objects directory.
	 * @param file The file to read. This can be generated using the
	 *             findFile method.
	 */
	public ObjectReader(File file) throws IOException
	{
		// Reset file position
		pos = 0;

		// Open our input stream.
		iis = new InflaterInputStream(new FileInputStream(file));

		// Now read octets until we hit a space. This is the object type.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int ch;
		while (-1 != (ch = read())) {
			if (ch == ' ') break;
			baos.write(ch);
		}
		String t = baos.toString(StandardCharsets.UTF_8);	// always assume UTF-8
		type = ObjectType.fromString(t);
		if (type == ObjectType.UNKNOWN) {
			throw new IOException("Invalid Object Type");
		}

		// Now read the octets until we hit a 0 byte
		// When done this puts us at the start of the file
		baos.reset();
		while (-1 != (ch = read())) {
			if (ch == 0) break;
			baos.write(ch);
		}

		try {
			length = Long.parseLong(baos.toString(StandardCharsets.UTF_8));
		}
		catch (NumberFormatException nfe) {
			throw new IOException("Invalid Object Length");
		}
		if (ch == -1) throw new IOException("Unexpected EOF");
	}

	/**
	 * Finds the file for the given SHA1 hash within the given GIT repository.
	 * This does the work of converting the SHA1 hash into a file name, without
	 * regards to whether the file actually exists.
	 *
	 * See https://alblue.bandlem.com/2011/08/git-tip-of-week-objects.html
	 *
	 * @param gitRoot the root of the git repository
	 * @param sha the SHA1 hash of the object to read
	 * @return the file for the given SHA1 hash
	 */
	public static File findFile(File gitRoot, String sha)
	{
		File scratch = new File(gitRoot,".git");
		scratch = new File(scratch,"objects");
		return findFileInObjectDirectory(scratch, sha);
	}

	/**
	 * This finds the file for the given SHA1 hash within the given GIT
	 * object directory. That is, we assume the objDir file path ends with
	 * '.git/objects'.
	 * @param objDir
	 * @param sha
	 * @return
	 */
	public static File findFileInObjectDirectory(File objDir, String sha)
	{
		// Make sure the file names are lower case for case sensitive file systems
		String prefix = sha.substring(0,2).toLowerCase();
		String suffix = sha.substring(2).toLowerCase();

		File scratch = new File(objDir,prefix);
		return new File(scratch,suffix);
	}

	/**
	 * The SHA1 signature of an object file must match the name of the object
	 * file. This decompresses the object file, computes the SHA1 hash and
	 * compares it to the name of the file.
	 * @param gitRoot The root GIT directory in which to look for this file
	 * @param sha The name of the file as an SHA1 hash
	 * @return This returns if the file is valid, invalid, or not present in
	 * the GIT repository.
	 */
	public static ValidateResult validateFile(File gitRoot, String sha) throws
			IOException, NoSuchAlgorithmException
	{
		File f = findFile(gitRoot, sha);
		if (!f.exists() || !f.isFile()) return ValidateResult.NOT_FOUND;

		InflaterInputStream iis = new InflaterInputStream(new FileInputStream(f));

		/*
		 *	Decompress and calculate the SHA1 hash of this file. Note that the
		 * 	SHA-1 hash is calculated on the uncompressed data, including the
		 * 	header, and not on the compressed data.
		 */

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] buffer = new byte[1024];
		int len;

		while ((len = iis.read(buffer)) > 0) {
			md.update(buffer, 0, len);
		}

		byte[] digest = md.digest();
		String digestString = Hex.toString(digest);
		if (sha.equalsIgnoreCase(digestString)) {
			return ValidateResult.VALID;
		} else {
			return ValidateResult.INVALID;
		}
	}

	/**
	 * 	Stupid test routine to dump the contents in hex
	 */

	public void dump() throws IOException
	{
		byte[] buffer = iis.readAllBytes();
		Hex.dump(buffer, 0, buffer.length);
		iis.close();
	}

	/**
	 * <p>The type of the object read from the header. Thus far in the wild we
	 * have seen the following types:</p>
	 * <ul>
	 *     <li><b>blob</b> stores the raw contents of a file</li>
	 *     <li><b>commit</b> stores the commit records for our system</li>
	 *     <li><b>tree</b> stores the directory structure of our commits</li>
	 * </ul>
	 * <p>Elsewhere in our system we also see the following types</p>
	 * <ul>
	 *     <li><b>tag</b> stores the tags associated with an object</li>
	 * </ul>
	 * <p>And inside packed files we may also see the following headers,
	 * though they will never be returned here:</p>
	 * <ul>
	 *     <li><b>ofs_delta</b> stored in a packed file, delta data</li>
	 *     <li><b>ref_delta</b> stored in a packed file, delta data</li>
	 * </ul>
	 * <p>The later delta types are stored only inside a packed format object
	 * and may not be returned when reading a top-level object file.</p>
	 * <p>See <a href="https://github.com/git/git/blob/011b648646fcf1f467336ac6bbf46145501c0f12/Documentation/technical/pack-format.txt">here</a>
	 * for more details.</a></p>
	 *
	 * <p>Note we provide parsers for trees, commits and tags, but not for
	 * blobs. Blobs can be read directly from the object reader input stream.</p>
	 *
	 * @return The type of this object
	 */

	public ObjectType getType()
	{
		return type;
	}

	/**
	 * The length of the object. This should be the total length of the
	 * uncompressed file
	 * @return The length of this object
	 */

	public long getLength()
	{
		return length;
	}

	/**
	 * Gets the offset into the file where we are currently reading. When we
	 * hit the EOF this should match the length
	 * @return
	 */
	public long getPos()
	{
		return pos;
	}

	/*
	 *	Pass through to our underlying input stream for reading.
	 */

	@Override
	public int read() throws IOException
	{
		int ret = iis.read();
		if (ret != -1) ++pos;
		return ret;
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		int ret = iis.read(b);
		if (ret != -1) pos += ret;
		return ret;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		int ret = iis.read(b, off, len);
		if (ret != -1) pos += ret;
		return ret;
	}

	@Override
	public void close() throws IOException
	{
		iis.close();
	}
}

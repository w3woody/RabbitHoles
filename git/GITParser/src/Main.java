import com.chaosinmotion.git.test.common.ObjectType;
import com.chaosinmotion.git.test.objects.CommitReader;
import com.chaosinmotion.git.test.objects.ObjectReader;
import com.chaosinmotion.git.test.objects.TagReader;
import com.chaosinmotion.git.test.objects.TreeReader;
import com.chaosinmotion.git.test.packfiles.Delta;
import com.chaosinmotion.git.test.packfiles.IndexReader;
import com.chaosinmotion.git.test.packfiles.PackReader;
import com.chaosinmotion.git.test.protocol.TestProtocol;
import com.chaosinmotion.git.test.utils.Hex;
import com.chaosinmotion.git.test.utils.RandomAccessByteArray;
import com.chaosinmotion.git.test.utils.Stream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.DataFormatException;

/**
 * This contains examples of the various parser objects in action. This uses
 * the two test directories under the test folder. The first one contains the
 * objects from a git repository; the second is the same repository but
 * compressed using `git gc --aggressive`, which put all of the objects into
 * a single pack file.
 */

public class Main
{
	/**
	 * This is a stupid utility class which prints a header block that
	 * separates major parts of our output stream.
	 * @param str The remark to throw into the block
	 */
	private static void printHeader(String str)
	{
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < 80; ++i) {
			b.append('*');
		}
		b.append('\n');

		b.append('*');
		for (int i = 0; i < 78; ++i) {
			b.append(' ');
		}
		b.append('*');

		b.append('\n');

		b.append("*   " + str);
		int remain = 75 - str.length();
		for (int i = 0; i < remain; ++i) {
			b.append(' ');
		}
		b.append('*');

		b.append('\n');

		b.append('*');
		for (int i = 0; i < 78; ++i) {
			b.append(' ');
		}
		b.append('*');
		b.append('\n');

		for (int i = 0; i < 80; ++i) {
			b.append('*');
		}

		b.append('\n');

		System.out.println(b);
	}

	/**
	 * This dumps the contents of the tree at the specified point. Note this
	 * gets called with all the tree objects we encounter, but we ignore
	 * anything that is not a tree object.
	 *
	 * @param level How far down the tree structure we are. Used to adjust
	 *              formatting of the output
	 * @param tree The tree SHA-1 to visit
	 * @param visited If we have already visited this level
	 */
	private static void dumpTree(File objRoot, int level, String tree, HashSet<String> visited) throws
			IOException
	{
		// Open the tree object specified and parse the contents
		File obj = ObjectReader.findFileInObjectDirectory(objRoot,tree);
		if (!obj.exists()) {
			System.out.println("### Object " + tree + " not found");
			return;
		}
		ObjectReader reader = new ObjectReader(obj);

		// Ignore if this is not actually a tree object
		if (reader.getType() != ObjectType.TREE) {
			reader.close();
			return;
		}

		// Verify we haven't been to this tree before
		if (visited.contains(tree)) {
			System.out.println("### Object " + tree + " already visited");
			return;
		}
		visited.add(tree);

		// Parse the tree
		TreeReader t = new TreeReader(reader);

		// Dump the contents, recursing down as needed
		TreeReader.Record r;
		while (null != (r = t.read())) {
			// Print the level depth
			for (int i = 0; i < level; i++) System.out.print("  ");

			// Print the contents at this row
			String sha = Hex.toString(r.sha1);
			System.out.printf("%-8s %s    %s\n", r.mode,sha,r.name);

			dumpTree(objRoot, level + 1, sha, visited);
		}
		reader.close();
	}

	/**
	 * <p>Given the SHA1 hash of a commit object, this finds the commit object,
	 * opens it, dumps the header information, then walks the tree objects
	 * representing the files within the commit.</p>
	 *
	 * <p>This demonstrates the object reader, as well as parsing commit
	 * objects and tree objects</p>
	 *
	 * @param sha1
	 */
	public static void test1(String sha1) throws IOException
	{
		/*
		 *	Step 1: Open the commit object for the SHA-1 hash provided.
		 * 	If this is not a commit object, bail.
		 */

		// We assume CWD is set to the root directory of our test objects,
		// which on IntelliJ is the root of the project.
		File path = new File("test/Test1/objects");
		File obj = ObjectReader.findFileInObjectDirectory(path,sha1);
		if (!obj.exists()) {
			System.err.println("Object not found: " + sha1);
			System.err.println("Is the current working directory set correctly?");
			System.err.println();
			return;
		}
		ObjectReader reader = new ObjectReader(obj);
		if (reader.getType() != ObjectType.COMMIT) {
			reader.close();
			System.err.println("Object is not a commit object: " + sha1);
			return;
		}

		/*
		 *	Step 2: parse and dump the commit data
		 */

		CommitReader cr = new CommitReader(reader);
		System.out.println("Commit: " + sha1);
		System.out.println("    Tree: " + cr.tree);
		for (String p: cr.parent) {
			System.out.println("    Parent: " + p);
		}
		System.out.println("    Author: " + cr.author.toString());
		System.out.println("    Committer: " + cr.committer.toString());
		System.out.println();
		reader.close();

		/*
		 *	Step 3: Walk the tree objects. Note this is a recursive descent
		 * 	through our tree objects. We also track the SHA of the tree objects
		 * 	we descend to make sure there are no circular dependencies.
		 *
		 * 	(This is done out of an abundance of caution.)
		 */

		HashSet<String> visited = new HashSet<String>();
		dumpTree(path,0,cr.tree,visited);
		System.out.println();
		System.out.println();
	}

	/**
	 * This dumps the contents of a blob. We assume the contents of the blob
	 * is a UTF-8 file. (This is not always the case, but in our test
	 * objects, it is.)
	 * @param sha1 The blob object to dump
	 */
	public static void test3(String sha1) throws IOException
	{
		File path = new File("test/Test1/objects");
		File obj = ObjectReader.findFileInObjectDirectory(path,sha1);
		if (!obj.exists()) {
			System.err.println("Object not found: " + sha1);
			System.err.println("Is the current working directory set correctly?");
			System.err.println();
			return;
		}
		ObjectReader reader = new ObjectReader(obj);
		if (reader.getType() != ObjectType.BLOB) {
			reader.close();
			System.err.println("Object is not a commit object: " + sha1);
			return;
		}

		/*
		 *	Dump the contents
		 */

		byte[] contents = reader.readAllBytes();
		reader.close();

		System.out.println("Blob: " + sha1);
		System.out.println("-------------------------------");
		System.out.println(new String(contents, StandardCharsets.UTF_8));
		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * This tests the tag functionality by loading the specified tag and
	 * dumping it, then dumping the associated object.
	 * @param sha1 The tag SHA1
	 */
	public static void test2(String sha1) throws IOException
	{
		File path = new File("test/Test1/objects");
		File obj = ObjectReader.findFileInObjectDirectory(path,sha1);
		if (!obj.exists()) {
			System.err.println("Object not found: " + sha1);
			System.err.println("Is the current working directory set correctly?");
			System.err.println();
			return;
		}
		ObjectReader reader = new ObjectReader(obj);
		if (reader.getType() != ObjectType.TAG) {
			reader.close();
			System.err.println("Object is not a commit object: " + sha1);
			return;
		}

		TagReader tag = new TagReader(reader);
		reader.close();

		System.out.println("Tag: " + sha1);
		System.out.println("    Object: " + tag.object);
		System.out.println("    Type: " + tag.type);
		System.out.println("    Tag: " + tag.tag);
		System.out.println("    Tagger: " + tag.tagger);
		System.out.println();

		/*
		 *	Based on the type, dump the contents
		 */

		if (tag.type.equals("commit")) {
			test1(tag.object);
		} else if (tag.type.equals("tree")) {
			dumpTree(path,0,tag.object,new HashSet<String>());
		} else if (tag.type.equals("tag")) {
			// Weird, but what can you say?
			test2(tag.object);
		} else if (tag.type.equals("blob")) {
			// Dump blob
			test3(tag.object);
		} else {
			System.out.println("Unknown type: " + tag.type);
		}
	}

	/**
	 * Simple utility to check if a letter is a hex digit
	 * @param ch The character to check
	 * @return True if a hex digit
	 */
	private static boolean isHex(char ch)
	{
		if ((ch >= 'a') && (ch <= 'f')) return true;
		if ((ch >= 'A') && (ch <= 'F')) return true;
		if ((ch >= '0') && (ch <= '9')) return true;
		return false;
	}

	/**
	 * This iterates through all the headers in our test directory, opening and
	 * dumping the headers. This produces a flat list of all the object SHA
	 * values and their type. This does not recurse into idx or pack files
	 */
	public static void dumpAllHeaders()
	{
		File path = new File("test/Test1/objects");
		File[] dirs = path.listFiles(pathname -> {
			String fname = pathname.getName();
			if (fname.length() != 2) return false;		// only the 2 character names
			if (!isHex(fname.charAt(0)) || !isHex(fname.charAt(1))) return false;
			return true;
		});
		Arrays.sort(dirs, (o1, o2) -> o1.getName().compareTo(o2.getName()));

		for (File dir: dirs) {
			File[] files = dir.listFiles();
			Arrays.sort(files, (o1, o2) -> o1.getName().compareTo(o2.getName()));

			for (File file: files) {
				if (file.getName().startsWith(".")) continue;
				if (file.getName().endsWith(".idx")) continue;
				if (file.getName().endsWith(".pack")) continue;
				try {
					ObjectReader reader = new ObjectReader(file);
					System.out.println(dir.getName() + file.getName() + ": " + reader.getType());
					reader.close();
				}
				catch (Throwable th) {
					th.printStackTrace(System.err);
				}
			}
		}
	}

	/**
	 * This dumps the current type to the display, and if it is a delta type,
	 * finds and dumps the base type.
	 * @param depth
	 * @param h
	 * @param ir
	 * @param pr
	 */
	private static void findDeltaBase(int depth, PackReader.ObjectHeader h, IndexReader ir, PackReader pr) throws
			DataFormatException, IOException
	{
		Delta d = pr.readDeltaData(h);
		long offset;

		for (int i = 0; i < depth; ++i) System.out.print("  ");
		if (d.sha == null) {
			System.out.print("Delta Offset: " + d.offset);

			offset = d.offset;
		} else {
			System.out.print("Delta SHA: " + Hex.toString(d.sha));

			IndexReader.Record r = ir.getRecord(d.sha);
			offset = r.offset;
		}

		/*
		 *	Now load the record at this location, and recurse until we find
		 * 	the bottom.
		 */

		PackReader.ObjectHeader oh = pr.readObjectHeader(offset);
		System.out.println("  " + oh.type);
		if ((oh.type == ObjectType.OFSDelta) || (oh.type == ObjectType.REFDelta)) {
			findDeltaBase(depth+1,oh,ir,pr);
		}
	}

	/**
	 * Given the file reference for an index file, this opens the corresponding
	 * pack file and dumps all the objects and their types.
	 * @param indexFile The .idx file to dump the contents of
	 */
	public static void dumpIndex(File indexFile) throws IOException,
			DataFormatException
	{
		IndexReader ir = new IndexReader(indexFile);
		IndexReader.Record[] r = ir.getTOC();

		File packFile = new File(indexFile.getParentFile(),indexFile.getName().replace(".idx",".pack"));
		PackReader pr = new PackReader(packFile);

		for (IndexReader.Record rec: r) {
			PackReader.ObjectHeader oh = pr.readObjectHeader(rec.offset);
			System.out.printf("%10d - ",oh.headerPos);
			System.out.println(Hex.toString(rec.sha1) + ": " + oh.type);

			if ((oh.type == ObjectType.OFSDelta) || (oh.type == ObjectType.REFDelta)) {
				findDeltaBase(1,oh,ir,pr);
			}
		}

		pr.close();
		ir.close();
	}

	/**
	 * This looks up the SHA1 object in the index file, and if it is not a
	 * delta file, loads the decompressed data associated with this object into
	 * a byte array and returns it. This is a helper we use for testting both
	 * dumping object files and processing delta files.
	 *
	 * Note: If this is a delta file this throws an exception.
	 *
	 * @param indexFile The index file
	 * @param sha1
	 * @return
	 */
	private static byte[] getDataFromPackFile(File indexFile, String sha1) throws
			IOException, DataFormatException
	{
		IndexReader ir = new IndexReader(indexFile);
		File packFile = new File(indexFile.getParentFile(),indexFile.getName().replace(".idx",".pack"));
		PackReader pr = new PackReader(packFile);

		IndexReader.Record rec = ir.getRecord(sha1);
		if (rec == null) throw new IOException("Unkown SHA1 file " + sha1);

		// The destination we're writing the contents to. This is an in-mmeory
		// byte array, though this could be a scratch file.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PackReader.ObjectHeader h = pr.readObjectHeader(rec.offset);
		if ((h.type == ObjectType.OFSDelta) || (h.type == ObjectType.REFDelta)) {
			throw new IOException("This method does not handle deltas");
		}
		pr.readObjectData(h,baos);

		pr.close();
		ir.close();
		baos.close();

		return baos.toByteArray();
	}

	/**
	 *	This looks up the SHA1 object, and dumps the contents, assuming it is
	 * 	a blob file. This is like test3, but uses the index file. Note we DO NOT
	 * 	show expanding delta files.
	 */
	private static void test4(File indexFile, String sha1) throws
			DataFormatException, IOException
	{
		byte[] data = getDataFromPackFile(indexFile,sha1);

		System.out.println("Blob: " + sha1);
		System.out.println("-------------------------------");
		System.out.println(new String(data, StandardCharsets.UTF_8));
		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * This recursively constructs the file from the delta data. Unlike
	 * getDataFromPackFile, this understands delta files and resolves them.
	 * If the data provided is a blob, this simply returns that blob.
	 * Otherwise this recurses down to the base object, then applies the
	 * delta to it.
	 *
	 * @param indexFile
	 * @param sha1
	 *
	 * @return
	 */
	private static byte[] resolveDelta(File indexFile, String sha1) throws
			IOException, DataFormatException
	{
		IndexReader ir = new IndexReader(indexFile);
		File packFile = new File(indexFile.getParentFile(),indexFile.getName().replace(".idx",".pack"));
		PackReader pr = new PackReader(packFile);

		IndexReader.Record rec = ir.getRecord(sha1);
		if (rec == null) throw new IOException("Unkown SHA1 file " + sha1);

		// The destination we're writing the contents to. This is an in-mmeory
		// byte array, though this could be a scratch file.
		byte[] ret = internalResolveDelta(ir,pr,rec.offset);

		pr.close();
		ir.close();

		return ret;
	}

	/**
	 * This internal routine does the heavy lifting of resolving a delta file.
	 * This takes an index reader, a pack reader, and an offset to a packed object
	 * header, and loads the header. If this is a delta object, this recurses
	 * to get the base object this is based on, and applies the deltas to
	 * it.
	 * @param ir
	 * @param pr
	 * @param offset
	 * @return
	 */
	private static byte[] internalResolveDelta(IndexReader ir, PackReader pr, long offset) throws
			IOException, DataFormatException
	{
		PackReader.ObjectHeader h = pr.readObjectHeader(offset);
		if ((h.type == ObjectType.OFSDelta) || (h.type == ObjectType.REFDelta)) {
			// This is delta data. First, get the delta data
			Delta d = pr.readDeltaData(h);

			// Next, get the base data object for this delta
			long baseOffset;
			if (d.sha != null) {
				IndexReader.Record r = ir.getRecord(d.sha);
				baseOffset = r.offset;
			} else {
				baseOffset = d.offset;
			}
			byte[] base = internalResolveDelta(ir,pr,baseOffset);

			if (base.length != d.baseSize) {
				throw new IOException("Base size mismatch");
			}

			// Now apply the delta to the base data
			RandomAccessByteArray raba = new RandomAccessByteArray(base);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			d.apply(raba,baos);
			raba.close();
			baos.close();

			byte[] result = baos.toByteArray();

			if (result.length != d.resultSize) {
				throw new IOException("Result size mismatch");
			}

			return result;

		} else {
			// This is blob data. Just read and return the data
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			pr.readObjectData(h,baos);
			baos.close();
			return baos.toByteArray();
		}
	}

	/**
	 * This gets the object file, resolving it (in memmory) if this is a delta
	 * file object.
	 *
	 * Though we don't show it, the type of this object is the type of the base
	 * object that we resolve.
	 *
	 * @param indexFile
	 * @param sha1
	 * @throws DataFormatException
	 * @throws IOException
	 */
	public static void test6(File indexFile, String sha1) throws DataFormatException,
			IOException
	{
		byte[] data = resolveDelta(indexFile,sha1);

		System.out.println("Blob: " + sha1);
		System.out.println("-------------------------------");
		System.out.println(new String(data, StandardCharsets.UTF_8));
		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * This dumps the delta information associated with this packed delta object
	 * @param indexFile
	 * @param sha1
	 * @throws DataFormatException
	 * @throws IOException
	 */
	public static void test5(File indexFile, String sha1) throws DataFormatException,
			IOException
	{
		IndexReader ir = new IndexReader(indexFile);
		File packFile = new File(indexFile.getParentFile(),indexFile.getName().replace(".idx",".pack"));
		PackReader pr = new PackReader(packFile);

		IndexReader.Record rec = ir.getRecord(sha1);
		if (rec == null) throw new IOException("Unkown SHA1 file " + sha1);

		// The destination we're writing the contents to. This is an in-mmeory
		// byte array, though this could be a scratch file.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PackReader.ObjectHeader h = pr.readObjectHeader(rec.offset);
		if ((h.type != ObjectType.OFSDelta) && (h.type != ObjectType.REFDelta)) {
			throw new IOException("Not a delta file");
		}

		Delta d = pr.readDeltaData(h);
		d.dump();
	}

	public static void main(String[] args)
	{
		try {
//			Stream.testEncoding();
//			TestProtocol.test1();
			TestProtocol.test2();

//			printHeader("Starting test group 1 with unpacked objects");
//
//			/*
//			 *	Dump all the objects within our test1 directory. Our test1
//			 * 	directory is an example of a GIT repository without any packed
//			 */
//			System.out.println("Dump all object headers");
//			dumpAllHeaders();
//
//			/*
//			 *	Our GIT example contains two commits. The first commit has the
//			 *  SHA-1 hash of 073f88d50901c9aa8a9a75df4f7b05145e6cf546
//			 * 	The second 71b71187fe2ac3088cb90096206a12f1601ae981
//			 *
//			 * 	This reads the files out of the first test directory, where the
//			 * 	objects are stored as bare object files rather than in a pack
//			 * 	directory.
//			 */
//
//			System.out.println();
//			System.out.println("Starting Test 1");
//			test1("073f88d50901c9aa8a9a75df4f7b05145e6cf546");
//			test1("71b71187fe2ac3088cb90096206a12f1601ae981");
//
//			/*
//			 *	This does the same thing but with a tag. Our tag identifies
//			 *  the first commit.
//			 */
//
//			System.out.println();
//			System.out.println("Starting Test 2");
//			test2("db68cde733462c0818c271653332906164881bc1");
//
//			/*
//			 *	This dumps a couple of blobs.
//			 */
//
//			System.out.println("Starting Test 3");
//			System.out.println("Blob for README.md");
//			test3("e4c018ddf45cc7bed5f598c48c6668ed88745a2b");
//			System.out.println("Blob for .gitignore");
//			test3("f35faf4a85c3ba1e722404f1309f5de4f78b0e76");
//			System.out.println("Blob for main.c (version 1)");
//			test3("bfb1b107bab520e424acf46c3dbfec7e2dd043f1");
//
//			/*
//			 *	Test 2 with our index files
//			 */
//			System.out.println();
//			System.out.println();
//			printHeader("Starting test group 2 with packed objects");
//
//			System.out.println("Dump all index file objects");
//			File indexFile = new File("test/Test2/objects/pack/pack-9f3aff5a53ee278cbbf7900ae45eb601d53374d6.idx");
//			dumpIndex(indexFile);
//
//			/*
//			 *	Dump a blob from our pack file.
//			 */
//
//			System.out.println("Blob for README.md");
//			test4(indexFile, "e4c018ddf45cc7bed5f598c48c6668ed88745a2b");
//			System.out.println("Blob for .gitignore");
//			test4(indexFile, "f35faf4a85c3ba1e722404f1309f5de4f78b0e76");
//
//			/*
//			 *	Now resolve the data stored as a delta data.
//			 */
//
//			test5(indexFile,"bfb1b107bab520e424acf46c3dbfec7e2dd043f1");
//			System.out.println();
//			test6(indexFile,"bfb1b107bab520e424acf46c3dbfec7e2dd043f1");
		}
		catch (Throwable err)
		{
			err.printStackTrace(System.err);
		}
	}
}
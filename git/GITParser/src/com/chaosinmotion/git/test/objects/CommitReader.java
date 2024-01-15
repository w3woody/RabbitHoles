package com.chaosinmotion.git.test.objects;

import com.chaosinmotion.git.test.common.User;
import com.chaosinmotion.git.test.utils.ReadHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The commit reader takes the contents of a commit object file (that is, the
 * data after the header, returned by an ObjectReader), and reads out the
 * commit information.
 *
 * The commit message is stored as a series of text lines with a header, and
 * an arbitrary commit message. The header contains lines starting with
 *
 * 	tree {tree_sha}
 * 	parent {parent_sha}
 * 	author {name} <{email}> {timestamp} {timezone}
 * 	committer {name} <{email}> {timestamp} {timezone}
 *  \n
 *  {commit message}
 *
 *  Note that the parent line is optional and may appear more than once.
 *
 *  The format is described here:
 *
 *  https://stackoverflow.com/questions/22968856/what-is-the-file-format-of-a-git-commit-object-data-structure
 */

/*
 *	TODO: Following lines with a single space starting merge with the prior
 *  line.
 *
 *  TODO: 'mergetag' line headers?
 *  TODO: 'gpgsig' line header
 *
 */
public class CommitReader
{
	public final String tree;
	public final String[] parent;
	public final User author;
	public final User committer;
	public final HashMap<String,String> otherKeys;
	public final String commitMessage;

	public CommitReader(InputStream is) throws IOException
	{
		ReadHeaders r = new ReadHeaders(is);
		ReadHeaders.Header h;

		String t = null;
		User a = null;
		User c = null;

		HashMap<String,String> k = new HashMap<>();

		ArrayList<String> plist = new ArrayList<>();
		while (null != (h = r.readHeader())) {
			switch (h.name) {
				case "tree":
					t = h.value;
					break;
				case "parent":
					plist.add(h.value);
					break;
				case "author":
					a = new User(h.value);
					break;
				case "committer":
					c = new User(h.value);
					break;
				default:
					k.put(h.name, h.value);
					break;
			}
		}

		/*
		 *	Populate results
		 */

		tree = t;
		author = a;
		committer = c;
		commitMessage = r.readContent();
		parent = plist.toArray(new String[plist.size()]);
		otherKeys = k;
	}
}

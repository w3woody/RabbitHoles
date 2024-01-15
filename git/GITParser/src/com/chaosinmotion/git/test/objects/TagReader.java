package com.chaosinmotion.git.test.objects;

import com.chaosinmotion.git.test.common.User;
import com.chaosinmotion.git.test.utils.ReadHeaders;

import java.io.IOException;
import java.io.InputStream;

/**
 * The tag reader takes the contents of a tag object file (that is, the data
 * after the header, returned by an ObjectReader), and reads out the tags
 * in the file.
 *
 * Each tag is stored with the format:
 *
 * object {sha}
 * type {type}
 * tag {name}
 * tagger {name} <{email}> {timestamp} {timezone}
 * \n
 * {tag message}
 *
 * see https://stackoverflow.com/questions/10986615/what-is-the-format-of-a-git-tag-object-and-how-to-calculate-its-sha
 *
 *
 */
public class TagReader
{
	public final String object;
	public final String type;
	public final String tag;
	public final User tagger;
	public final String tagMessage;

	public TagReader(InputStream is) throws IOException
	{
		ReadHeaders r = new ReadHeaders(is);
		ReadHeaders.Header h;

		String o = null;
		String t = null;
		String tg = null;
		User tgr = null;
		String tm = null;

		while (null != (h = r.readHeader())) {
			switch (h.name) {
				case "object":
					o = h.value;
					break;
				case "type":
					t = h.value;
					break;
				case "tag":
					tg = h.value;
					break;
				case "tagger":
					tgr = new User(h.value);
					break;
				default:
					break;
			}
		}

		/*
		 *	Populate the restuls
		 */

		object = o;
		type = t;
		tag = tg;
		tagger = tgr;
		tagMessage = r.readContent();
	}
}

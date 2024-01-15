package com.chaosinmotion.git.test.common;

/**
 * A common class representing a person (author, committer, or tagger) in the
 * commit and tag files.
 *
 * This contains the code necessary to parse these lines.
 */
public class User
{
	public final String name;
	public final String email;
	public final long timestamp;
	public final String timezone;

	/*
	 *	Extract common contents of author and committer lines
	 */
	public User(String line)
	{
		char ch;
		StringBuilder b = new StringBuilder();

		int i = 0, len = line.length();
		while (i < len) {
			ch = line.charAt(i++);
			if (ch == '<') break;
			b.append(ch);
		}
		name = b.toString().trim();

		b.setLength(0);
		while (i < len) {
			ch = line.charAt(i++);
			if (ch == '>') break;
			b.append(ch);
		}
		email = b.toString().trim();

		if (i < len) ++i;            // skip whitespace after closing bracket

		b.setLength(0);
		while (i < len) {
			ch = line.charAt(i++);
			if (ch == ' ') break;
			b.append(ch);
		}
		timestamp = Long.parseLong(b.toString().trim());

		b.setLength(0);
		while (i < len) {
			ch = line.charAt(i++);
			if (ch == '\n') break;
			b.append(ch);
		}
		timezone = b.toString().trim();
	}

	public String toString()
	{
		return String.format("%s <%s>", name, email);
	}
}

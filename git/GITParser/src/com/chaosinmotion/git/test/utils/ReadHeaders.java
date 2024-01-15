package com.chaosinmotion.git.test.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * A number of our files use a format similar to:
 *
 * header<sp>line
 * ...
 * blank line
 * content
 *
 * This handles loading the header lines and the content, including handling
 * line wrapping of header lines
 */
public class ReadHeaders
{
	private BufferedReader reader;
	private String lastLine;
	private boolean atEOH;

	public static class Header
	{
		public final String name;
		public final String value;

		public Header(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
	}

	/**
	 * Create a new reader for the given input stream
	 * @param is The input stream to read from
	 */
	public ReadHeaders(InputStream is)
	{
		InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
		reader = new BufferedReader(isr);
	}

	/**
	 * This returns the next header, or null if we're at the end of the file or
	 * a blank line marking the end of the headers.
	 * @return The next header, or null if we're at the end of the file
	 * @throws IOException
	 */
	public Header readHeader() throws IOException
	{
		if (atEOH) return null;

		/*
		 *	Read the first line, if not already in our buffer
		 */

		if (null == lastLine) {
			lastLine = reader.readLine();
		}

		// Are we at the end of all of this?
		if (lastLine.isEmpty()) {
			atEOH = true;
			return null;
		}

		// Now start concatenating the lines. We trim the leading space but
		// append a newline at the end of the line before concatenating

		StringBuilder builder = new StringBuilder();
		builder.append(lastLine);

		for (;;) {
			lastLine = reader.readLine();
			if (null == lastLine) {
				atEOH = true;
				break;
			}
			if (!lastLine.isEmpty() && (lastLine.charAt(0) == ' ')) {
				builder.append('\n');
				builder.append(lastLine.substring(1));
			} else {
				break;
			}
		}

		// Parse the header by finding the first space
		int index = builder.indexOf(" ");
		String headerName = builder.substring(0, index);
		String headerValue = builder.substring(index + 1);

		return new Header(headerName, headerValue);
	}

	/**
	 * If we are at the end of the header, this will return the rest of
	 * the contents
	 * @return The contents of the file, or null if we're not at the end of the
	 * @throws IOException
	 */
	public String readContent() throws IOException
	{
		if (!atEOH) return null;

		StringBuilder builder = new StringBuilder();
		for (;;) {
			lastLine = reader.readLine();
			if (null == lastLine) {
				break;
			}
			builder.append(lastLine);
			builder.append('\n');
		}
		return builder.toString();
	}
}

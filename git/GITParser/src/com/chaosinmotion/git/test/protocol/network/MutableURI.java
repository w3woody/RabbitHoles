package com.chaosinmotion.git.test.protocol.network;

import com.chaosinmotion.git.test.utils.Pair;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>URI processing class. Contains the code for pulling apart and assembling
 * URIs. This follows the pattern outlined in the Wikipedia article on the
 * subject, including identifying the authority and path and other components.</p>
 *
 * <a href="https://en.wikipedia.org/wiki/URL">Wikipedia URL</a>
 *
 * <p>This also contains utiltiies for modifying the query string: for appending
 * to the path, updating query parameters and modifying the other parameters
 * of the path</p>
 */
public class MutableURI
{
	private String scheme;

	private String username;
	private String password;
	private String host;
	private int port;

	private ArrayList<String> path = new ArrayList<>();

	private ArrayList<Pair<String,String>> query = new ArrayList<>();
	private String fragment;

	/**
	 * This is a simple string builder class which does not release internal
	 * memory inbetween being cleared. This is used to build up the string
	 * components, and we use this instead of a string builder in order to
	 * avoid the overhead of creating a new string builder for each component
	 */
	private static class StringGenerator
	{
		private int size;
		private char[] buffer;

		public StringGenerator()
		{
			size = 0;
			buffer = new char[256];
		}

		public void clear()
		{
			size = 0;
		}

		public void append(int c)
		{
			if (size >= buffer.length) {
				char[] newbuffer = new char[buffer.length * 2];
				System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
				buffer = newbuffer;
			}
			buffer[size++] = (char)c;
		}

		public int getSize()
		{
			return size;
		}

		public String toString()
		{
			return new String(buffer,0,size);
		}
	}

	private static boolean validURLChars(char c)
	{
		if ((c >= 'a') && (c <= 'z')) return true;
		if ((c >= 'A') && (c <= 'Z')) return true;
		if ((c >= '0') && (c <= '9')) return true;

		if ((c == '~') || (c == '-') || (c == '.') || (c == '_')) return true;
		return false;
	}

	/**
	 * Returns true if the string passed in can be handled unmodified as a
	 * URI query parameter
	 * @param str The string to test
	 * @return True if this can be passed unmodified
	 */
	private static boolean validURLString(String str)
	{
		int i,len = str.length();
		for (i = 0; i < len; ++i) {
			char c = str.charAt(i);
			if (!validURLChars(c)) return false;
		}
		return true;
	}

	private static int toHex(int c)
	{
		if ((c >= 'a') && (c <= 'f')) return 10 + (c - 'a');
		if ((c >= 'A') && (c <= 'F')) return 10 + (c - 'A');
		if ((c >= '0') && (c <= '9')) return (c - '0');
		return 0;
	}

	private static boolean isHex(int c)
	{
		if ((c >= 'a') && (c <= 'f')) return true;
		if ((c >= 'A') && (c <= 'F')) return true;
		if ((c >= '0') && (c <= '9')) return true;
		return false;
	}

	/**
	 * Handle percent-encoding decoding of the string.
	 * @param str The string from the query path
	 * @return The decoded string. Note we handle UTF-8 characters as in the
	 * specification
	 */
	public static String decodeURL(String str) throws URISyntaxException
	{
		/*
		 *	Determine if this can be passed 'as-is'
		 */

		if (validURLString(str)) return str;

		/*
		 *	Handle the decoding of the percent encoded characters
		 */

		StringReader r = new StringReader(str);
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		int ch;

		while (-1 != (ch = r.read())) {
			if (ch == '+') {
				b.write(' ');
			} else if (ch == '%') {
				// Decode hex
				int ach = r.read();
				int bch = r.read();
				if (!isHex(ach) || !isHex(bch)) {
					throw new URISyntaxException(str,"Ill-formed URI encoding");
				}
				int cch = (toHex(ach) << 4) | toHex(bch);
				b.write(cch);
			} else {
				b.write(ch);
			}
		}

		return b.toString(StandardCharsets.UTF_8);
	}

	public static String encodeURL(String str)
	{
		/*
		 *	Determine if this can be passed 'as-is'
		 */

		if (validURLString(str)) return str;

		/*
		 *	Convert to an array of bytes and encode
		 */

		byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
		StringGenerator g = new StringGenerator();

		for (byte b: utf8) {
			if (validURLChars((char)b)) {
				g.append((char)b);
			} else if (b == ' ') {
				g.append('+');
			} else {
				byte msn = (byte) (0x0F & (b >> 4));
				if (msn >= 10) {
					g.append('a' + (msn - 10));
				} else {
					g.append('0' + msn);
				}

				byte lsn = (byte) (0x0F & b);
				if (lsn >= 10) {
					g.append('a' + (lsn - 10));
				} else {
					g.append('0' + lsn);
				}
			}
		}

		return g.toString();
	}

	/**
	 * This is a convenience class for pulling characters out of our string, one
	 * at a time.
	 */
	private static class StringReader
	{
		private final int len;
		private final String string;
		private int pos;

		public StringReader(String string)
		{
			this.string = string;
			this.len = string.length();
			this.pos = 0;
		}

		public int read()
		{
			if (pos >= len) return -1;
			return string.charAt(pos++);
		}

		public int peek()
		{
			if (pos >= len) return -1;
			return string.charAt(pos);
		}

		public void skip()
		{
			pos++;
			if (pos > len) pos = len;
		}

//		public void unread()
//		{
//			if (pos > 0) --pos;
//		}
	}

	public MutableURI()
	{
		// Generate default empty URI that can be populated piecemeal
	}

	/**
	 *  Convenience constructor for creating a URI from a scheme, host and
	 *  path
	 * @param scheme The scheme, such as "https"
	 * @param host The host, such as "google.com"
	 * @param path The path, such as "/foo". Note this does not accept
	 *             queries or fragments.
	 */
	public MutableURI(String scheme, String host, String path)
	{
		this.scheme = scheme;
		this.host = host;
		appendPath(path);
	}

	/**
	 * Copy constructor
	 * @param uri
	 */
	public MutableURI(MutableURI uri)
	{
		this.scheme = uri.scheme;
		this.username = uri.username;
		this.password = uri.password;
		this.host = uri.host;
		this.port = uri.port;
		this.path = new ArrayList<>(uri.path);
		this.query = new ArrayList<>(uri.query);
		this.fragment = uri.fragment;
	}

	/**
	 * Parse the URI string into its components
	 * @param string The string to decode
	 * @throws URISyntaxException If the string is not a valid URI
	 */
	public MutableURI(String string) throws URISyntaxException
	{
		StringGenerator builder = new StringGenerator();
		StringReader reader = new StringReader(string);
		int c;

		/*
		 *	Pull the scheme. The scheme is any combination of letters, digits
		 * and certain symbols, ending with a ':'. Note that we don't bother
		 * to check if the scheme contains invalid charaters, we only use the
		 * idea that it terminates with a colon.
		 */

		while (-1 != (c = reader.read())) {
			if (c == ':') break;
			builder.append(c);
		}
		scheme = builder.toString();

		if (c != ':') {
			throw new URISyntaxException(string, "Missing scheme");
		}

		/*
		 *	Determine if we have an authority. The authority section follows
		 *  a '//' mark, and may consist of a username, password, host and port
		 *
		 *	Note: once we exit this block of code, c is loaded with the next
		 *	character to read.
		 */

		c = reader.read();
		if ((c == '/') && (reader.peek() == '/')) {
			reader.skip();

			/*
			 *	This gets complicated in that as we read the authority, it may
			 * 	be that what we thought was a host turns out to be a username.
			 *
			 * 	Thus the funky logic.
			 */

			boolean done = false;
			boolean foundUserInfo = false;
			boolean intoSecond = false;

			while (!done) {
				// Pull the next symbol
				builder.clear();
				while (-1 != (c = reader.read())) {
					// If we hit any possible delimiter, also stop
					if ((c == '@') || (c == ':') || (c == '/') || (c == '?') || (c == '#')) break;
					builder.append(c);
				}
				if (c == '@') {
					/*
					 *	At this point we've finished reading the userinfo, and
					 * 	whatever we thought was the host or port string are
					 * 	actually the username and password
					 */

					if (foundUserInfo) {
						throw new URISyntaxException(string,
													 "Ill formed authority: duplicate '@'");
					}
					foundUserInfo = true;

					if (intoSecond) {
						intoSecond = false;

						username = decodeURL(host);
						password = decodeURL(builder.toString());
						host = null;
					} else {
						username = decodeURL(builder.toString());
						password = null;
					}
				} else if (c == ':') {
					/*
					 *	Found a colon. Store into the appropriate field
					 */

					if (intoSecond) {
						throw new URISyntaxException(string,
													 "Ill formed authority: duplicate ':'");
					}
					intoSecond = true;

					/*
					 *	At this stage we either know we are reading a host or
					 * 	we don't know if it's a host or username. In either
					 * 	case we copy the string into the host field;
					 * 	if we encounter a '@' we will then move the string
					 * 	over to the appropriate field.
					 *
					 * 	NOTE: For security reasons we DO NOT url decode the
					 * 	host name.
					 */
					host = builder.toString();
				} else {
					/*
					 *	We hit the '/' character or EOF. Store the field we
					 * 	just read and move on. We know at this point the values
					 *  must go into the host or port field.
					 */

					if (intoSecond) {
						try {
							port = Integer.parseInt(builder.toString());
						}
						catch (NumberFormatException ex) {
							throw new URISyntaxException(string,
														 "Ill formed authority: port is not a number");
						}
					} else {
						/*
						 * 	NOTE: For security reasons we DO NOT url decode the
						 * 	host name.
						 */

						host = builder.toString();
					}

					done = true;
				}
			}
		} else {
			c = reader.read();
		}

		/*
		 *	At this point we are on a path. The path may begin with a '/'
		 * 	character; we treat this as a delimiter if we have an authority
		 * 	defined.
		 */

		builder.clear();
		if ((host != null) && (c == '/')) {
			// If we have an authority, skip the '/'
			c = reader.read();
		}

		while ((c != -1) && (c != '?') && (c != '#')) {
			if (c == '/') {
				path.add(builder.toString());
				builder.clear();
			} else {
				builder.append(c);
			}
			c = reader.read();
		}
		if ((builder.getSize() > 0) || !path.isEmpty()) {
			// This happens if we have a trailing '/' character. Treat it
			// as a blank entry at the end
			path.add(builder.toString());
		}

		if (c == '?') {
			/*
			 *	Read the query string
			 */

			String key = null;
			String value = null;
			boolean second = false;
			builder.clear();

			c = reader.read();
			while ((c != -1) && (c != '#')) {
				if (c == '=') {
					key = builder.toString();
					second = true;
				} else if ((c == ';') || (c == '&')) {
					if (second) {
						value = builder.toString();
					} else {
						key = builder.toString();
					}

					Pair<String,String> p = new Pair(decodeURL(key),decodeURL(value));
					query.add(p);

					key = null;
					value = null;

					second = false;
				}
				c = reader.read();
			}

			if ((builder.getSize() > 0) || (key != null)) {
				// we read something after the last delimiter. Spill.
				if (builder.getSize() > 0) {
					if (second) {
						value = builder.toString();
					} else {
						key = builder.toString();
					}
				}

				Pair<String,String> p = new Pair(decodeURL(key),decodeURL(value));
				query.add(p);
			}
		}

		if (c == '#') {
			/*
			 *	Read the trailing fragment
			 */
			builder.clear();
			while (-1 != (c = reader.read())) {
				builder.append(c);
			}
			fragment = builder.toString();
		}
	}

	public String getScheme()
	{
		return scheme;
	}

	public void setScheme(String scheme)
	{
		this.scheme = scheme;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	/**
	 * Note that if this is a URN, the path contains one element and the'
	 * element is the URN path attribute.
	 * @return
	 */
	public ArrayList<String> getPath()
	{
		return path;
	}

	public void setPath(ArrayList<String> path)
	{
		this.path = path;
	}

	public ArrayList<Pair<String, String>> getQuery()
	{
		return query;
	}

	public void setQuery(ArrayList<Pair<String, String>> query)
	{
		this.query = query;
	}

	public String getFragment()
	{
		return fragment;
	}

	public void setFragment(String fragment)
	{
		this.fragment = fragment;
	}

	/*
	 *	Modification utilities
	 */

	/**
	 * This sets the path with the value provided. Note that this can be used
	 * with URNs to update the URL value, or can be used to replace the path
	 * in a URL.
	 *
	 * @param pvalue The path (potentially '/' separated) to replace the current
	 *               path with.
	 */
	public void setPath(String pvalue)
	{
		path.clear();
		appendPath(pvalue);
	}

	/**
	 * This extracts the elements of the path provided, and updates or modifies
	 * the path. Note we handle '..' and '.' fields appropriately by removing
	 * or updating the path as needed.
	 *
	 * This method assumes the URI is a URL, and that the path is a '/'
	 * separated list of path elements.
	 *
	 * @param pfrag The path to append or update with.
	 */
	public void appendPath(String pfrag)
	{
		int ch;
		StringReader r = new StringReader(pfrag);
		StringGenerator b = new StringGenerator();

		/*
		 *	See if we start with a '/' symbol. If so, we're rewriting the path
		 */

		ch = r.read();
		if (ch == '/') {
			path.clear();
			ch = r.read();
		} else {
			/*
			 *	If the last string in the path is empty, we delete it. THis
			 * 	handles the case where the URI was initialized with a
			 * 	'foo/' path, and we append 'bar'
			 */

			int len = path.size();
			if (len > 0) {
				String end = path.get(len-1);
				if (end.isEmpty()) {
					path.remove(len-1);
				}
			}
		}

		/*
		 *	Split the fragment and process. If we see a '.', we don't add or
		 * 	change the path. If we see a '..' we move up one in the path.
		 */

		boolean lastSlash = false;
		while (ch != -1) {
			if (ch == '/') {
				lastSlash = true;
				String pelem = b.toString();
				if (!pelem.contentEquals(".")) {
					if (pelem.contentEquals("..")) {
						int end = path.size();
						if (end > 0) {
							path.remove(end-1);
						}
					} else {
						path.add(pelem);
					}
				}
				b.clear();
			} else {
				lastSlash = false;
				b.append(ch);
			}
			ch = r.read();
		}

		if (lastSlash || (b.getSize() > 0)) {
			String pelem = b.toString();
			if (!pelem.contentEquals(".")) {
				if (pelem.contentEquals("..")) {
					int end = path.size();
					if (end > 0) {
						path.remove(end - 1);
					}
				} else {
					path.add(pelem);
				}
			}
		}
	}

	/**
	 * This sets the query (or adds the query) to the value given. This
	 * scans for the first instance of key, and replaces the value if found.
	 * If not found, this appends the key/value pair.
	 * @param key The query key
	 * @param value The query value
	 */
	public void setQuery(String key, String value)
	{
		Pair<String,String> updated = new Pair<>(key,value);

		int i,len = query.size();
		for (i = 0; i < len; ++i) {
			Pair<String,String> p = query.get(i);
			if (p.first.contentEquals(key)) {
				// Replace in the same spot in the array
				query.set(i,updated);
				return;
			}
		}
		query.add(updated);
	}

	/**
	 * This clears the query key. This scans for ALL instances of the
	 * key, and removes the pair from the key/value pairs.
	 */

	public void clearQuery(String key)
	{
		Iterator<Pair<String,String>> iter = query.iterator();
		while (iter.hasNext()) {
			Pair<String,String> p = iter.next();
			if (p.first.contentEquals(key)) {
				iter.remove();
			}
		}
	}

	/**
	 * Formats our URI.
	 * @return
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		if (scheme != null) {
			builder.append(scheme);
			builder.append(':');
		} else {
			builder.append("urn:");		// Stupid default, I suppose.
		}

		if ((host != null) || (username != null) || (password != null) || (port != 0)) {
			/*
			 *	We have at least some fragment of an authority. Do our best to
			 * 	write something out.
			 */

			builder.append("//");
			if ((username != null) || (password != null)) {
				if (username != null) builder.append(encodeURL(username));
				if (password != null) {
					builder.append(':');
					builder.append(encodeURL(password));
				}
				builder.append('@');
			}
			if (host != null) builder.append(encodeURL(host));
			if (port != 0) {
				builder.append(':');
				builder.append(port);
			}

			/*
			 *	If we have a path, we append the '/' character here.
			 */

			if (path.size() > 0) {
				builder.append('/');
			}
		}

		/*
		 *	Append the path. If this is empty nothing is appended
		 */
		boolean first = true;
		for (String str: path) {
			if (first) {
				first = false;
			} else {
				builder.append('/');
			}
			builder.append(str);
		}

		if ((query != null) && (query.size() > 0)) {
			builder.append('?');
			first = true;
			for (Pair<String,String> p: query) {
				if (first) {
					first = false;
				} else {
					builder.append('&');
				}
				builder.append(encodeURL(p.first));
				builder.append('=');
				builder.append(encodeURL(p.second));
			}
		}

		if ((fragment != null) && !fragment.isEmpty()) {
			builder.append('#');
			builder.append(fragment);
		}

		return builder.toString();
	}
}

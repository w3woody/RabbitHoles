package com.chaosinmotion.git.test.protocol.network;

import com.chaosinmotion.git.test.protocol.parsing.PktLineWriter;
import com.chaosinmotion.git.test.utils.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared class for processing network requests.
 *
 * Should implement a basic HTTP/HTTPS request client to implement the HTTP
 * version of the GIT smart protocols documented here:
 *
 * https://git-scm.com/book/en/v2/Git-Internals-Transfer-Protocols
 *
 * Note this provides a generic service for GET and POST commands, without any
 * knowledge of what's being transported.
 */
public class HTTPConnection
{
	private MutableURI baseURI;
	private CookieManager cookieManager;

	/**
	 * Wrapper for the HTTP response. Includes the result code and a pointer
	 * to the input stream to read the results.
	 */
	public static class Response
	{
		public final int code;
		public final InputStream data;

		private Response(int code, InputStream data)
		{
			this.code = code;
			this.data = data;
		}

		public byte[] getAllData() throws IOException
		{
			if (data != null) return data.readAllBytes();
			return null;
		}

		public boolean isSuccessful()
		{
			return isSuccessCode(code);
		}
	}

	public HTTPConnection(String uri) throws URISyntaxException
	{
		baseURI = new MutableURI(uri);

		cookieManager = new CookieManager();
	}

	private static boolean isSuccessCode(int code)
	{
		return ((code >= 200) && (code < 300));
	}

	/**
	 * Make a GET request to the server.
	 *
	 * @param path the path to the resource
	 * @param params the parameters to pass to the server as part of the URI
	 * @param headers the headers to pass to the server
	 * @param callback the callback to invoke when the request completes
	 */
	public void get(String path, Collection<Pair<String,String>> params, Collection<Pair<String,String>> headers, Finish<Response> callback)
	{
		ThreadPool.shared.enqueue(() -> {
			try {
				/*
				 *	Construct the URI then make the request
				 */
				MutableURI uri = new MutableURI(baseURI);
				uri.appendPath(path);
				if (params != null) {
					for (Pair<String,String> header: params) {
						uri.setQuery(header.first,header.second);
					}
				}

				URL url = new URL(uri.toString());
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestMethod("GET");
				attachCookies(conn);
				if (headers != null) {
					for (Pair<String,String> header: headers) {
						conn.setRequestProperty(header.first,header.second);
					}
				}

				conn.connect();

				int responseCode = conn.getResponseCode();
				InputStream data;
				boolean success;
				if (isSuccessCode(responseCode)) {
					success = true;
					data = conn.getInputStream();
					storeCookies(conn);
				} else {
					success = false;
					data = conn.getErrorStream();
				}

				Response response = new Response(responseCode,data);
				callback.finish(success,response);
			}
			catch (Throwable th) {
				callback.finish(false,null);
			}
		});
	}

	/**
	 * Make a POST request to the server
	 *
	 * @param path the path to the resource
	 * @param params the parameters to pass to the server as part of the URI
	 * @param headers the headers to pass to the server
	 * @param content the content to send to the server
	 * @param callback the callback to invoke when the request completes
	 */
	public void post(String path, Collection<Pair<String,String>> params, Collection<Pair<String,String>> headers, byte[] content, Finish<Response> callback)
	{
		ThreadPool.shared.enqueue(() -> {
			try {
				/*
				 *	Construct the URI then make the request
				 */
				MutableURI uri = new MutableURI(baseURI);
				uri.appendPath(path);
				if (params != null) {
					for (Pair<String,String> header: params) {
						uri.setQuery(header.first,header.second);
					}
				}

				URL url = new URL(uri.toString());
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestMethod("POST");
				attachCookies(conn);
				if (headers != null) {
					for (Pair<String,String> header: headers) {
						conn.setRequestProperty(header.first,header.second);
					}
				}

				if (content != null) {
					conn.setDoOutput(true);

					OutputStream os = conn.getOutputStream();
					os.write(content);
					os.close();
				}

				conn.connect();

				int responseCode = conn.getResponseCode();
				boolean success;
				InputStream data;
				if (isSuccessCode(responseCode)) {
					success = true;
					data = conn.getInputStream();
					storeCookies(conn);
				} else {
					success = false;
					data = conn.getErrorStream();
				}

				Response response = new Response(responseCode,data);
				callback.finish(success,response);
			}
			catch (Throwable th) {
				callback.finish(false, null);
			}
		});
	}

	private void storeCookies(HttpURLConnection conn)
	{
		// Get the cookies from the connection and store them
		Map<String, List<String>> headerFields = conn.getHeaderFields();
		List<String> cookies = headerFields.get("Set-Cookie");

		if (cookies != null) {
			for (String cookie: cookies) {
				cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
			}
		}
	}

	private void attachCookies(HttpURLConnection conn)
	{
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (HttpCookie cookie: cookieManager.getCookieStore().getCookies()) {
			if (first) {
				first = false;
			} else {
				builder.append("; ");
			}
			builder.append(cookie.toString());
		}
		if (builder.isEmpty()) return;		// Don't add cookie if we have none

		conn.setRequestProperty("Cookie",builder.toString());
	}
}

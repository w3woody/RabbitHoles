package com.chaosinmotion.git.test.protocol.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

/**
 * Shared class for processing network requests.
 *
 * Should implement a basic HTTP/HTTPS request client to implement the HTTP
 * version of the GIT smart protocols documented here:
 *
 * https://git-scm.com/book/en/v2/Git-Internals-Transfer-Protocols
 */
public class HTTPConnection
{
	private MutableURI baseURI;

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

	public interface Callback
	{
		void response(Response response);
	}

	public HTTPConnection(String uri) throws URISyntaxException
	{
		baseURI = new MutableURI(uri);
	}

	private static boolean isSuccessCode(int code)
	{
		return ((code >= 200) && (code < 300));
	}

	public void get(String path, HashMap<String,String> params, Callback callback)
	{
		ThreadPool.shared.enqueue(() -> {
			try {
				/*
				 *	Construct the URI then make the request
				 */
				MutableURI uri = new MutableURI(baseURI);
				uri.appendPath(path);
				if (params != null) {
					for (HashMap.Entry<String,String> entry: params.entrySet()) {
						uri.setQuery(entry.getKey(),entry.getValue());
					}
				}

				URL url = new URL(uri.toString());
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Git-Protocol","version=2");

				conn.connect();

				int responseCode = conn.getResponseCode();
				InputStream data;
				if (isSuccessCode(responseCode)) {
					data = conn.getInputStream();
				} else {
					data = conn.getErrorStream();
				}

				Response response = new Response(responseCode,data);
				callback.response(response);
			}
			catch (Throwable th) {
				Response response = new Response(0,null);
				callback.response(response);
			}
		});
	}
}

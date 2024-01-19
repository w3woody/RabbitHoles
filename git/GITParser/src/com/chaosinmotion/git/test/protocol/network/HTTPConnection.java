package com.chaosinmotion.git.test.protocol.network;

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

	public static class Response
	{
		public final int code;
		public final byte[] data;

		private Response(int code, byte[] data)
		{
			this.code = code;
			this.data = data;
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
				conn.connect();

				int responseCode = conn.getResponseCode();
				byte[] data = null;
				if (responseCode == 200) {
					data = conn.getInputStream().readAllBytes();
				} else {
					data = conn.getErrorStream().readAllBytes();
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

package com.chaosinmotion.git.test.protocol;

import com.chaosinmotion.git.test.protocol.network.HTTPConnection;
import com.chaosinmotion.git.test.protocol.network.ThreadPool;

import java.util.HashMap;

/**
 * Test harness to sort out what's going on
 */
public class TestProtocol
{
	public static void test1()
	{
		try {
			HTTPConnection c = new HTTPConnection("https://github.com/w3woody/blocks.git");

			HashMap<String,String> params = new HashMap<>();
			// git-upload-pack
			// git-receive-pack

			params.put("service","git-upload-pack");
			c.get("info/refs",params,(response) -> {
				System.out.println("Response: " + response.code);
				System.out.println("Data: " + new String(response.data));
			});

			ThreadPool.shared.shutdown();
		}
		catch (Throwable th) {
			th.printStackTrace(System.err);
		}
	}
}

package com.chaosinmotion.git.test.protocol;

import com.chaosinmotion.git.test.protocol.network.GITV2Connection;
import com.chaosinmotion.git.test.protocol.network.HTTPConnection;
import com.chaosinmotion.git.test.protocol.network.ThreadPool;
import com.chaosinmotion.git.test.protocol.parsing.PktLineReader;
import com.chaosinmotion.git.test.utils.Pair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Test harness to sort out what's going on
 */
public class TestProtocol
{
	private static String toText(byte[] data)
	{
		return new String(data, StandardCharsets.UTF_8);
	}

	public static void test1()
	{
		try {
			HTTPConnection c = new HTTPConnection("https://github.com/w3woody/blocks.git");

			ArrayList<Pair<String,String>> params = new ArrayList<>();
			// git-upload-pack
			// git-receive-pack
			params.add(new Pair<>("service","git-upload-pack"));

			ArrayList<Pair<String,String>> headers = new ArrayList<>();
			headers.add(new Pair<>("Accept","application/x-git-upload-pack-result"));
			headers.add(new Pair<>("Git-Protocol","version=2"));

			c.get("info/refs",params,headers,(success, response) -> {
				try {
					if (!success) {
						System.out.println("Request failed");
						return;
					}

					System.out.println("Response: " + response.code);

					PktLineReader reader = new PktLineReader(response.data);
					PktLineReader.Return block;
					while (null != (block = reader.read())) {
						if (block.data != null) {
							System.out.println(
									"Data:  " + block.data.length);
							System.out.println("    " + toText(block.data));
						} else {
							System.out.println("Block: " + block.code);
						}
					}
				}
				catch (Throwable th) {
					th.printStackTrace(System.err);
				}
			});

			ThreadPool.shared.shutdown();
		}
		catch (Throwable th) {
			th.printStackTrace(System.err);
		}
	}

	public static void test2()
	{
		try {
			GITV2Connection connection = new GITV2Connection("https://github.com/w3woody/blocks.git");
			connection.start((success, capabilities) -> {
				if (!success) {
					System.out.println("Request failed");
					return;
				}

				System.out.println("Capabilities:");
				for (String key: capabilities.map.keySet()) {
					System.out.println("    " + key + " = " + capabilities.map.get(key));
				}

				connection.lsRefs(null,false,false,true,(success1, refs) -> {
					if (!success1) {
						System.out.println("Request failed");
						return;
					}

					System.out.println(refs);
				});
			});

			ThreadPool.shared.shutdown();
		}
		catch (Throwable th) {
			th.printStackTrace(System.err);
		}
	}
}

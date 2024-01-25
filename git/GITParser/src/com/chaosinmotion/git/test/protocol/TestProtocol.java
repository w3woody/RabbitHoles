package com.chaosinmotion.git.test.protocol;

import com.chaosinmotion.git.test.protocol.network.HTTPConnection;
import com.chaosinmotion.git.test.protocol.network.ThreadPool;
import com.chaosinmotion.git.test.protocol.parsing.PktLineReader;

import java.nio.charset.StandardCharsets;
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

			HashMap<String,String> params = new HashMap<>();
			// git-upload-pack
			// git-receive-pack

			params.put("service","git-upload-pack");
			c.get("info/refs",params,(response) -> {
				try {
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
}

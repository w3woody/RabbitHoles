package com.chaosinmotion.git.test.protocol.network;

import com.chaosinmotion.git.test.protocol.parsing.PktLineReader;
import com.chaosinmotion.git.test.protocol.parsing.PktLineType;
import com.chaosinmotion.git.test.protocol.parsing.PktLineWriter;
import com.chaosinmotion.git.test.utils.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Wraps the HTTP connection and provides the commands for ls-refs, fetch, and
 * push.
 *
 * See gitprotocol-v2.txt for details on ls-refs and fetch. See
 * gitprotocol-http.txt for details of the GIT HTTP protocol underlying the
 * rest of this.
 *
 * Note git-receive-pack is not documented, but we don't try to implement it,
 * as we're only interested in clone and fetch.
 */

public class GITV2Connection extends HTTPConnection
{
	private boolean isError = false;
	private Capabilities capabilities = null;

	/**
	 * This class stores the capabilities that were advertised on startup.
	 */
	public static class Capabilities
	{
		public final HashMap<String, HashSet<String>> map;

		private Capabilities(HashMap<String,HashSet<String>> map)
		{
			this.map = map;
		}
	}

	/**
	 * A single reference in the list of references returned by our ls-ref
	 * command API call. Also contains a flag indicating if this is unborn,
	 * as well as peeled and symref references.
	 */
	public static class Reference
	{
		public final String name;
		public final String referenceID;
		public final boolean unborn;

		public final String symref;
		public final String peeled;

		/**
		 * Parses the response row into a reference object. Each row has the
		 * format:
		 *
		 * 			obj-id-or-unborn = (obj-id | "unborn")
		 * 			ref = PKT-LINE(obj-id-or-unborn SP refname *(SP ref-attribute) LF)
		 * 			ref-attribute = (symref | peeled)
		 * 			symref = "symref-target:" symref-target
		 * 			peeled = "peeled:" obj-id
		 *
		 * @param row The string to parse
		 */
		private Reference(String row)
		{
			String[] parts = row.trim().split(" ");
			if (parts.length < 2) {
				throw new IllegalArgumentException("Invalid reference row: " + row);
			}
			name = parts[1];
			referenceID = parts[0];
			unborn = referenceID.equals("unborn");

			String sref = null;
			String peel = null;
			for (int i = 2; i < parts.length; ++i) {
				String part = parts[i];
				if (part.startsWith("symref-target:")) {
					sref = part.substring(14);
				} else if (part.startsWith("peeled:")) {
					peel = part.substring(7);
				}
			}
			symref = sref;
			peeled = peel;
		}
	}

	public static class References
	{
		public final List<Reference> references;
		public final HashMap<String,Reference> nameMap;
		public final HashMap<String,Reference> idMap;

		private References(ArrayList<Reference> references)
		{
			this.references = references;
			nameMap = new HashMap<>();
			idMap = new HashMap<>();
			for (Reference r: references) {
				nameMap.put(r.name,r);
				idMap.put(r.referenceID,r);
			}
		}
	}

	public GITV2Connection(String uri) throws URISyntaxException
	{
		super(uri);
	}

	/**
	 * Perform the initial service discovery request, and store the results
	 * internally. This should be the first call made when connecting to do
	 * a clone or fetch operation.
	 *
	 * @param callback callback to indicate the end of the request.
	 */

	public void start(Finish<Capabilities> callback)
	{
		/*
		 *	Populate the parameters and headers for our request
		 */
		ArrayList<Pair<String,String>> params = new ArrayList<>();
		params.add(new Pair<>("service","git-upload-pack"));

		ArrayList<Pair<String,String>> headers = new ArrayList<>();
		headers.add(new Pair<>("Accept","application/x-git-upload-pack-result"));
		headers.add(new Pair<>("Git-Protocol","version=2"));

		get("info/refs",params,headers,(success, response) -> {
			try {
				if (!success) {
					isError = true;
					callback.finish(false,null);
					return;
				}

				/*
				 *	Parse the response that we got, storing the capabilities as
				 * 	we parse them
				 */

				PktLineReader reader = new PktLineReader(response.data);
				PktLineReader.Return block;

				/*
				 *	Read the header. This should be one line and a 'flush'
				 * 	command.
				 */

				if (null == (block = reader.read())) {
					isError = true;
					callback.finish(false,null);
					return;
				}
				String service = new String(block.data, StandardCharsets.UTF_8).trim();
				if (!service.equals("# service=git-upload-pack")) {
					isError = true;
					callback.finish(false,null);
					return;
				}

				block = reader.read();
				if ((block == null) || (block.code != PktLineType.FLUSH)) {
					isError = true;
					callback.finish(false,null);
					return;
				}

				/*
				 *	Read the capabilities advertisement and store into a
				 * 	capabilities object
				 */
				block = reader.read();
				if ((block == null) || (block.code != PktLineType.LINE)) {
					isError = true;
					callback.finish(false,null);
					return;
				}
				String version = new String(block.data, StandardCharsets.UTF_8).trim();
				if (!version.equals("version 2")) {
					isError = true;
					callback.finish(false,null);
					return;
				}

				HashMap<String, HashSet<String>> map = new HashMap<>();
				while (null != (block = reader.read())) {
					if (block.code == PktLineType.FLUSH) {
						break;
					}
					if (block.code != PktLineType.LINE) {
						isError = true;
						callback.finish(false,null);
						return;
					}

					String cap = new String(block.data, StandardCharsets.UTF_8).trim();
					String key,value;

					int eq = cap.indexOf('=');
					if (eq == -1) {
						key = cap;
						value = "";
					} else {
						key = cap.substring(0,eq);
						value = cap.substring(eq+1);
					}

					String[] values = value.split(" ");
					HashSet<String> set = new HashSet<>();
					for (String v: values) {
						set.add(v);
					}

					map.put(key,set);
				}

				capabilities = new Capabilities(map);
				reader.close();

				callback.finish(true,capabilities);
			}
			catch (Throwable th) {
				isError = true;
				callback.finish(false,null);
			}
		});
	}

	private boolean hasCapability(String command, String capability)
	{
		HashSet<String> set = capabilities.map.get(command);
		if (set == null) {
			return false;
		}
		return set.contains(capability);
	}

	/**
	 * Performs the ls-ref command with the list of haves and wants
	 */

	public void lsRefs(ArrayList<String> prefix, boolean symrefs, boolean peel, boolean unborn, Finish<References> callback)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PktLineWriter writer = new PktLineWriter(baos);

		try {
			writer.write("command=ls-refs\n");
			if (unborn && hasCapability("ls-ref","unborn")) {
				writer.write("unborn\n");
			}

			if (((prefix != null) && !prefix.isEmpty()) || symrefs || peel) {
				// We have commands, so write the command delimiter
				writer.write(PktLineType.DELIM);
			}

			// Write the commands
			if (symrefs) {
				writer.write("symrefs\n");
			}
			if (peel) {
				writer.write("peel\n");
			}
			if (prefix != null) {
				for (String p: prefix) {
					writer.write("ref-prefix " + p + "\n");
				}
			}
			writer.write(PktLineType.FLUSH);
			writer.close();

			byte[] content = baos.toByteArray();

			/*
			 *	Populate the parameters and headers for our request
			 */

			ArrayList<Pair<String,String>> params = new ArrayList<>();
//			params.add(new Pair<>("service","git-upload-pack"));

			ArrayList<Pair<String,String>> headers = new ArrayList<>();
			headers.add(new Pair<>("Accept","application/x-git-upload-pack-result"));
			headers.add(new Pair<>("Content-Type","application/x-git-upload-pack-request"));
			headers.add(new Pair<>("Git-Protocol","version=2"));

			post("git-upload-pack",params,headers,content,(success, response) -> {
				try {
					if (!success) {
						byte[] data = response.getAllData();
						String error = (data == null) ? "" : new String(data, StandardCharsets.UTF_8);
						System.out.println("error: " + response.code + " " + error);
						callback.finish(false,null);
						return;
					}

					/*
					 *	Parse the response that we got
					 *
					 * 	TODO: We're expecting
					 *
					 * The output of ls-refs is as follows:

						output = *ref
							     flush-pkt
						obj-id-or-unborn = (obj-id | "unborn")
						ref = PKT-LINE(obj-id-or-unborn SP refname *(SP ref-attribute) LF)
						ref-attribute = (symref | peeled)
						symref = "symref-target:" symref-target
						peeled = "peeled:" obj-id

					 */

					ArrayList<Reference> references = new ArrayList<>();
					PktLineReader reader = new PktLineReader(response.data);
					PktLineReader.Return block;

					while (null != (block = reader.read())) {
						if (block.data != null) {
							String line = new String(block.data, StandardCharsets.UTF_8);
							Reference r = new Reference(line);
							references.add(r);
						} else if (block.code == PktLineType.FLUSH) {
							break;
						}
					}

					reader.close();

					References refs = new References(references);
					callback.finish(true,refs);
				}
				catch (Throwable th) {
					callback.finish(false,null);
					return;
				}
			});

		}
		catch (Throwable th) {
			callback.finish(false,null);
			return;
		}
	}

	/**
	 * Returns true if start has been called and successfully completed.
	 * @return
	 */
	public boolean isInitialized()
	{
		return (capabilities != null);
	}

	public Capabilities getCapabilities()
	{
		return capabilities;
	}
}

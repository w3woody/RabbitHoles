package com.chaosinmotion.git.test.common;

/**
 * GIT object types. There are six defined types, one reserved and an unknown
 *
 * Note the offset delta and reference delta types only appear in pack files.
 */
public enum ObjectType
{
	UNKNOWN,			// Unknown			 		0
	COMMIT,				// Commit object 			1
	TREE,				// Tree object				2
	BLOB,				// Blob object				3
	TAG,				// Tag object				4
	RESERVED,			// Reserved for future use	5
	OFSDelta,			// Offset delta				6
	REFDelta;			// Reference delta			7

	/**
	 * Convert the string type read from an object header to an enum
	 * @param t
	 * @return
	 */
	public static ObjectType fromString(String t)
	{
		switch (t) {
			case "commit": return COMMIT;
			case "tree": return TREE;
			case "blob": return BLOB;
			case "tag": return TAG;
		}
		return UNKNOWN;
	}

	public static ObjectType fromByte(byte b)
	{
		switch (b) {
			case 1: return COMMIT;
			case 2: return TREE;
			case 3: return BLOB;
			case 4: return TAG;
			case 6: return OFSDelta;
			case 7: return REFDelta;
		}
		return UNKNOWN;
	}
}

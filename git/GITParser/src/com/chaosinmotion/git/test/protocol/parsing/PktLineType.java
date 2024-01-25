package com.chaosinmotion.git.test.protocol.parsing;

public enum PktLineType
{
	FLUSH,        // flush-pkt
	DELIM,        // delim-pkt
	END,        // response-end-pkt
	LINE        // data
}

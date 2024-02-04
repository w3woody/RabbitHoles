package com.chaosinmotion.git.test.protocol.network;

/**
 * Universal callback to indicate the end of a request.
 */
public interface Finish<T>
{
	/**
	 * The first parameter indicates whether the request was successful or not.
	 * The second parameter is the result of the request.
	 * @param success true if the request was successful, false otherwise
	 * @param result the result of the request
	 */
	void finish(boolean success, T result);
}

package org.eclipse.californium.core.server.resources;

/**
 * An AcceptSupport object supports a resource in checking if the requested
 * content format (accept option) of a request is consistent with the content
 * format(s) the resource provides.
 */
public interface AcceptSupport {

	/**
	 * Adds the specified values to the acceptable content format requests.
	 *
	 * @param acceptableValues the acceptable values
	 */
	public void addAcceptable(int... acceptableValues);
	
	/**
	 * Removes the specified values from the acceptable content format requests.
	 *
	 * @param values the values
	 */
	public void removeAcceptable(int... values);
	
	/**
	 * Returns true if the specified content format can be provided.
	 *
	 * @param acceptable the requested content format
	 * @return true, if the accept request can be met
	 */
	public boolean isAcceptable(int acceptable);
	
}

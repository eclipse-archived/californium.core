package org.eclipse.californium.core.server.resources;

/**
 * An ETag Support object supports a resource in generating and maintaining
 * ETags.
 */
public interface ETagSupport {
	
	/**
	 * Generates the next ETag.
	 *
	 * @return the ETag as byte array
	 */
	public byte[] nextETag();
	
	/**
	 * Gets the current ETag.
	 *
	 * @return the current ETag
	 */
	public byte[] getCurrentETag();
	
	/**
	 * Sets the current ETag.
	 *
	 * @param etag the new ETag
	 */
	public void setCurrentETag(byte[] etag);
	
	/**
	 * Gets the current ETag as string.
	 *
	 * @return the current ETag as string
	 */
	public String getCurrentETagAsString();
	
}
package org.eclipse.californium.core.server.resources;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is e default ETag support implementation for a resource. An instance of
 * this class holds the current ETag. If the content of a resource changes, it
 * should call {@link #nextETag()} to increase the ETag value by 1. This class
 * is thread-safe.
 */
public class ETagDefaultSupport implements ETagSupport {

	/** The maximum size allowed by the coap-18 draft. */
	public static final int MAX_BYTES = 8;
	
	/** The minimum size allowed by the coap-18 draft. */
	public static final int MIN_BYTES = 0;
	
	/** A static instance of random to set random initial ETag values. */
	private static final Random rand = new Random();
	
	/** The size of the ETag. */
	private int bytes;
	
	/** The current ETag value. */
	private AtomicLong current;
	
	/**
	 * Instantiates a new instance of ETagDefaultSupport that generates 8 bytes
	 * and starts with a random value. long ETags.
	 */
	public ETagDefaultSupport() {
		this(MAX_BYTES);
	}
	
	/**
	 * Instantiates a new instance of ETagDefaultSupport that generates ETags of
	 * the specified size and starts with a random value.
	 *
	 * @param bytes the size of the ETags
	 */
	public ETagDefaultSupport(int bytes) {
		this(bytes, rand.nextLong());
	}
	
	/**
	 * Instantiates a new instance of ETagDefaultSupport that generates ETags of
	 * the specified size and starts with the specified value.
	 *
	 * @param bytes the size of the ETags
	 * @param initialValue the initial value
	 */
	public ETagDefaultSupport(int bytes, long initialValue) {
		current = new AtomicLong(initialValue);
		if (bytes > MAX_BYTES)
			throw new IllegalArgumentException("ETags must not be longer than 8 bytes but is "+bytes);
		if (bytes < MIN_BYTES)
			throw new IllegalArgumentException("ETags must be at least 0 bytes long but is "+bytes);
		this.bytes = bytes;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.ETagSupport#nextETag()
	 */
	public byte[] nextETag() {
		return long2bytes(current.incrementAndGet(), bytes);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.ETagSupport#getCurrentETag()
	 */
	public byte[] getCurrentETag() {
		return long2bytes(current.get(), bytes);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.ETagSupport#setCurrentETag(byte[])
	 */
	public void setCurrentETag(byte[] etag) {
		long cur = 0;
		for (int i=0;i<etag.length && i<bytes;i++)
			cur |= (etag[i] << (etag.length - i - 1)*8);
		this.current.set(cur);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.ETagSupport#getCurrentETagAsString()
	 */
	public String getCurrentETagAsString() {
		byte[] etag = getCurrentETag();
		StringBuffer string = new StringBuffer("");
		for(byte b:etag) string.append(String.format("%02x", b&0xff));
		return string.toString();
	}
	
	/**
	 * Converts the specified long value to a byte array of maximum maxSize
	 * length. Cuts off leading zeros.
	 *
	 * @param value the value
	 * @param maxSize the max size of the byte array
	 * @return the byte[] the byte array
	 */
	private byte[] long2bytes(long value, int maxSize) {
		int length = maxSize;
		// reduce length so that there will be no leading zeros
		for (;( (value >>> (length-1)*8) & 0xFF) == 0 && length > 0;length--);
		byte[] etag = new byte[length];
		for (int i=0;i<length;i++) {
			etag[length - i - 1] = (byte) (value >>> i*8);
		}
		return etag;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "DefaultETagSupport(current: 0x"+getCurrentETagAsString()+", bytes: "+bytes+")";
	}
	
}

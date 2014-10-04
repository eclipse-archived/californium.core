package org.eclipse.californium.core.test;

import org.eclipse.californium.core.server.resources.ETagDefaultSupport;
import org.junit.Assert;
import org.junit.Test;

public class ETagSupportTest {

	@Test
	public void test() {
		ETagDefaultSupport sup = new ETagDefaultSupport(8, 77);
		
		// Test 1000 increments
		for (int i=0;i<1000;i++)
			sup.nextETag();
		
		byte[] expected = new byte[] { (byte) (1077 >>> 8), (byte) 1077 };
		Assert.assertArrayEquals(expected, sup.getCurrentETag());
		
		// Test higher values and cut at 6 bytes
		sup = new ETagDefaultSupport(6, 0xFFFFFFFFFFFFL);
		Assert.assertArrayEquals(new byte[] {-1, -1, -1, -1, -1, -1}, sup.getCurrentETag());
		Assert.assertArrayEquals(new byte[0], sup.nextETag());

		// Test setCurrentETag( X )
		sup.setCurrentETag(new byte[] { 0});
		Assert.assertArrayEquals(new byte[0], sup.getCurrentETag());
		sup.setCurrentETag(new byte[] { 1});
		Assert.assertArrayEquals(new byte[] {1}, sup.getCurrentETag());
		sup.setCurrentETag(new byte[] { 2});
		Assert.assertArrayEquals(new byte[] {2}, sup.getCurrentETag());
		sup.setCurrentETag(new byte[] { 0xF});
		Assert.assertArrayEquals(new byte[] {15}, sup.getCurrentETag());
		sup.setCurrentETag(new byte[] { 1, 0});
		Assert.assertArrayEquals(new byte[] {1, 0}, sup.getCurrentETag());
		Assert.assertArrayEquals(new byte[] {1, 1}, sup.nextETag());
	}
	
}

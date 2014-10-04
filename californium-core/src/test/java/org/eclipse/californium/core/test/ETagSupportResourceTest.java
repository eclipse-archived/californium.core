package org.eclipse.californium.core.test;

import java.util.Arrays;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.ConcurrentCoapResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ETagSupportResourceTest {

	public static final String TARGET = "test";
	
	private CoapServer server;
	private int serverPort;
	
	private TestResource testResource;
	
	@Before
	public void startupServer() throws Exception {
		System.out.println("\nStart "+getClass().getSimpleName());

		CoAPEndpoint serverEndpoint = new CoAPEndpoint();
		server = new CoapServer();
		server.addEndpoint(serverEndpoint);
		server.add(testResource = new TestResource(TARGET));
		server.start();
		
		serverPort = serverEndpoint.getAddress().getPort();
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test() {
		String currentContent = "AAAA";
		testResource.setContent(currentContent);
		
		// First GET request expects a 2.05 response with an ETag.
		CoapClient client = new CoapClient("coap://localhost:"+serverPort+"/"+TARGET);
		client.setTimeout(100);
		
		CoapResponse res1 = client.get();
		Assert.assertNotNull(res1);
		Assert.assertEquals(ResponseCode.CONTENT, res1.getCode());
		Assert.assertEquals(currentContent, res1.getResponseText());
		Assert.assertTrue(res1.getOptions().getETagCount() > 0);
		
		// Second request validates that the ETag from the first response is still valid.
		byte[] etag1 = res1.getOptions().getETags().get(0);
		CoapResponse res2 = client.validate(etag1);
		Assert.assertNotNull(res2);
		Assert.assertEquals(ResponseCode.VALID, res2.getCode());
		Assert.assertTrue(res2.getResponseText().isEmpty());
		Assert.assertTrue(res2.getOptions().getETagCount() > 0);
		Assert.assertArrayEquals(etag1, res2.getOptions().getETags().get(0));
		
		// The content changes
		currentContent = "BBBB";
		testResource.setContent(currentContent);
		
		// The third request attempts to validate that the ETag from the first response is still valid.
		// However, since the content has changed, the server responds a 2.05 with the new content and a new ETag.
		CoapResponse res3 = client.validate(etag1);
		Assert.assertNotNull(res3);
		Assert.assertEquals(ResponseCode.CONTENT, res3.getCode());
		Assert.assertEquals(currentContent, res3.getResponseText());
		Assert.assertTrue(res3.getOptions().getETagCount() > 0);
		Assert.assertFalse(Arrays.equals(etag1, res3.getOptions().getETags().get(0))); // has another ETag
		
		// The forth request validates the ETags from the first and third response.
		// The server responds that the third ETag is still valid.
		byte[] etag3 = res3.getOptions().getETags().get(0);
		CoapResponse res4 = client.validate(etag1, etag3);
		Assert.assertNotNull(res4);
		Assert.assertEquals(ResponseCode.VALID, res4.getCode());
		Assert.assertTrue(res4.getResponseText().isEmpty());
		Assert.assertTrue(res4.getOptions().getETagCount() > 0);
		Assert.assertArrayEquals(etag3, res4.getOptions().getETags().get(0));
		
	}
	
	private static class TestResource extends ConcurrentCoapResource {
		
		private String content;
		
		public TestResource(String name) {
			super(name, ConcurrentCoapResource.SINGLE_THREADED);
		}
		
		@Override
		public void handleGET(CoapExchange exchange) {
			exchange.setETag(getETagSupport().getCurrentETag());
			exchange.respond(content);
		}
		
		public void setContent(final String newContent) {
			// This Changes the content and update the ETag. This is executed by
			// the same thread that also handles requests so that there are no
			// race conditions!
			execute(new Runnable() {
				public void run() {
					content = newContent;
					getETagSupport().nextETag();
				}
			});
		}
	}
}

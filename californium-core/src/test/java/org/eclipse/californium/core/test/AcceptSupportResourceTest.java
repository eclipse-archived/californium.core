package org.eclipse.californium.core.test;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AcceptSupportResourceTest {

	public static final String RESPONSE_PAYLOAD = "some content";
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
		// First GET request expects an HTML format. The resource grants anything.
		CoapClient client = new CoapClient("coap://localhost:"+serverPort+"/"+TARGET);
		client.setTimeout(100);
		
		CoapResponse res1 = client.get(MediaTypeRegistry.TEXT_HTML);
		Assert.assertNotNull(res1);
		Assert.assertEquals(ResponseCode.CONTENT, res1.getCode());
		Assert.assertEquals(RESPONSE_PAYLOAD, res1.getResponseText());
		
		// From now on, the resource only provides the following two formats
		testResource.getAcceptSupport().addAcceptable(MediaTypeRegistry.TEXT_CSV);
		testResource.getAcceptSupport().addAcceptable(MediaTypeRegistry.APPLICATION_XML);
		
		// The second GET request expects a format unsupported by the resource
		CoapResponse res2 = client.get(MediaTypeRegistry.TEXT_HTML);
		Assert.assertNotNull(res2);
		Assert.assertEquals(ResponseCode.NOT_ACCEPTABLE, res2.getCode());
		Assert.assertEquals("", res2.getResponseText());
		
		// The third GET request expects a supported format
		CoapResponse res3 = client.get(MediaTypeRegistry.APPLICATION_XML);
		Assert.assertNotNull(res3);
		Assert.assertEquals(ResponseCode.CONTENT, res3.getCode());
		Assert.assertEquals(RESPONSE_PAYLOAD, res3.getResponseText());
	}
	
	private static class TestResource extends CoapResource {
		
		public TestResource(String name) {
			super(name);
		}
		
		@Override
		public void handleGET(CoapExchange exchange) {
			exchange.respond(RESPONSE_PAYLOAD);
		}
	}
}

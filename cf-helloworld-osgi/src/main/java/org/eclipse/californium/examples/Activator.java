/*******************************************************************************
 * Copyright (c) 2014 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Example of BundleActivator registering 2 Resource services
 * @author Didier Donsez
 *
 */

public class Activator implements BundleActivator {

	private final static Logger LOGGER = Logger.getLogger(Activator.class.getCanonicalName());

	HelloResource bob;
	HelloResource alice;
	
	@Override
	public void start(BundleContext context) throws Exception {
		LOGGER.fine("Example activator is starting");

		try {
			// GET coap://0.0.0.0:5683/Bob (with Copper plugin)
			bob=new HelloResource("Bob");
			bob.start(context);
		} catch (Exception e) {
			bob=null;
			e.printStackTrace();
		}
		
		try {
			// GET coap://0.0.0.0:5683/Alice (with Copper plugin)
			alice=new HelloResource("Alice");
			alice.start(context);
		} catch (Exception e) {
			alice=null;
			e.printStackTrace();
		}
		LOGGER.fine("Example activator is started");

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOGGER.fine("Example activator is stopping");

		try {
			if(alice!=null) alice.stop(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		alice=null;

		try {
			if(bob!=null) bob.stop(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bob=null;
		LOGGER.fine("Example activator is stopped");

	}
	
    /*
     * Definition of the Hello-World Resource
     */
    class HelloResource extends CoapResource implements BundleActivator /*, ManagedService */ {
        
    	private String name;
    	
        public HelloResource(String name) {
            
            // set resource identifier
            super(name);

        	this.name=name;
        	            
            // set display name
            getAttributes().setTitle(name+" Resource");
        }
        
        @Override
        public void handleGET(CoapExchange exchange) {            
            // respond to the request
            exchange.respond("Hello "+name+"!");
			LOGGER.fine(name+" Resource GET is invoked");
        }
        
        ServiceRegistration<Resource> serviceRegistration;

		@Override
		public void start(BundleContext context) throws Exception {
			LOGGER.fine(name+" Resource is starting");
			serviceRegistration=(ServiceRegistration<Resource>) context.registerService(Resource.class.getName(),this,null);
			LOGGER.fine(name+" Resource is registered");
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			LOGGER.fine(name+" Resource is stopping");
			if(serviceRegistration!=null) serviceRegistration.unregister();
			LOGGER.fine(name+" Resource is unregistered");
		}
    }
}

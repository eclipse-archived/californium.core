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
package org.eclipse.californium.osgi;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.ServerInterface;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;


/**
 * Example of BundleActivator to start and stop a managed server
 * @author Didier Donsez
 *
 */

public class Activator implements BundleActivator {
	
	private final static Logger LOGGER = Logger.getLogger(ManagedServer.class.getCanonicalName());

	ServerInterfaceFactory serverFactory;
	ServerInterface server;
	ManagedServer managedServer;
	BundleContext bundleContext;
	EndpointFactory endpointFactory;
	EndpointFactory secureEndpointFactory;
	List<Endpoint> endpointList = new LinkedList<Endpoint>();
	Endpoint standardEndpoint;
	Endpoint secureEndpoint;
	
    ServiceRegistration<ManagedService> serviceRegistration;
	
	InetSocketAddress standardAddress = new InetSocketAddress(EndpointManager.DEFAULT_COAP_PORT);
	InetSocketAddress secureAddress = new InetSocketAddress(EndpointManager.DEFAULT_COAP_PORT);
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		LOGGER.fine("managed server instance is starting");

		this.bundleContext=bundleContext;
		
		serverFactory = new ServerInterfaceFactory() {
			
			@Override
			public ServerInterface newServer(NetworkConfig config) {
				return newServer(config, EndpointManager.DEFAULT_COAP_PORT);
			}

			@Override
			public ServerInterface newServer(NetworkConfig config, int... ports) {
				for (int port : ports) {
					if (port == standardAddress.getPort()) {
						endpointList.add(standardEndpoint);
					} else if (port == secureAddress.getPort()) {
						endpointList.add(secureEndpoint);
					}
				}
				return server;
			}
		};

		endpointFactory = new EndpointFactory() {
			
			@Override
			public Endpoint getSecureEndpoint(NetworkConfig config,
					InetSocketAddress address) {
				return null;
			}
			
			@Override
			public Endpoint getEndpoint(NetworkConfig config, InetSocketAddress address) {
				return standardEndpoint;
			}
		};
		//managedServer = new ManagedServer(bundleContext, serverFactory, endpointFactory);
		managedServer = new ManagedServer(bundleContext, endpointFactory);

		try {
			java.util.Dictionary properties=new java.util.Hashtable();
			properties.put(org.osgi.framework.Constants.SERVICE_PID, ManagedServer.class.getName());
			serviceRegistration=(ServiceRegistration<ManagedService>) bundleContext.registerService(ManagedService.class.getName(),managedServer,properties);			
		} catch (Exception e) {
			LOGGER.warning("Could not register the ManagedService service of managed server instance");
			throw e;
		}
		LOGGER.fine("managed server instance is started");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(serviceRegistration!=null) serviceRegistration.unregister();
		managedServer.stop();
		managedServer = null;
		LOGGER.fine("managed server instance is stopped");
	}	
}

/*******************************************************************************
 * Copyright (c) 2014 Wireless Networks Group, UPC Barcelona and i2CAT.
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
 *    August Betzler    - CoCoA implementation
 *    Matthias Kovatsch - Embedding of CoCoA in Californium
 ******************************************************************************/

package org.eclipse.californium.core.network.stack;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.RemoteEndpoint;
import org.eclipse.californium.core.network.RemoteEndpoint.BucketElement;
import org.eclipse.californium.core.network.RemoteEndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfigDefaults;
import org.eclipse.californium.core.network.stack.congestioncontrol.*;
import org.hamcrest.core.IsInstanceOf;

/**
 * The optional Congestion Control (CC) Layer for the Californium CoAP
 * implementation provides the methods for advanced congestion control
 * mechanisms. The RTO calculations and other mechanisms are implemented in the
 * correspondent child classes. The alternatives to CoCoA are implemented for
 * testing purposes and are not maintained/updated.
 * 
 * BASICRTO = Use previously measured RTT and mutliply it by 1.5 to calculate
 * the RTO for the next transmission COCOA = CoCoA algorithm as defined in
 * draft-bormann-cocoa-02 LINUXRTO = The Linux RTO calculation mechanism
 * COCOASTRONG = CoCoA but only with the strong estimator PEAKHOPPERRTO = The
 * Peakhopper RTO calculation mechanism (PH-RTO)
 * 
 * @author augustbetzler
 * 
 */

public abstract class CongestionControlLayer extends ReliabilityLayer {

	/** The configuration */
	protected NetworkConfig config;

	// Maximum duration of a transaction, after that, sweep the exchanges
	private final static long MAX_REMOTE_TRANSACTION_DURATION = 255 * 1000;
	// Amount of non-confirmables that can be transmitted before a NON is converted to a CON (to get an RTT measurement); this is a CoCoA feature
	private final static int MAX_SUCCESSIVE_NONS = 7;

	protected final static int OVERALLRTOTYPE = 0;
	protected final static int STRONGRTOTYPE = 1;
	protected final static int WEAKRTOTYPE = 2;
	protected final static int NOESTIMATOR = 3;

	// An upper limit for the queue size of confirmables and non-confirmables
	// (separate queues)
	private final static int EXCHANGELIMIT = 50;
	private final static int MAX_RTO = 60000;
	
	// In CoAP, dithering is applied to the initial RTO of a transmission; set
	// to true to apply dithering
	private boolean appliesDithering;

	private RemoteEndpointManager remoteEndpointmanager;

	/**
	 * Constructs a new congestion control layer.
	 * 
	 * @param config
	 *            the configuration
	 */
	public CongestionControlLayer(NetworkConfig config) {
		super(config);
		this.config = config;
		this.remoteEndpointmanager = new RemoteEndpointManager(config);
		setDithering(false);
	}

	protected RemoteEndpoint getRemoteEndpoint(Exchange exchange) {
		return remoteEndpointmanager.getRemoteEndpoint(exchange);
	}

	public boolean appliesDithering() {
		return appliesDithering;
	}

	public void setDithering(boolean mode) {
		this.appliesDithering = mode;
	}
	/*
	 * Method called when receiving a Response/Request from the upper layers:
	 * 1.) Checks if message is a non-confirmable. If so, it is added to the
	 * non-confirmable queue and in case the bucket thread is not running, it is
	 * started 2.) Checks if message is confirmable and if the NSTART rule is
	 * followed. If more than NSTART exchanges are running, the Request is
	 * enqueued. If the NSTART limit is respected, the message is passed on to
	 * the reliability layer.
	 */
	private boolean processMessage(Exchange exchange, Message message) {

		Type messageType = message.getType();

		// Put into queues for NON or CON messages
		if (messageType == Type.CON) {
			// Check if NSTART is not reached yet for confirmable transmissions
			if (!checkNSTART(exchange)) {
				return false;
			}

		} else if (messageType == Type.NON) {
			if (getRemoteEndpoint(exchange).getNonConfirmableCounter() > MAX_SUCCESSIVE_NONS) {
				// Every MAX_SUCCESSIVE_NONS + 1 packets, a non-confirmable needs to be converted to a confirmable [CoCoA]
				message.setType(Type.CON);
				getRemoteEndpoint(exchange).resetNonConfirmableCounter();

				// Check if NSTART is not reached yet for confirmable transmissions
				if (!checkNSTART(exchange)) {
					return false;
				}
			} else {
				// Check of if there's space to queue a NON
				if (getRemoteEndpoint(exchange).getNonConfirmableQueue().size() == EXCHANGELIMIT) {
					// System.out.println("Non-confirmable exchange queue limit reached!");
					// TODO: Drop packet -> Notify upper layers?
				} else {
					getRemoteEndpoint(exchange).registerBucketElement(exchange, message);
					// Check if NONs are already processed, if not, start bucket Thread
					if (!getRemoteEndpoint(exchange).getProcessingNON()) {
						executor.schedule(new bucketThread(getRemoteEndpoint(exchange)), 0, TimeUnit.MILLISECONDS);
					}
				}
				return false;
			}
		}
		return true;
	}

	/*
	 * Check if the limit of exchanges towards the remote endpoint has reached
	 * NSTART.
	 */
	private boolean checkNSTART(Exchange exchange) {
		getRemoteEndpoint(exchange).checkForDeletedExchanges();
		if (getRemoteEndpoint(exchange).getNumberOfOngoingExchanges(exchange) < config.getInt("NSTART")) {
			// System.out.println("Processing exchange (NSTART OK!)");

			// NSTART allows to start the exchange, proceed normally
			getRemoteEndpoint(exchange).registerExchange(exchange, calculateVBF(getRemoteEndpoint(exchange).getRTO()));

			// The exchange needs to be deleted after at least 255 s TODO:
			// should this value be calculated dynamically ?
			executor.schedule(new SweepCheckTask(getRemoteEndpoint(exchange), exchange), MAX_REMOTE_TRANSACTION_DURATION, TimeUnit.MILLISECONDS);
			return true;
		} else {
			// NSTART does not allow any further parallel exchanges towards the remote endpoint
			// System.out.println("Nstart does not allow further exchanges with "
			// + getRemoteEndpoint(exchange).getRemoteAddress().toString());

			// Check if the queue limit for exchanges is already reached
			if (getRemoteEndpoint(exchange).getConfirmableQueue().size() == EXCHANGELIMIT) {
				// Request cannot be queued TODO: does this trigger some feedback for other layers?
				// System.out.println("Confirmable exchange queue limit reached! Message dropped...");

			} else {
				// Queue exchange in the CON-Queue
				getRemoteEndpoint(exchange).getConfirmableQueue().add(exchange);
				// System.out.println("Added exchange to the queue (NSTART limit reached)");
			}
		}
		return false;
	}

	/*
	 * When a response or an ACK was received, update the RTO values with the
	 * measured RTT.
	 */
	private void calculateRTT(Exchange exchange) {
		long timestamp, measuredRTT;
		timestamp = getRemoteEndpoint(exchange).getExchangeTimestamp(exchange);
		if (timestamp != 0) {
			measuredRTT = System.currentTimeMillis() - timestamp;
			// process the RTT measurement
			processRTTmeasurement(measuredRTT, exchange, exchange.getFailedTransmissionCount());
			getRemoteEndpoint(exchange).removeExchangeInfo(exchange);
		}
	}

	/**
	 * Received a new RTT measurement, evaluate it and update correspondent
	 * estimators
	 * 
	 * @param measuredRTT
	 *            the round-trip time of a CON-ACK pair
	 * @param exchange
	 *            the exchange that was used for the RTT measurement
	 * @param retransmissionCount
	 *            the number of retransmissions that were applied to the
	 *            transmission of the CON message
	 */
	protected void processRTTmeasurement(long measuredRTT, Exchange exchange, int retransmissionCount) {
		// Default CoAP does not use RTT info, so do nothing
		return;
	}

	/**
	 * Override this method in RTO algorithms that implement some sort of RTO
	 * aging
	 * 
	 * @param exchange
	 */
	protected void checkAging(Exchange exchange) {
		return;
	}

	/**
	 * This method is only called if there hasn't been an RTO update yet.
	 * Initializes the
	 * 
	 * @param measuredRTT
	 *            the time it took to get an ACK for a CON message
	 * @param estimatorType
	 *            the type indicating if the measurement was a strong or a weak
	 *            one
	 * @param endpoint
	 *            the Remote Endpoint for which the RTO update is done
	 */
	protected void initializeRTOEstimators(long measuredRTT, int estimatorType, RemoteEndpoint endpoint) {
		final long newRTO = config.getInt(NetworkConfigDefaults.ACK_TIMEOUT);

		endpoint.updateRTO(newRTO);
	}

	/**
	 * If the RTO estimator already has been used previously, this function
	 * takes care of updating it according to the new RTT measurement (or other
	 * trigger for non-CoCoA algorithms)
	 * 
	 * @param measuredRTT
	 *            Time it took to get an ACK for a CON message
	 * @param estimatorType
	 *            Estimatortype indicates if the measurement was a strong or a
	 *            weak one
	 * @param endpoint
	 *            The Remote Endpoint for which the RTO update is done
	 */
	protected void updateEstimator(long measuredRTT, int estimatorType, RemoteEndpoint endpoint) {
		// Default CoAP always uses the default timeout
		long newRTO = config.getInt(NetworkConfigDefaults.ACK_TIMEOUT);
		endpoint.updateRTO(newRTO);
	}

	/**
	 * Calculates the Backoff Factor for the retransmissions. By default this is
	 * a binary backoff (= 2)
	 * 
	 * @param rto
	 *            the initial RTO value
	 * @return the new VBF
	 */
	protected double calculateVBF(long rto) {
		return config.getInt(NetworkConfigDefaults.ACK_TIMEOUT_SCALE);
	}

	/*
	 * Gets a request or response from the dedicated queue and polls it
	 */
	private void checkRemoteEndpointQueue(Exchange exchange) {
		// 0 = empty queue | 1 = response | 2 = request
		if (!getRemoteEndpoint(exchange).getConfirmableQueue().isEmpty()) {
			// We have some exchanges that need to be processed; is it a response or a request?
			Exchange queuedExchange = getRemoteEndpoint(exchange).getConfirmableQueue().poll();
			if (queuedExchange.getCurrentResponse() != null) {
				// it's a response
				sendResponse(queuedExchange, queuedExchange.getCurrentResponse());
			} else if (queuedExchange.getCurrentRequest() != null) {
				// it's a request
				sendRequest(queuedExchange, queuedExchange.getCurrentRequest());
			}
		}
	}

	/**
	 * Forward the request to the lower layer.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param request
	 *            the current request
	 */
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		// Check if exchange is already running into a retransmission; if so,
		// don't call processMessage
		if (exchange.getFailedTransmissionCount() > 0) {
			super.sendRequest(exchange, request);
		} else if (processMessage(exchange, request)) {
			checkAging(exchange);
			super.sendRequest(exchange, request);
		}
	}

	/**
	 * Forward the response to the lower layer.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param response
	 *            the current response
	 */
	@Override
	public void sendResponse(Exchange exchange, Response response) {
		// Check if exchange is already running into a retransmission; if so, don't call processMessage, since this is a retransmission
		if (exchange.getFailedTransmissionCount() > 0) {
			super.sendResponse(exchange, response);
		} else if (processMessage(exchange, response)) {
			checkAging(exchange);
			super.sendResponse(exchange, response);
		}
	}

	/**
	 * The following method overrides the method provided by the reliability
	 * layer to include the advanced RTO calculation values when determining the
	 * RTO.
	 */
	@Override
	protected void prepareRetransmission(Exchange exchange, RetransmissionTask task) {
		int timeout;
		// System.out.println("TXCount: " + exchange.getFailedTransmissionCount());
		if (exchange.getFailedTransmissionCount() == 0) {
			timeout = (int) getRemoteEndpoint(exchange).getRTO();
			if (appliesDithering()) {
				// RTO algorithms with dithering don't reuse backed-off RTO values, reset the RTO to the estimated value
				getRemoteEndpoint(exchange).matchCurrentRTO();
				timeout = (int) getRemoteEndpoint(exchange).getRTO();
				// Apply dithering by randomly choosing RTO from [RTO, RTO * 1.5]
				float ack_random_factor = config.getFloat(NetworkConfigDefaults.ACK_RANDOM_FACTOR);
				timeout = getRandomTimeout(timeout, (int) (timeout * ack_random_factor));
			}
			// System.out.println("meanrto:" + timeout + ";" +
			// System.currentTimeMillis());
		} else {
			int tempTimeout = (int) (getRemoteEndpoint(exchange).getExchangeVBF(exchange) * exchange.getCurrentTimeout());
			timeout = (tempTimeout < MAX_RTO) ? tempTimeout : MAX_RTO;
			getRemoteEndpoint(exchange).setCurrentRTO(timeout);
			// System.out.println("RTX");
		}
		exchange.setCurrentTimeout(timeout);
		ScheduledFuture<?> f = executor.schedule(task, timeout, TimeUnit.MILLISECONDS);
		exchange.setRetransmissionHandle(f);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		// August: change the state of the remote endpoint
		// (STRONG/WEAK/NOESTIMATOR) if failedTransmissionCount = 0;
		if (exchange.getFailedTransmissionCount() != 0) {
			getRemoteEndpoint(exchange).setEstimatorState(exchange);
		}
		super.receiveResponse(exchange, response);

		calculateRTT(exchange);
		checkRemoteEndpointQueue(exchange);
	}

	/**
	 * If we receive an ACK or RST, calculate the RTT and update the RTO values
	 */
	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		// If retransmissions were used, update the estimator state (WEAK / NO)
		if (exchange.getFailedTransmissionCount() != 0) {
			getRemoteEndpoint(exchange).setEstimatorState(exchange);
		}
		super.receiveEmptyMessage(exchange, message);

		calculateRTT(exchange);
		checkRemoteEndpointQueue(exchange);
	}

	/**
	 * Methods to send NON packets chosen by the bucket Thread (no reliability)
	 */
	public void sendBucketRequest(Exchange exchange, Request request) {
		super.sendRequest(exchange, request);
	}

	public void sendBucketResponse(Exchange exchange, Response response) {
		super.sendResponse(exchange, response);
	}

	/*
	 * This Thread is used to apply rate control to non-confirmables by polling
	 * them from the queue and scheduling the task to run again later.
	 */
	private  class bucketThread implements Runnable {

		RemoteEndpoint endpoint;

		public bucketThread(RemoteEndpoint endpoint) {
			this.endpoint = endpoint;
		}

		@Override
		public void run() {
			if (!endpoint.getNonConfirmableQueue().isEmpty()) {
				endpoint.setProcessingNON(true);

				BucketElement bucketElement = endpoint.getBucketElement();//.getExchange();
				
				
				if (endpoint.getNonConfirmableCounter() <= MAX_SUCCESSIVE_NONS) {
					endpoint.increaseNonConfirmableCounter();
					// FIXME: Is there a cheaper way to determine whether it is a Request/Response?
					if (bucketElement.getMessage() instanceof Request){
						// it's a request
						sendBucketRequest(bucketElement.getExchange(), (Request) bucketElement.getMessage());
					} else {
						// it's a response
						sendBucketResponse(bucketElement.getExchange(), (Response) bucketElement.getMessage());
					}
					
				}
				// schedule next transmission of a NON based on the RTO value (rate = 1/RTO)
				executor.schedule(new bucketThread(endpoint), endpoint.getRTO(), TimeUnit.MILLISECONDS);

			} else {
				endpoint.setProcessingNON(false);
			}			
		}
	}

	/*
	 * Task that deletes old exchanges from the remote endpoint list
	 */
	private class SweepCheckTask implements Runnable {

		RemoteEndpoint endpoint;
		Exchange exchange;

		public SweepCheckTask(RemoteEndpoint endpoint, Exchange exchange) {
			this.endpoint = endpoint;
			this.exchange = exchange;
		}

		@Override
		public void run() {
			if (endpoint.removeExchangeInfo(exchange) == false) {
				// The entry already was removed
			} else {
				// Entry was removed, check if there are more messages in the
				// queue
				checkRemoteEndpointQueue(exchange);
			}
		}
	}

	public static CongestionControlLayer newImplementation(NetworkConfig config) {
		final String implementation = config.getString(NetworkConfigDefaults.CONGESTION_CONTROL_ALGORITHM);
		if ("Cocoa".equals(implementation))
			return new Cocoa(config);
		else if ("CocoaStrong".equals(implementation))
			return new CocoaStrong(config);
		else if ("BasicRto".equals(implementation))
			return new BasicRto(config);
		else if ("LinuxRto".equals(implementation))
			return new LinuxRto(config);
		else if ("PeakhopperRto".equals(implementation))
			return new PeakhopperRto(config);
		else {
			LOGGER.config("Unknown CONGESTION_CONTROL_ALGORITHM (" + implementation + "), using Cocoa");
			return new Cocoa(config);
		}
	}
}

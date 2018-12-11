/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageConstants;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.jsonrpc.services.GenericEndpoint;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


/**
 * @author beyang
 *
 */
public class WebSocketLanguageServer extends WebSocketServer {

	private static final Logger LOG = Logger.getLogger(WebSocketLanguageServer.class.getName());

	private MessageJsonHandler jsonHandler;
	private Map<WebSocket, Endpoint> endpoints = new LinkedHashMap<>();

	public WebSocketLanguageServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));

		Map<String, JsonRpcMethod> supportedMethods = new LinkedHashMap<>();
		supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient.class));
		supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(JDTLanguageServer.class));
		this.jsonHandler = new MessageJsonHandler(supportedMethods);
		//		System.err.println(supportedMethods);
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onStart()
	 */
	@Override
	public void onStart() {
		LOG.info("Language server started");
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onOpen(org.java_websocket.WebSocket, org.java_websocket.handshake.ClientHandshake)
	 */
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		PreferenceManager preferenceManager = new PreferenceManager();
		ProjectsManager projectsManager = new ProjectsManager(preferenceManager);
		JDTLanguageServer ls = new JDTLanguageServer(projectsManager, preferenceManager);

		GenericEndpoint localEndpoint = new GenericEndpoint(Collections.singleton(ls));

		// TODO(beyang): why does adding this in cause things to hang (i.e., no hovers)
		//		MessageConsumer out = new MessageConsumer() {
		//			@Override
		//			public void consume(Message arg0) throws MessageIssueException, JsonRpcException {
		//				LOG.log(Level.INFO, "sending message: " + arg0);
		//				conn.send(arg0.toString());
		//			}
		//		};
		//		RemoteEndpoint remote = new RemoteEndpoint(out, localEndpoint);
		//		ls.connectClient(ServiceEndpoints.toServiceObject(remote, JavaLanguageClient.class));

		this.endpoints.put(conn, localEndpoint);
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onClose(org.java_websocket.WebSocket, int, java.lang.String, boolean)
	 */
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		this.endpoints.remove(conn);
		conn.close();
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onMessage(org.java_websocket.WebSocket, java.lang.String)
	 */
	@Override
	public void onMessage(WebSocket conn, String msg) {
		System.err.println(" ### msg: " + msg);
		Endpoint endpoint = this.endpoints.get(conn);
		Message m = jsonHandler.parseMessage(msg);
		if (m instanceof RequestMessage) {
			LOG.log(Level.INFO, "Request from client {}", m);
			RequestMessage req = (RequestMessage) m;
			Object result = endpoint.request(req.getMethod(), req.getParams()).join();
			ResponseMessage resp = new ResponseMessage();
			resp.setRawId(req.getRawId());
			resp.setJsonrpc(MessageConstants.JSONRPC_VERSION);
			resp.setResult(result);
			conn.send(resp.toString());
		} else if (m instanceof NotificationMessage) {
			LOG.log(Level.INFO, "Notif from client {}", m);
			NotificationMessage notify = (NotificationMessage) m;
			endpoint.notify(notify.getMethod(), notify.getParams());
		} else {
			LOG.log(Level.INFO, "WebSocket message from client of unknown type: {}", msg);
		}
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onError(org.java_websocket.WebSocket, java.lang.Exception)
	 */
	@Override
	public void onError(WebSocket conn, Exception err) {
		LOG.log(Level.WARNING, "WebSocket error: {}", err);
	}
}

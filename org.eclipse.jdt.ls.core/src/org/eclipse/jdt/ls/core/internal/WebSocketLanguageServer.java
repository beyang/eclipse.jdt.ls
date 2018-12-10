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

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


/**
 * @author beyang
 *
 */
public class WebSocketLanguageServer extends WebSocketServer {

	public WebSocketLanguageServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onClose(org.java_websocket.WebSocket, int, java.lang.String, boolean)
	 */
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// TODO Auto-generated method stub
		System.err.println("# close:" + reason);
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onError(org.java_websocket.WebSocket, java.lang.Exception)
	 */
	@Override
	public void onError(WebSocket conn, Exception err) {
		// TODO Auto-generated method stub
		System.err.println("# err: " + err.toString());
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onMessage(org.java_websocket.WebSocket, java.lang.String)
	 */
	@Override
	public void onMessage(WebSocket conn, String msg) {
		// TODO Auto-generated method stub
		System.err.println("# message: " + msg);
	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onOpen(org.java_websocket.WebSocket, org.java_websocket.handshake.ClientHandshake)
	 */
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.err.println("Welcome to the server!");
		// TODO Auto-generated method stub


	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onStart()
	 */
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		System.err.println("# onStart");
	}

}

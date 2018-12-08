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
	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onError(org.java_websocket.WebSocket, java.lang.Exception)
	 */
	@Override
	public void onError(WebSocket arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onMessage(org.java_websocket.WebSocket, java.lang.String)
	 */
	@Override
	public void onMessage(WebSocket arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onOpen(org.java_websocket.WebSocket, org.java_websocket.handshake.ClientHandshake)
	 */
	@Override
	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		System.err.println("Welcome to the server!");
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.java_websocket.server.WebSocketServer#onStart()
	 */
	@Override
	public void onStart() {
		// TODO Auto-generated method stub

	}

}

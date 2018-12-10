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

import java.io.IOException;

import javax.xml.ws.Endpoint;

/**
 * @author beyang
 *
 */
public class WebSocketEndpoint extends Endpoint {
	@Override
	public void onOpen(final Session session, EndpointConfig config) {
		session.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				try {
					session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}

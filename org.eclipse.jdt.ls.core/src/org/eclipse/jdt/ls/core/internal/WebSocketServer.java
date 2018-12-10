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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.maven.settings.Server;

/**
 * @author beyang
 *
 */
public class WebSocketServer {
	public static void runServer() {
		// NEXT: add the dependencies: Step 2 in https://blog.openshift.com/how-to-build-java-websocket-applications-using-the-jsr-356-api/

		Server server = new Server("localhost", 8025, "/websockets", WordgameServerEndpoint.class);

		try {
			server.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Please press a key to stop the server.");
			reader.readLine();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			server.stop();
		}
	}
}

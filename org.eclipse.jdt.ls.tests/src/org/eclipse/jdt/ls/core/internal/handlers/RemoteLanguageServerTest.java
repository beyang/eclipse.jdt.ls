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
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.junit.Test;

/**
 * @author beyang
 *
 */
public class RemoteLanguageServerTest {

	@Test
	public void testRewriteUris() {
		Function<String, String> rewriter = (uri) -> uri != null ? "MODIFIED" : null;

		TextDocumentPositionParams tdpParams = new TextDocumentPositionParams(new TextDocumentIdentifier("https://foo.com"), new Position());
		RemoteLanguageServer.rewriteUris(tdpParams, rewriter);
		TextDocumentPositionParams expTDPParams = new TextDocumentPositionParams(new TextDocumentIdentifier("MODIFIED"), new Position());
		assertEquals(expTDPParams, tdpParams);

		List<? extends Location> locs = Arrays.asList(new Location("file:///foo", new Range()), new Location("file:///foo", new Range()));
		RemoteLanguageServer.rewriteUris(locs, rewriter);
		List<? extends Location> expLocs = Arrays.asList(new Location("MODIFIED", new Range()), new Location("MODIFIED", new Range()));
		assertEquals(expLocs, locs);
	}
}

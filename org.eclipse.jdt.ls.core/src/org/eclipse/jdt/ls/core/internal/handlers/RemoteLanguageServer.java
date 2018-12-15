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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jdt.ls.core.internal.BuildWorkspaceStatus;
import org.eclipse.jdt.ls.core.internal.lsp.JavaProtocolExtensions;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * @author beyang
 *
 */
public class RemoteLanguageServer implements LanguageServer, TextDocumentService, WorkspaceService, JavaProtocolExtensions {

	private JDTLanguageServer underlying;

	// cacheRoot is the root directory of the local cache of source files.
	private File cacheRoot;

	private ConcurrentMap<String, String> uriToPath;
	private ConcurrentMap<String, String> pathToUri;

	public RemoteLanguageServer(JDTLanguageServer underlying, File cacheRoot) {
		this.underlying = underlying;
		this.cacheRoot = cacheRoot;
		this.uriToPath = new ConcurrentHashMap<>();
		this.pathToUri = new ConcurrentHashMap<>();
	}

	private String uriToCachePath(String uri) {
		try {
			URL url = new URL(uri);
			return Paths.get(cacheRoot.toPath().toString(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		try {
			String rootUri = params.getRootUri();
			String rootCachePath = uriToCachePath(rootUri);
			this.uriToPath.put(rootUri, rootCachePath);
			this.pathToUri.put(rootCachePath, rootUri);

			// TODO: try-with-resources block (so things are closed properly)
			InputStream respBody = HTTPUtils.httpGet("http://localhost:3080/github.com/gorilla/mux/-/raw");

			// TODO: unzip to temp folder, then move to rootCachePath

			//			String respBodyString = IOUtils.toString(respBody, "UTF-8");
			//			System.err.println("################");
			//			System.err.println(respBodyString);

			ZipInputStream zipIn = new ZipInputStream(respBody);
			byte[] buffer = new byte[1024];
			for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
				if (entry.isDirectory()) {
					new File(rootCachePath + File.separator + entry.getName()).mkdirs();
					continue;
				}
				File newFile = new File(rootCachePath + File.separator + entry.getName());
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zipIn.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}

			return underlying.initialize(params);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		// TODO(beyang): cleanup
		return underlying.shutdown();
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		return underlying.hover(position);
	}

	@Override
	public CompletableFuture<String> classFileContents(TextDocumentIdentifier documentUri) {
		return underlying.classFileContents(documentUri);
	}

	@Override
	public void projectConfigurationUpdate(TextDocumentIdentifier documentUri) {
		underlying.projectConfigurationUpdate(documentUri);
	}

	@Override
	public CompletableFuture<BuildWorkspaceStatus> buildWorkspace(boolean forceReBuild) {
		return underlying.buildWorkspace(forceReBuild);
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		underlying.didChangeConfiguration(params);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		underlying.didChangeWatchedFiles(params);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		underlying.didOpen(params);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		underlying.didChange(params);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		underlying.didClose(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		underlying.didSave(params);
	}
	@Override
	public void exit() {
		underlying.exit();
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return underlying.getTextDocumentService();
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return underlying.getWorkspaceService();
	}

}

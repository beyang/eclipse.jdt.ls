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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
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
	private File cacheContainer;

	private ConcurrentMap<String, String> uriToPath;
	private ConcurrentMap<String, String> pathToUri;

	public RemoteLanguageServer(JDTLanguageServer underlying, File cacheContainer) {
		this.underlying = underlying;
		this.cacheContainer = cacheContainer;
		this.uriToPath = new ConcurrentHashMap<>();
		this.pathToUri = new ConcurrentHashMap<>();
	}

	private String uriToCachePath(String uri) {
		if (uri == null) {
			return null;
		}
		try {
			URL url = new URL(uri);
			//			return Paths.get(cacheRootDir(), url.getProtocol(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
			return Paths.get(cacheRootDir(), url.getProtocol(), url.getHost(), url.getPath().replace("/", File.separator)).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String remoteToLocalUri(String remote) {
		if (remote == null) {
			return null;
		}
		return "file://" + uriToCachePath(remote).replace(File.separator, "/");
	}

	private String localToRemoteUri(String local) {
		if (local == null) {
			return null;
		}
		String path = StringUtils.replace(StringUtils.removeStart(local, "file://"), "/", File.separator);
		Path subpath = Paths.get(StringUtils.removeStart(StringUtils.removeStart(path, cacheRootDir()), File.separator));
		if (subpath.getNameCount() < 2) {
			throw new RuntimeException("TODO");
		}
		String protocol = subpath.getName(0).toString();
		String remainder = StringUtils.removeStart(subpath.toString(), protocol + File.separator);
		return protocol + "://" + StringUtils.replace(remainder, File.separator, "/");
	}

	private String cacheTmpDir() {
		return Paths.get(cacheContainer.toString(), "tmp").toString();
	}

	private String cacheRootDir() {
		return Paths.get(cacheContainer.toString(), "root").toString();
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		try {
			String rootUri = params.getRootUri();
			String rootCachePath = uriToCachePath(rootUri);

			if (!uriToPath.containsKey(rootUri)) {
				// TODO(beyang): make async
				fetchTree(rootUri, rootCachePath);
				this.pathToUri.put(rootCachePath, rootUri);
				this.uriToPath.put(rootUri, rootCachePath);
			}

			return underlying.initialize(params);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void fetchTree(String remoteUri, String localPath) {
		// TODO(beyang): try-with-resources block (so things are closed properly)
		try {
			if (Files.exists(Paths.get(localPath))) {
				return;
			}

			InputStream respBody = HTTPUtils.httpGet(remoteUri);
			new File(cacheTmpDir()).mkdirs();
			Path tmpDir = Files.createTempDirectory(Paths.get(cacheTmpDir()), Paths.get(localPath).getFileName().toString());
			// TODO(beyang): delete tmpDir if still exists

			ZipInputStream zipIn = new ZipInputStream(respBody);
			byte[] buffer = new byte[1024];
			for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
				if (entry.isDirectory()) {
					new File(tmpDir + File.separator + entry.getName()).mkdirs();
					continue;
				}
				File newFile = new File(tmpDir + File.separator + entry.getName());
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zipIn.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}

			new File(new File(localPath).getParent()).mkdirs();
			Files.move(tmpDir, Paths.get(localPath));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		// TODO(beyang): cleanup
		return underlying.shutdown();
	}

	public static void rewriteUris(Object obj, Function<String, String> modifier) {
		if (obj == null) {
			return;
		}
		// Container cases
		if (obj instanceof Array || obj instanceof Map) {
			// Ignore arrays and maps, because they aren't returned by any
			// of the lsp4j data structure accessors.
			return;
		}
		if (obj instanceof List) {
			for (Object elem : (List<?>) obj) {
				rewriteUris(elem, modifier);
			}
			return;
		}

		Class<?> type = obj.getClass();
		Method[] methods = type.getMethods();
		for (Method m : methods) {
			String methodName = m.getName();
			if (!methodName.startsWith("get") || m.getParameterCount() > 0) {
				continue;
			}
			if (methodName.equals("getClass")) {
				continue;
			}
			Class<?> returnType = m.getReturnType();
			if (returnType.isPrimitive() || returnType.equals(Byte.class) || returnType.equals(Short.class) || returnType.equals(Integer.class) || returnType.equals(Long.class) || returnType.equals(Character.class)
					|| returnType.equals(Float.class)
					|| returnType.equals(Double.class) || returnType.equals(Boolean.class)) {
				continue;
			}
			if ((methodName.contains("Uri") || methodName.contains("URI")) && returnType.equals(String.class)) {
				String setterMethodName = "set" + StringUtils.removeStart(methodName, "get");
				Method setter;
				try {
					setter = type.getMethod(setterMethodName, String.class);
				} catch (NoSuchMethodException e) {
					continue;
				}
				try {
					setter.invoke(obj, modifier.apply((String) m.invoke(obj)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				continue;
			}
			// Object getter
			try {
				rewriteUris(m.invoke(obj), modifier);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		rewriteUris(position, this::remoteToLocalUri);
//		position.getTextDocument().setUri(remoteToLocalUri(position.getTextDocument().getUri()));
		return underlying.hover(position);
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
		//		position.getTextDocument().setUri(remoteToLocalUri(position.getTextDocument().getUri()));
		rewriteUris(position, this::remoteToLocalUri);
		CompletableFuture<List<? extends Location>> defRes = underlying.definition(position);
		return defRes.thenApply(defs -> {
			RemoteLanguageServer.rewriteUris(defs, RemoteLanguageServer.this::localToRemoteUri);
			return defs;
		});
//		return defRes.thenApply(defs ->
//			defs.stream().map(def -> {
//			def.setUri(localToRemoteUri(def.getUri()));
//			return def;
//			}).collect(Collectors.toList())
//		);
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return underlying.references(params);
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

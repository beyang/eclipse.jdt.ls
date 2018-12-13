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

import java.net.URI;

import org.eclipse.core.resources.IFile;

/**
 * @author beyang
 *
 */
public class RemoteWorkspace {
	public IFile[] findFilesForLocationURI(URI location) {
		return new IFile[] { new RemoteFile(location) };
	}
}
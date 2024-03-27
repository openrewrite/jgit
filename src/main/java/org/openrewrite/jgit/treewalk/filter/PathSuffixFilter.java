/*
 * Copyright (C) 2009, 2021 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.openrewrite.jgit.treewalk.filter;

import java.io.IOException;

import org.openrewrite.jgit.errors.IncorrectObjectTypeException;
import org.openrewrite.jgit.errors.MissingObjectException;
import org.openrewrite.jgit.internal.JGitText;
import org.openrewrite.jgit.lib.Constants;
import org.openrewrite.jgit.treewalk.TreeWalk;

/**
 * Includes tree entries only if they end with the configured path (suffix
 * match).
 * <p>
 * For example, <code>PathSuffixFilter.create(".txt")</code> will match all
 * paths ending in <code>.txt</code>.
 * <p>
 * Using this filter is recommended instead of filtering the entries using
 * {@link org.openrewrite.jgit.treewalk.TreeWalk#getPathString()} and
 * <code>endsWith</code> or some other type of string match function.
 */
public class PathSuffixFilter extends TreeFilter {

	/**
	 * Create a new tree filter for a user supplied path suffix.
	 * <p>
	 * Path strings use '/' to delimit directories on all platforms.
	 *
	 * @param path
	 *            the path suffix to filter on. Must not be the empty string.
	 * @return a new filter for the requested path.
	 * @throws java.lang.IllegalArgumentException
	 *             the path supplied was the empty string.
	 */
	public static PathSuffixFilter create(String path) {
		if (path.length() == 0)
			throw new IllegalArgumentException(JGitText.get().emptyPathNotPermitted);
		return new PathSuffixFilter(path);
	}

	final String pathStr;
	final byte[] pathRaw;

	private PathSuffixFilter(String s) {
		pathStr = s;
		pathRaw = Constants.encode(pathStr);
	}

	/** {@inheritDoc} */
	@Override
	public TreeFilter clone() {
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public boolean include(TreeWalk walker) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (walker.isSubtree()) {
			return true;
		}
		return walker.isPathSuffix(pathRaw, pathRaw.length);

	}

	@Override
	public int matchFilter(TreeWalk walker) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (walker.isSubtree()) {
			return -1;
		}
		return super.matchFilter(walker);
	}

	/** {@inheritDoc} */
	@Override
	public boolean shouldBeRecursive() {
		return true;
	}

}

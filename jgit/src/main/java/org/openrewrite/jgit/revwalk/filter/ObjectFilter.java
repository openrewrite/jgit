/**
 * Copyright (C) 2015, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.openrewrite.jgit.revwalk.filter;

import java.io.IOException;

import org.openrewrite.jgit.errors.IncorrectObjectTypeException;
import org.openrewrite.jgit.errors.MissingObjectException;
import org.openrewrite.jgit.lib.AnyObjectId;
import org.openrewrite.jgit.revwalk.ObjectWalk;

/**
 * Selects interesting objects when walking.
 * <p>
 * Applications should install the filter on an ObjectWalk by
 * {@link org.openrewrite.jgit.revwalk.ObjectWalk#setObjectFilter(ObjectFilter)}
 * prior to starting traversal.
 *
 * @since 4.0
 */
public abstract class ObjectFilter {
	/** Default filter that always returns true. */
	public static final ObjectFilter ALL = new AllFilter();

	private static final class AllFilter extends ObjectFilter {
		@Override
		public boolean include(ObjectWalk walker, AnyObjectId o) {
			return true;
		}
	}

	/**
	 * Determine if the named object should be included in the walk.
	 *
	 * @param walker
	 *            the active walker this filter is being invoked from within.
	 * @param objid
	 *            the object currently being tested.
	 * @return {@code true} if the named object should be included in the walk.
	 * @throws org.openrewrite.jgit.errors.MissingObjectException
	 *             an object the filter needed to consult to determine its
	 *             answer was missing
	 * @throws org.openrewrite.jgit.errors.IncorrectObjectTypeException
	 *             an object the filter needed to consult to determine its
	 *             answer was of the wrong type
	 * @throws java.io.IOException
	 *             an object the filter needed to consult to determine its
	 *             answer could not be read.
	 */
	public abstract boolean include(ObjectWalk walker, AnyObjectId objid)
			throws MissingObjectException, IncorrectObjectTypeException,
			       IOException;
}

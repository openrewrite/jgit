/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.openrewrite.jgit.internal.storage.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.openrewrite.jgit.internal.storage.pack.ObjectToPack;
import org.openrewrite.jgit.internal.storage.pack.PackWriter;
import org.openrewrite.jgit.lib.AbbreviatedObjectId;
import org.openrewrite.jgit.lib.AnyObjectId;
import org.openrewrite.jgit.lib.Config;
import org.openrewrite.jgit.lib.ObjectDatabase;
import org.openrewrite.jgit.lib.ObjectId;
import org.openrewrite.jgit.lib.ObjectLoader;
import org.openrewrite.jgit.lib.ObjectReader;
import org.openrewrite.jgit.util.FS;

abstract class FileObjectDatabase extends ObjectDatabase {
	enum InsertLooseObjectResult {
		INSERTED, EXISTS_PACKED, EXISTS_LOOSE, FAILURE;
	}

	/** {@inheritDoc} */
	@Override
	public ObjectReader newReader() {
		return new WindowCursor(this);
	}

	/** {@inheritDoc} */
	@Override
	public ObjectDirectoryInserter newInserter() {
		return new ObjectDirectoryInserter(this, getConfig());
	}

	abstract void resolve(Set<ObjectId> matches, AbbreviatedObjectId id)
			throws IOException;

	abstract Config getConfig();

	abstract FS getFS();

	abstract Set<ObjectId> getShallowCommits() throws IOException;

	abstract void selectObjectRepresentation(PackWriter packer,
			ObjectToPack otp, WindowCursor curs) throws IOException;

	abstract File getDirectory();

	abstract File fileFor(AnyObjectId id);

	abstract ObjectLoader openObject(WindowCursor curs, AnyObjectId objectId)
			throws IOException;

	abstract long getObjectSize(WindowCursor curs, AnyObjectId objectId)
			throws IOException;

	abstract ObjectLoader openLooseObject(WindowCursor curs, AnyObjectId id)
			throws IOException;

	abstract InsertLooseObjectResult insertUnpackedObject(File tmp,
			ObjectId id, boolean createDuplicate) throws IOException;

	abstract Pack openPack(File pack) throws IOException;

	abstract Collection<Pack> getPacks();
}

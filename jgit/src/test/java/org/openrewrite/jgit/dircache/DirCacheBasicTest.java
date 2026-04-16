/*
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openrewrite.jgit.dircache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.errors.CorruptObjectException;
import org.openrewrite.jgit.internal.JGitText;
import org.openrewrite.jgit.lib.ConfigConstants;
import org.openrewrite.jgit.lib.Constants;
import org.openrewrite.jgit.lib.FileMode;
import org.openrewrite.jgit.lib.ObjectInserter;
import org.openrewrite.jgit.lib.Repository;
import org.openrewrite.jgit.lib.StoredConfig;
import org.openrewrite.jgit.storage.file.FileBasedConfig;
import org.openrewrite.jgit.util.FileUtils;
import org.openrewrite.jgit.util.SystemReader;

public class DirCacheBasicTest {

	protected Repository db;
	private Git git;
	private File trash;
	private SystemReader originalSystemReader;

	private void setup(boolean skipHash) throws Exception {
		originalSystemReader = SystemReader.getInstance();
		trash = FileUtils.createTempDir("jgit-dircache", null, null);
		git = Git.init().setDirectory(trash).call();
		db = git.getRepository();
		StoredConfig cfg = db.getConfig();
		cfg.setBoolean(ConfigConstants.CONFIG_INDEX_SECTION, null,
				ConfigConstants.CONFIG_KEY_SKIPHASH, skipHash);
		cfg.save();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (originalSystemReader != null) {
			SystemReader.setInstance(originalSystemReader);
		}
		if (db != null) {
			db.close();
		}
		if (trash != null) {
			FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.SKIP_MISSING);
		}
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testReadMissing_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		assertFalse(idx.exists());

		final DirCache dc = db.readDirCache();
		assertNotNull(dc);
		assertEquals(0, dc.getEntryCount());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testReadMissing_TempIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "tmp_index");
		assertFalse(idx.exists());

		final DirCache dc = DirCache.read(idx, db.getFS());
		assertNotNull(dc);
		assertEquals(0, dc.getEntryCount());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testLockMissing_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertNotNull(dc);
		assertFalse(idx.exists());
		assertTrue(lck.exists());
		assertEquals(0, dc.getEntryCount());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testLockMissing_TempIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "tmp_index");
		final File lck = new File(db.getDirectory(), "tmp_index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = DirCache.lock(idx, db.getFS());
		assertNotNull(dc);
		assertFalse(idx.exists());
		assertTrue(lck.exists());
		assertEquals(0, dc.getEntryCount());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testWriteEmptyUnlock_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertEquals(0, lck.length());
		dc.write();
		assertEquals(12 + 20, lck.length());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testWriteEmptyCommit_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertEquals(0, lck.length());
		dc.write();
		assertEquals(12 + 20, lck.length());

		assertTrue(dc.commit());
		assertTrue(idx.exists());
		assertFalse(lck.exists());
		assertEquals(12 + 20, idx.length());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testWriteEmptyReadEmpty_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());
		{
			final DirCache dc = db.lockDirCache();
			dc.write();
			assertTrue(dc.commit());
			assertTrue(idx.exists());
		}
		{
			final DirCache dc = db.readDirCache();
			assertEquals(0, dc.getEntryCount());
		}
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testWriteEmptyLockEmpty_RealIndex(boolean skipHash) throws Exception {
		setup(skipHash);
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());
		{
			final DirCache dc = db.lockDirCache();
			dc.write();
			assertTrue(dc.commit());
			assertTrue(idx.exists());
		}
		{
			final DirCache dc = db.lockDirCache();
			assertEquals(0, dc.getEntryCount());
			assertTrue(idx.exists());
			assertTrue(lck.exists());
			dc.unlock();
		}
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testBuildThenClear(boolean skipHash) throws Exception {
		setup(skipHash);
		final DirCache dc = db.readDirCache();

		final String[] paths = { "a-", "a.b", "a/b", "a0b" };
		final DirCacheEntry[] ents = new DirCacheEntry[paths.length];
		for (int i = 0; i < paths.length; i++) {
			ents[i] = new DirCacheEntry(paths[i]);
			ents[i].setFileMode(FileMode.REGULAR_FILE);
		}

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();
		assertFalse(dc.hasUnmergedPaths());

		assertEquals(paths.length, dc.getEntryCount());
		dc.clear();
		assertEquals(0, dc.getEntryCount());
		assertFalse(dc.hasUnmergedPaths());
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testDetectUnmergedPaths(boolean skipHash) throws Exception {
		setup(skipHash);
		final DirCache dc = db.readDirCache();
		final DirCacheEntry[] ents = new DirCacheEntry[3];

		ents[0] = new DirCacheEntry("a", 1);
		ents[0].setFileMode(FileMode.REGULAR_FILE);
		ents[1] = new DirCacheEntry("a", 2);
		ents[1].setFileMode(FileMode.REGULAR_FILE);
		ents[2] = new DirCacheEntry("a", 3);
		ents[2].setFileMode(FileMode.REGULAR_FILE);

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();
		assertTrue(dc.hasUnmergedPaths());
	}

	@Test
	public void testFindOnEmpty() throws Exception {
		final DirCache dc = DirCache.newInCore();
		final byte[] path = Constants.encode("a");
		assertEquals(-1, dc.findEntry(path, path.length));
	}

	@ParameterizedTest(name = "skipHash: {0}")
	@ValueSource(booleans = { true, false })
	public void testRejectInvalidWindowsPaths(boolean skipHash) throws Exception {
		setup(skipHash);
		SystemReader.setInstance(new OsNameSystemReader(originalSystemReader, "Linux"));

		String path = "src/con.txt";
		DirCache dc = db.lockDirCache();
		DirCacheBuilder b = dc.builder();
		DirCacheEntry e = new DirCacheEntry(path);
		e.setFileMode(FileMode.REGULAR_FILE);
		try (ObjectInserter.Formatter formatter = new ObjectInserter.Formatter()) {
			e.setObjectId(formatter.idFor(
					Constants.OBJ_BLOB,
					Constants.encode(path)));
		}
		b.add(e);
		b.commit();
		db.readDirCache();

		SystemReader.setInstance(new OsNameSystemReader(originalSystemReader, "Windows"));

		try {
			db.readDirCache();
			fail("should have rejected " + path);
		} catch (CorruptObjectException err) {
			assertEquals(MessageFormat.format(JGitText.get().invalidPath, path),
					err.getMessage());
			assertNotNull(err.getCause());
			assertEquals("invalid name 'CON'", err.getCause().getMessage());
		}
	}

	private static class OsNameSystemReader extends SystemReader {
		private final SystemReader delegate;
		private final String osName;

		OsNameSystemReader(SystemReader delegate, String osName) {
			this.delegate = delegate;
			this.osName = osName;
		}

		@Override
		public String getHostname() {
			return delegate.getHostname();
		}

		@Override
		public String getenv(String variable) {
			return delegate.getenv(variable);
		}

		@Override
		public String getProperty(String key) {
			if ("os.name".equals(key)) {
				return osName;
			}
			return delegate.getProperty(key);
		}

		@Override
		public FileBasedConfig openUserConfig(org.openrewrite.jgit.lib.Config parent,
				org.openrewrite.jgit.util.FS fs) {
			return delegate.openUserConfig(parent, fs);
		}

		@Override
		public FileBasedConfig openSystemConfig(org.openrewrite.jgit.lib.Config parent,
				org.openrewrite.jgit.util.FS fs) {
			return delegate.openSystemConfig(parent, fs);
		}

		@Override
		public FileBasedConfig openJGitConfig(org.openrewrite.jgit.lib.Config parent,
				org.openrewrite.jgit.util.FS fs) {
			return delegate.openJGitConfig(parent, fs);
		}

		@Override
		public long getCurrentTime() {
			return delegate.getCurrentTime();
		}

		@Override
		public int getTimezone(long when) {
			return delegate.getTimezone(when);
		}
	}
}

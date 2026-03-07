/*
 * Copyright (C) 2026, OpenRewrite and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.openrewrite.jgit.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.jgit.lib.Repository;
import org.openrewrite.jgit.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ApplyCommand}, focusing on multi-hunk patch application.
 */
public class ApplyCommandTest {

	private Repository db;
	private Git git;
	private File trash;

	@BeforeEach
	public void setUp() throws Exception {
		trash = Files.createTempDirectory("jgit-apply-test").toFile();
		git = Git.init().setDirectory(trash).call();
		db = git.getRepository();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (db != null) {
			db.close();
		}
		if (trash != null) {
			FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.RETRY);
		}
	}

	/**
	 * Test applying a multi-hunk patch where both the file and patch use LF line endings.
	 */
	@Test
	public void testMultiHunkPatchWithLF() throws Exception {
		// Create a file with 150 lines using LF endings
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append('\n');
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		// Create a patch with 2 hunks:
		// Hunk 1: adds 6 lines around line 10 (net +6)
		// Hunk 2: adds 1 line around line 118 (old) / 124 (new)
		String patch = "diff --git a/test.txt b/test.txt\n"
				+ "--- a/test.txt\n"
				+ "+++ b/test.txt\n"
				+ "@@ -10,6 +10,12 @@\n"
				+ " Line 10\n"
				+ " Line 11\n"
				+ " Line 12\n"
				+ "+New Line A1\n"
				+ "+New Line A2\n"
				+ "+New Line A3\n"
				+ "+New Line A4\n"
				+ "+New Line A5\n"
				+ "+New Line A6\n"
				+ " Line 13\n"
				+ " Line 14\n"
				+ " Line 15\n"
				+ "@@ -118,6 +124,7 @@\n"
				+ " Line 118\n"
				+ " Line 119\n"
				+ " Line 120\n"
				+ "+New Line B\n"
				+ " Line 121\n"
				+ " Line 122\n"
				+ " Line 123\n";

		ApplyResult result = applyPatch(patch);
		assertTrue(result.getUpdatedFiles().size() > 0);

		String resultContent = readFile("test.txt");
		assertTrue(resultContent.contains("New Line A1"));
		assertTrue(resultContent.contains("New Line A6"));
		assertTrue(resultContent.contains("New Line B"));
	}

	/**
	 * Test applying a patch with CRLF line endings to a file with LF line
	 * endings. The patch context lines will have trailing \r after
	 * getRawString strips the \n, causing mismatch with the LF-only file
	 * lines.
	 */
	@Test
	public void testMultiHunkPatchWithCRLFPatchAndLFFile() throws Exception {
		// Create a file with 150 lines using LF endings
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append('\n');
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		// Create the same patch but with CRLF line endings
		String patch = "diff --git a/test.txt b/test.txt\r\n"
				+ "--- a/test.txt\r\n"
				+ "+++ b/test.txt\r\n"
				+ "@@ -10,6 +10,12 @@\r\n"
				+ " Line 10\r\n"
				+ " Line 11\r\n"
				+ " Line 12\r\n"
				+ "+New Line A1\r\n"
				+ "+New Line A2\r\n"
				+ "+New Line A3\r\n"
				+ "+New Line A4\r\n"
				+ "+New Line A5\r\n"
				+ "+New Line A6\r\n"
				+ " Line 13\r\n"
				+ " Line 14\r\n"
				+ " Line 15\r\n"
				+ "@@ -118,6 +124,7 @@\r\n"
				+ " Line 118\r\n"
				+ " Line 119\r\n"
				+ " Line 120\r\n"
				+ "+New Line B\r\n"
				+ " Line 121\r\n"
				+ " Line 122\r\n"
				+ " Line 123\r\n";

		ApplyResult result = applyPatch(patch);
		assertTrue(result.getUpdatedFiles().size() > 0);

		String resultContent = readFile("test.txt");
		assertTrue(resultContent.contains("New Line A1"));
		assertTrue(resultContent.contains("New Line B"));
	}

	/**
	 * Test applying a patch with LF line endings to a file with CRLF line
	 * endings. The needsCrLfConversion logic should handle this case by
	 * converting the file to LF before comparison.
	 */
	@Test
	public void testMultiHunkPatchWithLFPatchAndCRLFFile() throws Exception {
		// Create a file with 150 lines using CRLF endings
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append("\r\n");
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		// Create a patch with LF line endings
		String patch = "diff --git a/test.txt b/test.txt\n"
				+ "--- a/test.txt\n"
				+ "+++ b/test.txt\n"
				+ "@@ -10,6 +10,12 @@\n"
				+ " Line 10\n"
				+ " Line 11\n"
				+ " Line 12\n"
				+ "+New Line A1\n"
				+ "+New Line A2\n"
				+ "+New Line A3\n"
				+ "+New Line A4\n"
				+ "+New Line A5\n"
				+ "+New Line A6\n"
				+ " Line 13\n"
				+ " Line 14\n"
				+ " Line 15\n"
				+ "@@ -118,6 +124,7 @@\n"
				+ " Line 118\n"
				+ " Line 119\n"
				+ " Line 120\n"
				+ "+New Line B\n"
				+ " Line 121\n"
				+ " Line 122\n"
				+ " Line 123\n";

		ApplyResult result = applyPatch(patch);
		assertTrue(result.getUpdatedFiles().size() > 0);

		String resultContent = readFile("test.txt");
		assertTrue(resultContent.contains("New Line A1"));
		assertTrue(resultContent.contains("New Line B"));
	}

	/**
	 * Test applying a patch where the file has trailing whitespace on some
	 * context lines but the patch does not. The trailing-whitespace-tolerant
	 * comparison should handle this.
	 */
	@Test
	public void testMultiHunkPatchWithTrailingWhitespaceDifference()
			throws Exception {
		// Create a file where some lines have trailing spaces
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			if (i == 118 || i == 119 || i == 120) {
				fileContent.append("Line ").append(i).append("   \n");
			} else {
				fileContent.append("Line ").append(i).append('\n');
			}
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		// Patch context lines do NOT have trailing spaces
		String patch = "diff --git a/test.txt b/test.txt\n"
				+ "--- a/test.txt\n"
				+ "+++ b/test.txt\n"
				+ "@@ -10,6 +10,12 @@\n"
				+ " Line 10\n"
				+ " Line 11\n"
				+ " Line 12\n"
				+ "+New Line A1\n"
				+ "+New Line A2\n"
				+ "+New Line A3\n"
				+ "+New Line A4\n"
				+ "+New Line A5\n"
				+ "+New Line A6\n"
				+ " Line 13\n"
				+ " Line 14\n"
				+ " Line 15\n"
				+ "@@ -118,6 +124,7 @@\n"
				+ " Line 118\n"
				+ " Line 119\n"
				+ " Line 120\n"
				+ "+New Line B\n"
				+ " Line 121\n"
				+ " Line 122\n"
				+ " Line 123\n";

		ApplyResult result = applyPatch(patch);
		assertTrue(result.getUpdatedFiles().size() > 0);

		String resultContent = readFile("test.txt");
		assertTrue(resultContent.contains("New Line A1"));
		assertTrue(resultContent.contains("New Line B"));
	}

	private void writeFileAndCommit(String name, String content)
			throws Exception {
		File f = new File(trash, name);
		try (FileOutputStream fos = new FileOutputStream(f)) {
			fos.write(content.getBytes(StandardCharsets.UTF_8));
		}
		git.add().addFilepattern(name).call();
		git.commit().setMessage("initial").call();
	}

	private ApplyResult applyPatch(String patch) throws Exception {
		InputStream in = new ByteArrayInputStream(
				patch.getBytes(StandardCharsets.UTF_8));
		return git.apply().setPatch(in).call();
	}

	private String readFile(String name) throws IOException {
		File f = new File(trash, name);
		return new String(Files.readAllBytes(f.toPath()),
				StandardCharsets.UTF_8);
	}
}

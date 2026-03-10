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
import org.openrewrite.jgit.api.errors.PatchApplyException;
import org.openrewrite.jgit.lib.Repository;
import org.openrewrite.jgit.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append('\n');
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		String patch = multiHunkPatch("\n");

		ApplyResult result = applyPatch(patch);
		assertThat(result.getUpdatedFiles()).isNotEmpty();

		String resultContent = readFile("test.txt");
		assertThat(resultContent).contains("New Line A1", "New Line A6",
				"New Line B");
	}

	/**
	 * Test applying a patch with CRLF line endings to a file with LF line
	 * endings.
	 */
	@Test
	public void testMultiHunkPatchWithCRLFPatchAndLFFile() throws Exception {
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append('\n');
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		String patch = multiHunkPatch("\r\n");

		ApplyResult result = applyPatch(patch);
		assertThat(result.getUpdatedFiles()).isNotEmpty();

		String resultContent = readFile("test.txt");
		assertThat(resultContent).contains("New Line A1", "New Line B");
	}

	/**
	 * Test applying a patch with LF line endings to a file with CRLF line
	 * endings.
	 */
	@Test
	public void testMultiHunkPatchWithLFPatchAndCRLFFile() throws Exception {
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append("\r\n");
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		String patch = multiHunkPatch("\n");

		ApplyResult result = applyPatch(patch);
		assertThat(result.getUpdatedFiles()).isNotEmpty();

		String resultContent = readFile("test.txt");
		assertThat(resultContent).contains("New Line A1", "New Line B");
	}

	/**
	 * Test applying a patch where the file has trailing whitespace on some
	 * context lines but the patch does not.
	 */
	@Test
	public void testMultiHunkPatchWithTrailingWhitespaceDifference()
			throws Exception {
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			if (i == 118 || i == 119 || i == 120) {
				fileContent.append("Line ").append(i).append("   \n");
			} else {
				fileContent.append("Line ").append(i).append('\n');
			}
		}
		writeFileAndCommit("test.txt", fileContent.toString());

		String patch = multiHunkPatch("\n");

		ApplyResult result = applyPatch(patch);
		assertThat(result.getUpdatedFiles()).isNotEmpty();

		String resultContent = readFile("test.txt");
		assertThat(resultContent).contains("New Line A1", "New Line B");
	}

	/**
	 * Test that a patch with genuinely mismatched context lines is still
	 * rejected.
	 */
	@Test
	public void testMultiHunkPatchWithMismatchedContextIsRejected()
			throws Exception {
		StringBuilder fileContent = new StringBuilder();
		for (int i = 1; i <= 150; i++) {
			fileContent.append("Line ").append(i).append('\n');
		}
		// Replace line 119 with completely different content
		String content = fileContent.toString()
				.replace("Line 119\n", "DIFFERENT CONTENT\n");
		writeFileAndCommit("test.txt", content);

		String patch = multiHunkPatch("\n");

		assertThatThrownBy(() -> applyPatch(patch))
				.isInstanceOf(PatchApplyException.class)
				.hasMessageContaining("hunk");
	}

	private String multiHunkPatch(String eol) {
		return "diff --git a/test.txt b/test.txt" + eol
				+ "--- a/test.txt" + eol
				+ "+++ b/test.txt" + eol
				+ "@@ -10,6 +10,12 @@" + eol
				+ " Line 10" + eol
				+ " Line 11" + eol
				+ " Line 12" + eol
				+ "+New Line A1" + eol
				+ "+New Line A2" + eol
				+ "+New Line A3" + eol
				+ "+New Line A4" + eol
				+ "+New Line A5" + eol
				+ "+New Line A6" + eol
				+ " Line 13" + eol
				+ " Line 14" + eol
				+ " Line 15" + eol
				+ "@@ -118,6 +124,7 @@" + eol
				+ " Line 118" + eol
				+ " Line 119" + eol
				+ " Line 120" + eol
				+ "+New Line B" + eol
				+ " Line 121" + eol
				+ " Line 122" + eol
				+ " Line 123" + eol;
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

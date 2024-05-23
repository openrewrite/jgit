/*
 * Copyright (C) 2009, Constantine Plotnikov <constantine.plotnikov@gmail.com>
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2009, Google, Inc.
 * Copyright (C) 2009, JetBrains s.r.o.
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

//TODO(ms): move to org.openrewrite.jgit.ssh.jsch in 6.0
package org.openrewrite.jgit.transport;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.openrewrite.jgit.errors.TransportException;
import org.openrewrite.jgit.internal.transport.jsch.JSchText;
import org.openrewrite.jgit.util.io.IsolatedOutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Run remote commands using Jsch.
 * <p>
 * This class is the default session implementation using Jsch. Note that
 * {@link org.openrewrite.jgit.transport.JschConfigSessionFactory} is used to create
 * the actual session passed to the constructor.
 */
public class JschSession implements RemoteSession2 {
	final Session sock;
	final URIish uri;

	/**
	 * Create a new session object by passing the real Jsch session and the URI
	 * information.
	 *
	 * @param session
	 *            the real Jsch session created elsewhere.
	 * @param uri
	 *            the URI information for the remote connection
	 */
	public JschSession(Session session, URIish uri) {
		sock = session;
		this.uri = uri;
	}

	/** {@inheritDoc} */
	@Override
	public Process exec(String command, int timeout) throws IOException {
		return exec(command, Collections.emptyMap(), timeout);
	}

	/** {@inheritDoc} */
	@Override
	public Process exec(String command, Map<String, String> environment,
			int timeout) throws IOException {
		return new JschProcess(command, environment, timeout);
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() {
		if (sock.isConnected())
			sock.disconnect();
	}

	/**
	 * A kludge to allow {@link org.openrewrite.jgit.transport.TransportSftp} to get
	 * an Sftp channel from Jsch. Ideally, this method would be generic, which
	 * would require implementing generic Sftp channel operations in the
	 * RemoteSession class.
	 *
	 * @return a channel suitable for Sftp operations.
	 * @throws com.jcraft.jsch.JSchException
	 *             on problems getting the channel.
	 * @deprecated since 5.2; use {@link #getFtpChannel()} instead
	 */
	@Deprecated
	public Channel getSftpChannel() throws JSchException {
		return sock.openChannel("sftp"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 5.2
	 */
	@Override
	public FtpChannel getFtpChannel() {
		return new JschFtpChannel();
	}

	/**
	 * Implementation of Process for running a single command using Jsch.
	 * <p>
	 * Uses the Jsch session to do actual command execution and manage the
	 * execution.
	 */
	private class JschProcess extends Process {
		private ChannelExec channel;

		final int timeout;

		private InputStream inputStream;

		private OutputStream outputStream;

		private InputStream errStream;

		/**
		 * Opens a channel on the session ("sock") for executing the given
		 * command, opens streams, and starts command execution.
		 *
		 * @param commandName
		 *            the command to execute
		 * @param environment
		 *            environment variables to pass on
		 * @param tms
		 *            the timeout value, in seconds, for the command.
		 * @throws TransportException
		 *             on problems opening a channel or connecting to the remote
		 *             host
		 * @throws IOException
		 *             on problems opening streams
		 */
		JschProcess(String commandName, Map<String, String> environment,
				int tms) throws TransportException, IOException {
			timeout = tms;
			try {
				channel = (ChannelExec) sock.openChannel("exec"); //$NON-NLS-1$
				if (environment != null) {
					for (Map.Entry<String, String> envVar : environment
							.entrySet()) {
						channel.setEnv(envVar.getKey(), envVar.getValue());
					}
				}
				channel.setCommand(commandName);
				setupStreams();
				channel.connect(timeout > 0 ? timeout * 1000 : 0);
				if (!channel.isConnected()) {
					closeOutputStream();
					throw new TransportException(uri,
							JSchText.get().connectionFailed);
				}
			} catch (JSchException e) {
				closeOutputStream();
				throw new TransportException(uri, e.getMessage(), e);
			}
		}

		private void closeOutputStream() {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}

		private void setupStreams() throws IOException {
			inputStream = channel.getInputStream();

			// JSch won't let us interrupt writes when we use our InterruptTimer
			// to break out of a long-running write operation. To work around
			// that we spawn a background thread to shuttle data through a pipe,
			// as we can issue an interrupted write out of that. Its slower, so
			// we only use this route if there is a timeout.
			OutputStream out = channel.getOutputStream();
			if (timeout <= 0) {
				outputStream = out;
			} else {
				IsolatedOutputStream i = new IsolatedOutputStream(out);
				outputStream = new BufferedOutputStream(i, 16 * 1024);
			}

			errStream = channel.getErrStream();
		}

		@Override
		public InputStream getInputStream() {
			return inputStream;
		}

		@Override
		public OutputStream getOutputStream() {
			return outputStream;
		}

		@Override
		public InputStream getErrorStream() {
			return errStream;
		}

		@Override
		public int exitValue() {
			if (isRunning())
				throw new IllegalThreadStateException();
			return channel.getExitStatus();
		}

		private boolean isRunning() {
			return channel.getExitStatus() < 0 && channel.isConnected();
		}

		@Override
		public void destroy() {
			if (channel.isConnected())
				channel.disconnect();
			closeOutputStream();
		}

		@Override
		public int waitFor() throws InterruptedException {
			while (isRunning())
				Thread.sleep(100);
			return exitValue();
		}
	}

	private class JschFtpChannel implements FtpChannel {

		private ChannelSftp ftp;

		@Override
		public void connect(int timeout, TimeUnit unit) throws IOException {
			try {
				ftp = (ChannelSftp) sock.openChannel("sftp"); //$NON-NLS-1$
				ftp.connect((int) unit.toMillis(timeout));
			} catch (JSchException e) {
				ftp = null;
				throw new IOException(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public void disconnect() {
			ftp.disconnect();
			ftp = null;
		}

		private <T> T map(Callable<T> op) throws IOException {
			try {
				return op.call();
			} catch (Exception e) {
				if (e instanceof SftpException) {
					throw new FtpChannel.FtpException(e.getLocalizedMessage(),
							((SftpException) e).id, e);
				}
				throw new IOException(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public boolean isConnected() {
			return ftp != null && sock.isConnected();
		}

		@Override
		public void cd(String path) throws IOException {
			map(() -> {
				ftp.cd(path);
				return null;
			});
		}

		@Override
		public String pwd() throws IOException {
			return map(() -> ftp.pwd());
		}

		@Override
		public Collection<DirEntry> ls(String path) throws IOException {
			return map(() -> {
				List<DirEntry> result = new ArrayList<>();
				for (Object e : ftp.ls(path)) {
					ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) e;
					result.add(new DirEntry() {

						@Override
						public String getFilename() {
							return entry.getFilename();
						}

						@Override
						public long getModifiedTime() {
							return entry.getAttrs().getMTime();
						}

						@Override
						public boolean isDirectory() {
							return entry.getAttrs().isDir();
						}
					});
				}
				return result;
			});
		}

		@Override
		public void rmdir(String path) throws IOException {
			map(() -> {
				ftp.rm(path);
				return null;
			});
		}

		@Override
		public void mkdir(String path) throws IOException {
			map(() -> {
				ftp.mkdir(path);
				return null;
			});
		}

		@Override
		public InputStream get(String path) throws IOException {
			return map(() -> ftp.get(path));
		}

		@Override
		public OutputStream put(String path) throws IOException {
			return map(() -> ftp.put(path));
		}

		@Override
		public void rm(String path) throws IOException {
			map(() -> {
				ftp.rm(path);
				return null;
			});
		}

		@Override
		public void rename(String from, String to) throws IOException {
			map(() -> {
				// Plain FTP rename will fail if "to" exists. Jsch knows about
				// the FTP extension "posix-rename@openssh.com", which will
				// remove "to" first if it exists.
				if (hasPosixRename()) {
					ftp.rename(from, to);
				} else if (!to.equals(from)) {
					// Try to remove "to" first. With git, we typically get this
					// when a lock file is moved over the file locked. Note that
					// the check for to being equal to from may still fail in
					// the general case, but for use with JGit's TransportSftp
					// it should be good enough.
					delete(to);
					ftp.rename(from, to);
				}
				return null;
			});
		}

		/**
		 * Determine whether the server has the posix-rename extension.
		 *
		 * @return {@code true} if it is supported, {@code false} otherwise
		 * @see <a href=
		 *      "https://cvsweb.openbsd.org/src/usr.bin/ssh/PROTOCOL?annotate=HEAD">OpenSSH
		 *      deviations and extensions to the published SSH protocol</a>
		 * @see <a href=
		 *      "http://pubs.opengroup.org/onlinepubs/9699919799/functions/rename.html">stdio.h:
		 *      rename()</a>
		 */
		private boolean hasPosixRename() {
			return "1".equals(ftp.getExtension("posix-rename@openssh.com")); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
}

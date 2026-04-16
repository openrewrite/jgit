package org.openrewrite.jgit.internal.storage.io;

import java.security.MessageDigest;

import org.openrewrite.jgit.lib.Constants;

/**
 * Dummy message digest consisting of only null bytes with the length of an
 * ObjectId. This class can be used to skip computing a real digest.
 */
public final class NullMessageDigest extends MessageDigest {
	private static final byte[] digest = new byte[Constants.OBJECT_ID_LENGTH];

	private static final NullMessageDigest INSTANCE = new NullMessageDigest();

	/**
	 * Get the only instance of NullMessageDigest
	 *
	 * @return the only instance of NullMessageDigest
	 */
	public static MessageDigest getInstance() {
		return INSTANCE;
	}

	private NullMessageDigest() {
		super("null"); //$NON-NLS-1$
	}

	@Override
	protected void engineUpdate(byte input) {
		// empty
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		// empty
	}

	@Override
	protected byte[] engineDigest() {
		return digest;
	}

	@Override
	protected void engineReset() {
		// empty
	}
}

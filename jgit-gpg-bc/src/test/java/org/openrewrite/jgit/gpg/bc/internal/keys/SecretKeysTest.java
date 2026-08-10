/*
 * Copyright (C) 2021, 2024 Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.openrewrite.jgit.gpg.bc.internal.keys;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

class SecretKeysTest {

	@BeforeAll
	static void ensureBC() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	// Ed25519 keys are not covered: SExprParser has never read the
	// `(flags eddsa)` element that gpg writes for them.
	@ParameterizedTest(name = "{0}")
	@CsvSource({
			"AFDA8EA10E185ACF8C0D0F8885A0EF61A72ECB11, false",
			"2FB05DBB70FC07CB84C13431F640CA6CEA1DBF8A, false",
			"66CCECEC2AB46A9735B10FEC54EDF9FD0F77BAF9, true",
			"F727FAB884DA3BD402B6E0F5472E108D21033124, true" })
	void secretKeyIsRead(String keyGrip, boolean encrypted) throws Exception {
		PGPPublicKey publicKey;
		try (InputStream pubIn = getClass()
				.getResourceAsStream(keyGrip + ".asc")) {
			assertThat(pubIn).as("public key").isNotNull();
			publicKey = readAsc(pubIn);
		}
		PGPDigestCalculatorProvider calculatorProvider = new JcaPGPDigestCalculatorProviderBuilder()
				.build();
		try (InputStream in = new BufferedInputStream(
				getClass().getResourceAsStream(keyGrip + ".key"))) {
			PGPSecretKey secretKey = SecretKeys.readSecretKey(in,
					calculatorProvider,
					encrypted ? () -> "nonsense".toCharArray() : null,
					publicKey);
			assertThat(secretKey).isNotNull();
			assertThat(secretKey.extractPrivateKey(null)).isNotNull();
		}
	}

	private static PGPPublicKey readAsc(InputStream in)
			throws IOException, PGPException {
		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(in), new JcaKeyFingerprintCalculator());
		Iterator<PGPPublicKeyRing> keyRings = pgpPub.getKeyRings();
		while (keyRings.hasNext()) {
			Iterator<PGPPublicKey> keys = keyRings.next().getPublicKeys();
			if (keys.hasNext()) {
				return keys.next();
			}
		}
		return null;
	}
}

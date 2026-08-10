/*
 * Copyright (C) 2021, Thomas Wolf <thomas.wolf@paranor.ch> and others
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
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KeyGripTest {

	@BeforeAll
	static void ensureBC() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static Stream<Arguments> keys() {
		return Stream.of(
				Arguments.of("rsa.asc",
						new String[] { "D148210FAF36468055B83D0F5A6DEB83FBC8E864",
								"A5E4CD2CBBE44A16E4D6EC05C2E3C3A599DC763C" }),
				Arguments.of("dsa-elgamal.asc",
						new String[] { "552286BEB2999F0A9E26A50385B90D9724001187",
								"CED7034A8EB5F4CE90DF99147EC33D86FCD3296C" }),
				Arguments.of("brainpool256.asc",
						new String[] { "A01BAA22A72F09A0FF0A1D4CBCE70844DD52DDD7",
								"C1678B7DE5F144C93B89468D5F9764ACE182ED36" }),
				Arguments.of("brainpool384.asc",
						new String[] { "2F25DB025DEBF3EA2715350209B985829B04F50A",
								"B6BD8B81F75AF914163D97DF8DE8F6FC64C283F8" }),
				Arguments.of("brainpool512.asc",
						new String[] { "5A484F56AB4B8B6583B6365034999F6543FAE1AE",
								"9133E4A7E8FC8515518DF444C3F2F247EEBBADEC" }),
				Arguments.of("nistp256.asc",
						new String[] { "FC81AECE90BCE6E54D0D637D266109783AC8DAC0",
								"A56DC8DB8355747A809037459B4258B8A743EAB5" }),
				Arguments.of("nistp384.asc",
						new String[] { "A1338230AED1C9C125663518470B49056C9D1733",
								"797A83FE041FFE06A7F4B1D32C6F4AE0F6D87ADF" }),
				Arguments.of("nistp521.asc",
						new String[] { "D91B789603EC9138AA20342A2B6DC86C81B70F5D",
								"FD048B2CA1919CB241DC8A2C7FA3E742EF343DCA" }),
				Arguments.of("secp256k1.asc",
						new String[] { "498B89C485489BA16B40755C0EBA580166393074",
								"48FFED40D018747363BDEFFDD404D1F4870F8064" }),
				Arguments.of("ed25519.asc",
						new String[] { "940D97D75C306D737A59A98EAFF1272832CEDC0B" }),
				Arguments.of("x25519.asc",
						new String[] { "A77DC8173DA6BEE126F5BD6F5A14E01200B52FCE",
								"636C983EDB558527BA82780B52CB5DAE011BE46B" }));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("keys")
	void keyGripsMatch(String filename, String[] expectedKeyGrips)
			throws Exception {
		List<String> keyGrips = new ArrayList<>();
		try (InputStream in = getClass().getResourceAsStream(filename)) {
			assertThat(in).as(filename).isNotNull();
			for (PGPPublicKey key : readAsc(in)) {
				keyGrips.add(Hex.toHexString(KeyGrip.getKeyGrip(key))
						.toUpperCase(Locale.ROOT));
			}
		}
		assertThat(keyGrips).containsExactly(expectedKeyGrips);
	}

	private static List<PGPPublicKey> readAsc(InputStream in)
			throws IOException, PGPException {
		List<PGPPublicKey> result = new ArrayList<>();
		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(in), new JcaKeyFingerprintCalculator());
		Iterator<PGPPublicKeyRing> keyRings = pgpPub.getKeyRings();
		while (keyRings.hasNext()) {
			Iterator<PGPPublicKey> keys = keyRings.next().getPublicKeys();
			while (keys.hasNext()) {
				result.add(keys.next());
			}
		}
		return result;
	}
}

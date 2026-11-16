/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.hisensetv.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import javax.net.ssl.X509TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Verifies the exact certificate configured for a TV with a self-signed certificate.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
final class HisenseTVTrustManager implements X509TrustManager {
    private final byte[] fingerprint;

    HisenseTVTrustManager(String fingerprint) {
        this.fingerprint = HexFormat.of().parseHex(fingerprint);
    }

    @Override
    public void checkServerTrusted(X509Certificate @Nullable [] chain, @Nullable String authType)
            throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("The TV did not present a certificate");
        }
        try {
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());
            if (!MessageDigest.isEqual(fingerprint, actual)) {
                throw new CertificateException("The TV certificate does not match the configured fingerprint");
            }
            chain[0].checkValidity();
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate @Nullable [] chain, @Nullable String authType)
            throws CertificateException {
        throw new CertificateException("Only server certificates are supported");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}

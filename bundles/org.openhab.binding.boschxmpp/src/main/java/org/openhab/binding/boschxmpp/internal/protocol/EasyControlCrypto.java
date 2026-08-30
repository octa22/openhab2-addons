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
package org.openhab.binding.boschxmpp.internal.protocol;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;

/**
 * Implements the key derivation and AES encryption used by Bosch EasyControl.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EasyControlCrypto {

    private static final byte[] MAGIC = hexToBytes("1d86b2631b02f2c7978b41e8a3ae609b0b2afbfd30ff386da60c586a827408e4");

    private final SecretKeySpec key;

    public EasyControlCrypto(String accessKey, String devicePassword) throws BoschXmppException {
        key = new SecretKeySpec(deriveKey(accessKey.replace("-", ""), devicePassword), "AES");
    }

    public String encrypt(String plaintext) throws BoschXmppException {
        try {
            byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
            int paddedLength = (data.length + 15) / 16 * 16;
            byte[] padded = Arrays.copyOf(data, paddedLength);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(padded));
        } catch (GeneralSecurityException e) {
            throw new BoschXmppException(Reason.DECRYPTION, "Could not encrypt EasyControl payload", e);
        }
    }

    public String decrypt(String ciphertext) throws BoschXmppException {
        try {
            byte[] encrypted = Base64.getDecoder().decode(ciphertext.strip());
            if (encrypted.length == 0 || encrypted.length % 16 != 0) {
                throw new BoschXmppException(Reason.DECRYPTION, "Invalid encrypted payload length");
            }
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encrypted);
            int length = decrypted.length;
            while (length > 0 && decrypted[length - 1] == 0) {
                length--;
            }
            return new String(decrypted, 0, length, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new BoschXmppException(Reason.DECRYPTION, "Could not decrypt EasyControl payload", e);
        }
    }

    byte[] getKey() {
        return key.getEncoded();
    }

    private static byte[] deriveKey(String accessKey, String devicePassword) throws BoschXmppException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(accessKey.getBytes(StandardCharsets.UTF_8));
            byte[] first = md5.digest(MAGIC);
            md5.reset();
            md5.update(MAGIC);
            byte[] second = md5.digest(devicePassword.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[32];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new BoschXmppException(Reason.DECRYPTION, "Could not derive EasyControl key", e);
        }
    }

    private static byte[] hexToBytes(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Serves the pairing page and supplies a fresh PKCE verifier for openHAB sign-in.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@Component(service = Servlet.class, property = HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN
        + "=/hisensetv/pairing")
@NonNullByDefault
public class HisenseTVPairingPage extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doGet(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws IOException {
        if (response == null) {
            return;
        }
        try (InputStream resource = HisenseTVPairingPage.class.getResourceAsStream("/web/pairing.html")) {
            if (resource == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            String verifier = randomValue();
            String nonce = randomValue();
            String html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Content-Security-Policy",
                    "default-src 'none'; connect-src 'self'; style-src 'unsafe-inline'; script-src 'nonce-" + nonce
                            + "'; form-action 'none'; base-uri 'none'; frame-ancestors 'none'");
            response.getWriter().write(html.replace("@VERIFIER@", verifier).replace("@CHALLENGE@", challenge(verifier))
                    .replace("@NONCE@", nonce));
        }
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

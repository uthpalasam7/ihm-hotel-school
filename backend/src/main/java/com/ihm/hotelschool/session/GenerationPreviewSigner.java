package com.ihm.hotelschool.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.hotelschool.common.web.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class GenerationPreviewSigner {
    private final ObjectMapper json;
    private final Clock clock;
    private final byte[] key;

    GenerationPreviewSigner(ObjectMapper json, Clock clock, @Value("${app.security.jwt-secret}") String secret) {
        this.json = json;
        this.clock = clock;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    String issue(Object context) {
        long expires = clock.instant().plusSeconds(1800).getEpochSecond();
        return expires + "." + signature(expires, context);
    }

    Instant expiresAt(String token) { return Instant.ofEpochSecond(Long.parseLong(token.split("\\.")[0])); }

    void verify(String token, Object context) {
        if (token == null || !token.matches("[0-9]{1,12}\\.[0-9a-f]{64}")) throw stale();
        long expires = Long.parseLong(token.split("\\.")[0]);
        if (!clock.instant().isBefore(Instant.ofEpochSecond(expires)) || !MessageDigest.isEqual(
                token.substring(token.indexOf('.') + 1).getBytes(StandardCharsets.UTF_8),
                signature(expires, context).getBytes(StandardCharsets.UTF_8))) throw stale();
    }

    private String signature(long expires, Object context) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("session-generation-v1:" + expires + ":"
                    + json.writeValueAsString(context)).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not validate the session preview", exception);
        }
    }

    private ConflictException stale() {
        return new ConflictException("Session preview is missing, expired or changed. Preview the sessions again.");
    }
}

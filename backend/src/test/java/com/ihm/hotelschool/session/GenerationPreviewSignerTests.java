package com.ihm.hotelschool.session;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.hotelschool.common.web.ConflictException;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GenerationPreviewSignerTests {
    private final Instant now = Instant.parse("2026-10-04T00:00:00Z");
    private final String secret = "test-only-preview-key-not-a-real-secret";

    @Test void tokenExpiresAndRejectsChangedContextTamperingAndDifferentKey() {
        var signer = signer(now, secret);
        var context = Map.of("batch", 1, "actor", 2, "version", 3);
        var token = signer.issue(context);
        assertThatCode(() -> signer.verify(token, context)).doesNotThrowAnyException();
        assertThat(signer.expiresAt(token)).isEqualTo(now.plusSeconds(1800));
        assertThatThrownBy(() -> signer(now.plusSeconds(1800), secret).verify(token, context)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> signer.verify(token, Map.of("batch", 2))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> signer.verify(token.substring(0, token.length()-1), context)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> signer(now, "another-test-key").verify(token, context)).isInstanceOf(ConflictException.class);
    }
    private GenerationPreviewSigner signer(Instant instant, String key) {
        return new GenerationPreviewSigner(new ObjectMapper(), Clock.fixed(instant, ZoneOffset.UTC), key);
    }
}

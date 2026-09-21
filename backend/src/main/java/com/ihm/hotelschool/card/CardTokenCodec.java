package com.ihm.hotelschool.card;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CardTokenCodec {
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;
    CardTokenCodec(@Value("${app.card.secret}") String secret) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("Configure a card secret of at least 32 characters");
        this.key = new SecretKeySpec(digest(secret.getBytes(StandardCharsets.UTF_8)), "AES");
    }
    SealedToken issue() {
        byte[] randomBytes = new byte[32]; random.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        byte[] iv = new byte[12]; random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128,iv));
            return new SealedToken(hash(token), cipher.doFinal(token.getBytes(StandardCharsets.US_ASCII)), iv);
        } catch (Exception exception) { throw new IllegalStateException("Could not create card credential",exception); }
    }
    public String unseal(StudentCard card) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128,card.getTokenIv()));
            String token = new String(cipher.doFinal(card.getTokenCiphertext()), StandardCharsets.US_ASCII);
            if (!hash(token).equals(card.getTokenHash())) throw new IllegalStateException("Card credential failed integrity check");
            return token;
        } catch (Exception exception) { throw new IllegalStateException("Card credential cannot be opened",exception); }
    }
    public String hash(String token) { return HexFormat.of().formatHex(digest(token.getBytes(StandardCharsets.UTF_8))); }
    private static byte[] digest(byte[] input) {
        try { return MessageDigest.getInstance("SHA-256").digest(input); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable",exception); }
    }
    record SealedToken(String hash, byte[] ciphertext, byte[] iv) {}
}

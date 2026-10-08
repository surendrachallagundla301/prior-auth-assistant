package com.surendra.priorauth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Field-level AES-256-GCM encryption for PHI. Each value gets a random 12-byte IV, stored
 * in front of the ciphertext, so equal plaintexts never produce equal ciphertexts.
 *
 * <p>The key comes from {@code PHI_ENCRYPTION_KEY} (base64, 32 bytes). In production it would
 * be fetched from a KMS-backed secret store, never committed.
 */
@Component
public class PhiEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static volatile PhiEncryptor instance;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PhiEncryptor(@Value("${phi.encryption-key}") String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("phi.encryption-key must be 32 bytes (AES-256), base64-encoded");
        }
        this.key = new SecretKeySpec(raw, "AES");
        instance = this;
    }

    /** Lets JPA converters (created by Hibernate, not Spring) reach the configured encryptor. */
    static PhiEncryptor get() {
        PhiEncryptor current = instance;
        if (current == null) {
            throw new IllegalStateException("PhiEncryptor has not been initialised");
        }
        return current;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt PHI field", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt PHI field", e);
        }
    }
}

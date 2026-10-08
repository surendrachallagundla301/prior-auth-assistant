package com.surendra.priorauth.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhiEncryptorTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final PhiEncryptor encryptor = new PhiEncryptor(KEY);

    @Test
    void roundTripsPlaintext() {
        String encrypted = encryptor.encrypt("ABC123456789");
        assertThat(encrypted).doesNotContain("ABC123456789");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("ABC123456789");
    }

    @Test
    void samePlaintextEncryptsDifferentlyEachTime() {
        assertThat(encryptor.encrypt("Jane Doe")).isNotEqualTo(encryptor.encrypt("Jane Doe"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] raw = Base64.getDecoder().decode(encryptor.encrypt("1985-04-12"));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new PhiEncryptor(shortKey)).isInstanceOf(IllegalArgumentException.class);
    }
}

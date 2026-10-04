package com.skillmatch.backend.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    void resolveSigningKey_configuredSecret_usesItAsKey() {
        String secret = "a-configured-secret-of-at-least-32-bytes!";

        SecretKey key = JwtTokenProvider.resolveSigningKey(secret);

        assertThat(key.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void resolveSigningKey_missingSecret_generatesRandom256BitKey(String missingSecret) {
        SecretKey first = JwtTokenProvider.resolveSigningKey(missingSecret);
        SecretKey second = JwtTokenProvider.resolveSigningKey(missingSecret);

        assertThat(first.getEncoded()).hasSize(32);
        assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }

    @Test
    void resolveSigningKey_shortSecret_failsFast() {
        assertThatThrownBy(() -> JwtTokenProvider.resolveSigningKey("too-short"))
                .isInstanceOf(WeakKeyException.class);
    }
}

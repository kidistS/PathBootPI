package com.pathboot.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheKeyUtil – Unit Tests")
class CacheKeyUtilTest {

    // ── Null / empty input ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Null and empty input")
    class NullAndEmpty {

        @Test
        @DisplayName("null input returns literal 'null'")
        void nullInput_returnsLiteralNull() {
            assertThat(CacheKeyUtil.hashQuestion(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("empty string produces a 64-char hex hash (not empty)")
        void emptyString_produces64CharHash() {
            String hash = CacheKeyUtil.hashQuestion("");
            assertThat(hash).hasSize(64);
        }
    }

    // ── Hash properties ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hash properties")
    class HashProperties {

        @Test
        @DisplayName("hash is always 64 hex characters (SHA-256)")
        void hash_isAlways64Chars() {
            assertThat(CacheKeyUtil.hashQuestion("What is income tax?")).hasSize(64);
            assertThat(CacheKeyUtil.hashQuestion("a")).hasSize(64);
            assertThat(CacheKeyUtil.hashQuestion("x".repeat(4000))).hasSize(64);
        }

        @Test
        @DisplayName("hash is lowercase hex")
        void hash_isLowercaseHex() {
            String hash = CacheKeyUtil.hashQuestion("Hello");
            assertThat(hash).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("same input always produces the same hash (deterministic)")
        void sameInput_samehash() {
            String question = "What is the tax filing deadline in Norway?";
            assertThat(CacheKeyUtil.hashQuestion(question))
                    .isEqualTo(CacheKeyUtil.hashQuestion(question));
        }

        @Test
        @DisplayName("different inputs produce different hashes (collision resistance)")
        void differentInputs_differentHashes() {
            String h1 = CacheKeyUtil.hashQuestion("What is income tax?");
            String h2 = CacheKeyUtil.hashQuestion("What is VAT?");
            assertThat(h1).isNotEqualTo(h2);
        }

        @Test
        @DisplayName("SHA-256 of 'hello' matches known value")
        void knownHash_matchesSha256Spec() {
            // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
            assertThat(CacheKeyUtil.hashQuestion("hello"))
                    .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        }

        @Test
        @DisplayName("long question (4000 chars) still produces 64-char hash")
        void longQuestion_stillProduces64CharHash() {
            String longQuestion = "tax ".repeat(1000);  // 4000 chars
            assertThat(CacheKeyUtil.hashQuestion(longQuestion)).hasSize(64);
        }
    }
}


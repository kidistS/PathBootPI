package com.pathboot.util;

import com.pathboot.exception.LlmCommunicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataGroundingLoader – Unit Tests")
class DataGroundingLoaderTest {

    private DataGroundingLoader loader;

    // A real classpath file that is always present (compiled from main/resources).
    private static final String VALID_PATH    = "grounding/tax/tax-grounding.txt";
    private static final String MISSING_PATH  = "grounding/nonexistent/ghost.txt";

    @BeforeEach
    void setUp() {
        loader = new DataGroundingLoader();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("loads a real classpath grounding file and returns non-blank content")
        void loadValidFile_returnsContent() {
            String content = loader.loadGroundingContent(VALID_PATH);
            assertThat(content).isNotBlank();
        }

        @Test
        @DisplayName("second call for the same path returns the same cached content")
        void secondCall_returnsCachedContent() {
            String first  = loader.loadGroundingContent(VALID_PATH);
            String second = loader.loadGroundingContent(VALID_PATH);
            assertThat(second).isSameAs(first);   // same object reference = cache hit
        }

        @Test
        @DisplayName("each domain grounding file loads successfully")
        void allDomainFiles_loadSuccessfully() {
            assertThat(loader.loadGroundingContent("grounding/tax/tax-grounding.txt")).isNotBlank();
            assertThat(loader.loadGroundingContent("grounding/nav/nav-grounding.txt")).isNotBlank();
            assertThat(loader.loadGroundingContent("grounding/immigration/immigration-grounding.txt"))
                    .isNotBlank();
        }
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("missing classpath file throws LlmCommunicationException")
        void missingFile_throwsLlmCommunicationException() {
            assertThatThrownBy(() -> loader.loadGroundingContent(MISSING_PATH))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining(MISSING_PATH);
        }
    }
}


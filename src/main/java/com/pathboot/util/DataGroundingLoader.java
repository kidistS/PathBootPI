package com.pathboot.util;

import com.pathboot.exception.LlmCommunicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads domain-specific data-grounding text files from the classpath.
 *
 * <p>Files are cached in memory after the first read so they are not re-read on every request.
 * This implements the <em>Flyweight</em> pattern – the same grounding content is shared
 * across all invocations for a given domain.</p>
 */
@Component
public class DataGroundingLoader {

    private static final Logger logger = LogManager.getLogger(DataGroundingLoader.class);

    /** In-memory cache: classpath path → file content. */
    private final Map<String, String> groundingCache = new ConcurrentHashMap<>();

    /**
     * Loads and returns the grounding content for the given classpath resource path.
     *
     * @param classpathFilePath path relative to the classpath root, e.g.
     *                          {@code "grounding/tax/tax-grounding.txt"}
     * @return the file content as a UTF-8 string
     * @throws LlmCommunicationException if the file cannot be read
     */
    public String loadGroundingContent(String classpathFilePath) {
        return groundingCache.computeIfAbsent(classpathFilePath, this::readGroundingFile);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String readGroundingFile(String classpathFilePath) {
        logger.info("Loading grounding file from classpath: {}", classpathFilePath);
        try {
            ClassPathResource resource = new ClassPathResource(classpathFilePath);
            byte[] bytes = resource.getInputStream().readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            logger.debug("Grounding file loaded ({} chars): {}", content.length(), classpathFilePath);
            return content;
        } catch (IOException ex) {
            logger.error("Failed to load grounding file '{}': {}", classpathFilePath, ex.getMessage(), ex);
            throw new LlmCommunicationException(
                    "Could not load domain grounding file: " + classpathFilePath, ex);
        }
    }
}


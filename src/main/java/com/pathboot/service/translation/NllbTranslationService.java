package com.pathboot.service.translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pathboot.enums.Language;
import com.pathboot.exception.TranslationException;
import com.pathboot.util.PathBootConstants;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Translation service that calls the local NLLB-200-Distilled-600M Python server
 * for Amharic ↔ English translations.
 *
 * <p>Acts as an <em>Adapter</em> wrapping the HTTP REST call to the NLLB server.</p>
 *
 * <p>The NLLB server must be running at the URL configured in {@code application.yml}
 * ({@code nllb.server.base-url}). See {@code nllb_server.py} in the project root.</p>
 *
 * <h3>Resilience</h3>
 * <ul>
 *   <li>On application start a warm-up POST to {@code /warmup} is sent so the model
 *       is hot before the first real user request.</li>
 *   <li>A single automatic retry is attempted on {@link ResourceAccessException} (I/O
 *       timeout) to handle cases where the server was momentarily overloaded.</li>
 * </ul>
 */
@Service("nllbTranslationService")
public class NllbTranslationService implements TranslationService {

    private static final Logger logger = LogManager.getLogger(NllbTranslationService.class);

    /** How many times to retry a translation on I/O timeout before giving up. */
    private static final int MAX_RETRY_ATTEMPTS = 1;

    private final RestTemplate nllbRestTemplate;
    private final String       nllbTranslateUrl;
    /** Base URL without the endpoint path – used to build /warmup URL. */
    private final String       nllbBaseUrl;

    public NllbTranslationService(@Qualifier("nllbRestTemplate") RestTemplate nllbRestTemplate,
                                   @Qualifier("nllbTranslateUrl") String nllbTranslateUrl,
                                   @Value("${nllb.server.base-url}") String nllbBaseUrl) {
        this.nllbRestTemplate = nllbRestTemplate;
        this.nllbTranslateUrl = nllbTranslateUrl;
        this.nllbBaseUrl      = nllbBaseUrl;
    }

    // ─── Startup warm-up ──────────────────────────────────────────────────────

    /**
     * Sends a warm-up POST to the NLLB server after the application is fully started.
     * This pre-loads the model pipeline so the first real translation request is fast.
     * Non-fatal: if the NLLB server is not running the warning is logged and ignored.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpNllbServer() {
        String warmupUrl = nllbBaseUrl + "/warmup";
        logger.info("Sending NLLB warm-up ping to: {}", warmupUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            nllbRestTemplate.exchange(
                    warmupUrl, HttpMethod.POST,
                    new HttpEntity<>("{}", headers), String.class);
            logger.info("NLLB warm-up ping successful – model is ready.");
        } catch (Exception ex) {
            logger.warn("NLLB warm-up ping failed (server may not be running yet): {}", ex.getMessage());
        }
    }

    // ─── Translation ──────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Supported pairs: Amharic → English and English → Amharic.
     * Automatically retries once on I/O timeout.</p>
     */
    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        logger.info("NLLB translation: {} → {}", sourceLanguage, targetLanguage);

        String nllbSourceCode = resolveNllbCode(sourceLanguage);
        String nllbTargetCode = resolveNllbCode(targetLanguage);

        NllbTranslationRequest requestPayload = NllbTranslationRequest.builder()
                .text(text)
                .sourceLanguage(nllbSourceCode)
                .targetLanguage(nllbTargetCode)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<NllbTranslationRequest> httpEntity = new HttpEntity<>(requestPayload, headers);

        return executeWithRetry(httpEntity, MAX_RETRY_ATTEMPTS);
    }

    // ─── canHandleLanguagePair ────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Handles Amharic ↔ English pairs only.</p>
     */
    @Override
    public boolean canHandleLanguagePair(String sourceLanguage, String targetLanguage) {
        String src = sourceLanguage.toUpperCase();
        String tgt = targetLanguage.toUpperCase();
        return (Language.AMHARIC.name().equals(src) && Language.ENGLISH.name().equals(tgt))
                || (Language.ENGLISH.name().equals(src) && Language.AMHARIC.name().equals(tgt));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Executes the NLLB HTTP call, retrying on I/O timeout up to {@code retriesLeft} times.
     */
    private String executeWithRetry(HttpEntity<NllbTranslationRequest> httpEntity, int retriesLeft) {
        try {
            ResponseEntity<NllbTranslationResponse> responseEntity =
                    nllbRestTemplate.exchange(nllbTranslateUrl, HttpMethod.POST,
                            httpEntity, NllbTranslationResponse.class);

            NllbTranslationResponse responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.getTranslatedText() == null
                    || responseBody.getTranslatedText().isBlank()) {
                throw new TranslationException("NLLB server returned an empty translation response.");
            }

            logger.debug("NLLB translation result (snippet): {}",
                    responseBody.getTranslatedText().length() > 80
                            ? responseBody.getTranslatedText().substring(0, 80) + "…"
                            : responseBody.getTranslatedText());
            return responseBody.getTranslatedText().trim();

        } catch (TranslationException ex) {
            // Rethrow as-is to prevent the generic catch below from double-wrapping it.
            throw ex;
        } catch (ResourceAccessException ex) {
            // I/O error (timeout, connection refused, etc.)
            if (retriesLeft > 0) {
                logger.warn("NLLB I/O error ({}), retrying… ({} attempt(s) left)",
                        ex.getMessage(), retriesLeft);
                return executeWithRetry(httpEntity, retriesLeft - 1);
            }
            logger.error("NLLB server call failed after retries: {}", ex.getMessage(), ex);
            throw new TranslationException(
                    "NLLB translation timed out at " + nllbTranslateUrl
                    + ". The model may need more time on first call – please retry. "
                    + "Ensure nllb_server.py is running.", ex);
        } catch (RestClientException ex) {
            logger.error("NLLB server call failed: {}", ex.getMessage(), ex);
            throw new TranslationException(
                    "Could not reach the NLLB translation server at " + nllbTranslateUrl
                    + ". Ensure nllb_server.py is running.", ex);
        }
    }

    private String resolveNllbCode(String language) {
        String upper = language.toUpperCase();
        if (Language.AMHARIC.name().equals(upper)) return PathBootConstants.NLLB_AMHARIC_CODE;
        if (Language.ENGLISH.name().equals(upper)) return PathBootConstants.NLLB_ENGLISH_CODE;
        throw new TranslationException("Unsupported NLLB language: " + language);
    }

    // ─── Inner DTOs ───────────────────────────────────────────────────────────

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class NllbTranslationRequest {
        private String text;

        @JsonProperty("source_language")
        private String sourceLanguage;

        @JsonProperty("target_language")
        private String targetLanguage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class NllbTranslationResponse {
        @JsonProperty("translated_text")
        private String translatedText;
    }
}


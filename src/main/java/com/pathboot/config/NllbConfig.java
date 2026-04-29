package com.pathboot.config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
/**
 * Configuration for the local NLLB-200-Distilled-600M translation server.
 *
 * <p>The NLLB server is a Python Flask/FastAPI process running locally. See
 * {@code nllb_server.py} in the project root for how to start it.
 * Configurable timeouts prevent the application from blocking indefinitely
 * when the translation server is slow or unavailable.</p>
 */
@Configuration
public class NllbConfig {
    private static final Logger logger = LogManager.getLogger(NllbConfig.class);
    @Value("${nllb.server.base-url}")
    private String nllbServerBaseUrl;
    @Value("${nllb.server.translate-endpoint}")
    private String translateEndpoint;
    @Value("${nllb.server.connect-timeout-ms:5000}")
    private int connectTimeoutMs;
    @Value("${nllb.server.read-timeout-ms:120000}")
    private int readTimeoutMs;
    /**
     * Returns the full URL for the NLLB translation endpoint.
     *
     * @return URL string, e.g. {@code http://localhost:5000/translate}
     */
    @Bean(name = "nllbTranslateUrl")
    public String nllbTranslateUrl() {
        String fullUrl = nllbServerBaseUrl + translateEndpoint;
        logger.info("NLLB translate URL configured as: {}", fullUrl);
        return fullUrl;
    }
    /**
     * Provides a {@link RestTemplate} with configured timeouts for NLLB server calls.
     *
     * @param builder Spring-managed builder with timeout support
     * @return configured RestTemplate
     */
    @Bean(name = "nllbRestTemplate")
    public RestTemplate nllbRestTemplate(RestTemplateBuilder builder) {
        logger.info("NLLB RestTemplate timeouts – connect: {}ms, read: {}ms",
                connectTimeoutMs, readTimeoutMs);
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
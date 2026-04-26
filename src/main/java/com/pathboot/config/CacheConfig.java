package com.pathboot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine-backed in-memory caching for the application.
 *
 * <h3>Cache names</h3>
 * <ul>
 *   <li>{@code domainAnswers} – caches LLM-generated answers keyed by
 *       {@code domainType + "_" + language + "_" + question}.  Entry expires after
 *       60 minutes and the cache holds at most 500 entries.</li>
 * </ul>
 *
 * <p>The {@link EnableCaching} annotation activates Spring's caching abstraction,
 * allowing {@code @Cacheable} annotations to intercept method calls and return
 * cached results without hitting Ollama again for repeated questions.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LogManager.getLogger(CacheConfig.class);

    public static final String DOMAIN_ANSWERS_CACHE = "domainAnswers";
    public static final int    CACHE_MAX_SIZE        = 500;
    public static final long   CACHE_EXPIRE_MINUTES  = 60L;

    /**
     * Creates a {@link CaffeineCacheManager} with a single named cache for domain answers.
     *
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        logger.info("Initialising Caffeine cache '{}' (maxSize={}, expireAfter={}m)",
                DOMAIN_ANSWERS_CACHE, CACHE_MAX_SIZE, CACHE_EXPIRE_MINUTES);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DOMAIN_ANSWERS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}

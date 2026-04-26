package com.pathboot.service.classification;

import com.pathboot.enums.DomainType;
import com.pathboot.util.PathBootConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Classifies a question into one of the supported domains (TAX, NAV, IMMIGRATION)
 * using fast keyword matching – <strong>no LLM call required</strong>.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Lowercase the input text.</li>
 *   <li>Count keyword hits for each domain.</li>
 *   <li>Return the domain with the most hits.</li>
 *   <li>Tie or no hit → {@link DomainType#UNKNOWN}.</li>
 * </ol>
 *
 * <p>Keyword lists cover both English and Norwegian terms so the classifier works
 * even when the original (non-Amharic) text is passed directly without prior translation.</p>
 *
 * <p>Replaces the previous LLM-based classifier, removing one full Ollama round-trip
 * and dramatically reducing end-to-end latency.</p>
 */
@Service
public class DomainClassificationService {

    private static final Logger logger = LogManager.getLogger(DomainClassificationService.class);

    /**
     * Classifies the given text into a {@link DomainType} using keyword scoring.
     *
     * @param text the question text (English or Norwegian)
     * @return detected domain, or {@link DomainType#UNKNOWN} if no domain matches
     */
    public DomainType classifyDomain(String text) {
        if (text == null || text.isBlank()) {
            logger.warn("classifyDomain called with null/blank text – returning UNKNOWN");
            return DomainType.UNKNOWN;
        }

        String lowerText = text.toLowerCase();

        int taxScore         = countKeywordHits(lowerText, PathBootConstants.TAX_KEYWORDS);
        int navScore         = countKeywordHits(lowerText, PathBootConstants.NAV_KEYWORDS);
        int immigrationScore = countKeywordHits(lowerText, PathBootConstants.IMMIGRATION_KEYWORDS);

        logger.debug("Domain scores – TAX: {}, NAV: {}, IMMIGRATION: {}", taxScore, navScore, immigrationScore);

        DomainType result = selectDomainByHighestScore(taxScore, navScore, immigrationScore);
        logger.info("Domain classified as: {} (scores: TAX={}, NAV={}, IMMIGRATION={})",
                result, taxScore, navScore, immigrationScore);
        return result;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private int countKeywordHits(String lowerText, String[] keywords) {
        int hits = 0;
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                hits++;
            }
        }
        return hits;
    }

    private DomainType selectDomainByHighestScore(int taxScore, int navScore, int immigrationScore) {
        int maxScore = Math.max(taxScore, Math.max(navScore, immigrationScore));
        if (maxScore == 0) {
            return DomainType.UNKNOWN;
        }
        // Ties fall through to UNKNOWN so the facade can apply its own fallback
        int winners = (taxScore == maxScore ? 1 : 0)
                    + (navScore == maxScore ? 1 : 0)
                    + (immigrationScore == maxScore ? 1 : 0);
        if (winners > 1) {
            logger.debug("Domain classification tie – returning UNKNOWN");
            return DomainType.UNKNOWN;
        }
        if (taxScore == maxScore)         return DomainType.TAX;
        if (navScore == maxScore)         return DomainType.NAV;
        return DomainType.IMMIGRATION;
    }
}


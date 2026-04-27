package com.pathboot.service.rag;

import com.pathboot.enums.DomainType;
import com.pathboot.util.DataGroundingLoader;
import com.pathboot.util.PathBootConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagGroundingService implements SmartInitializingSingleton {

    private static final Logger logger = LogManager.getLogger(RagGroundingService.class);

    private final SimpleVectorStore vectorStore;
    private final DataGroundingLoader dataGroundingLoader;

    @Value("${rag.top-k:3}")
    private int defaultTopK;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.chunk-min-length:30}")
    private int chunkMinLength;

    /** Path where the embedded vector store is persisted between restarts. */
    @Value("${rag.store-path:./data/vector-store.json}")
    private String storePath;

    /** Ollama base URL – used to pull missing models. */
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /** Embedding model name – pulled automatically if Ollama does not have it yet. */
    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModel;

    /** Written once after initialisation; volatile for safe multi-thread visibility. */
    private volatile boolean vectorStoreReady = false;

    public RagGroundingService(SimpleVectorStore vectorStore,
                                DataGroundingLoader dataGroundingLoader) {
        this.vectorStore         = vectorStore;
        this.dataGroundingLoader = dataGroundingLoader;
    }

    // ─── Startup document loading ─────────────────────────────────────────────
    /**
     * Called by Spring during context refresh, <b>before</b> the embedded web server opens its port.  Guarantees the vector
     * store is populated before any HTTP request can arrive.
     *
     * <p>On subsequent restarts the fast path (load from disk) completes in under a second, so startup latency is negligible.</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (!ragEnabled) {
            logger.info("[RAG] Disabled (rag.enabled=false) – skipping vector store population");
            return;
        }
        try {
            ensureEmbeddingModelAvailable();   // pull model if Ollama doesn't have it yet

            File storeFile = new File(storePath);
            if (storeFile.exists()) {
                // ── Fast path: load pre-computed embeddings from disk ──────────
                logger.info("[RAG] Loading persisted vector store from: {}", storeFile.getAbsolutePath());
                vectorStore.load(storeFile);
                logger.info("[RAG] Vector store restored from disk – no re-embedding needed");
            } else {
                // ── Slow path: embed all grounding files and save to disk ──────
                logger.info("[RAG] No persisted store found – embedding grounding files (first-run only)");
                loadDomainFile(PathBootConstants.TAX_GROUNDING_FILE,         DomainType.TAX.name());
                loadDomainFile(PathBootConstants.NAV_GROUNDING_FILE,         DomainType.NAV.name());
                loadDomainFile(PathBootConstants.IMMIGRATION_GROUNDING_FILE, DomainType.IMMIGRATION.name());

                storeFile.getParentFile().mkdirs();
                vectorStore.save(storeFile);
                logger.info("[RAG] Vector store persisted to: {}", storeFile.getAbsolutePath());
            }
            vectorStoreReady = true;
            logger.info("[RAG] Vector store ready – port will open now");
        } catch (Exception ex) {
            logger.warn("[RAG] Failed to initialise vector store – falling back to full-file context. Cause: {}",
                    ex.getMessage());
        }
    }

    // ─── Query-time retrieval ──────────────────────────────────────────────────
    /**
     * Returns the most relevant grounding context for the given question and domain.
     * Falls back to the full grounding file if RAG is disabled or unavailable.
     *
     * @param question          user question in English
     * @param domain            classified domain (used as metadata filter in the vector search)
     * @param groundingFilePath classpath path to the domain full grounding file (fallback)
     * @return context text to inject into the LLM system prompt
     */
    public String findRelevantContext(String question,
                                       DomainType domain,
                                       String groundingFilePath) {
        if (!ragEnabled || !vectorStoreReady) {
            logger.debug("[RAG] Not ready – using full grounding file for domain: {}", domain);
            return dataGroundingLoader.loadGroundingContent(groundingFilePath);
        }
        try {
            return retrieveTopKChunks(question, domain, groundingFilePath);
        } catch (Exception ex) {
            logger.warn("[RAG] Vector search failed for domain {} – falling back. Cause: {}",
                    domain, ex.getMessage());
            return dataGroundingLoader.loadGroundingContent(groundingFilePath);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────
    /**
     * Checks whether the configured embedding model is present in Ollama. If it is missing, issues a {@code POST /api/pull}
     * request to pull it automatically. This ensures the embedding model is available without requiring manual setup steps.
     */
    private void ensureEmbeddingModelAvailable() {
        try {
            RestTemplate rt = new RestTemplate();
            String tagsUrl = ollamaBaseUrl + "/api/tags";
            ResponseEntity<String> tagsResp = rt.getForEntity(tagsUrl, String.class);

            if (tagsResp.getBody() != null && tagsResp.getBody().contains(embeddingModel)) {
                logger.info("[RAG] Embedding model '{}' is already available", embeddingModel);
                return;
            }

            // Model not present – pull it automatically
            logger.info("[RAG] Embedding model '{}' not found in Ollama – pulling now "
                    + "(this may take several minutes on first run)…", embeddingModel);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(15 * 60 * 1_000);   // model download can take minutes
            RestTemplate pullRt = new RestTemplate(factory);

            Map<String, Object> pullBody = Map.of("name", embeddingModel, "stream", false);
            pullRt.postForEntity(ollamaBaseUrl + "/api/pull", pullBody, String.class);

            logger.info("[RAG] Embedding model '{}' pulled successfully", embeddingModel);

        } catch (Exception ex) {
            logger.warn("[RAG] Could not auto-pull embedding model '{}': {} – will attempt embedding anyway",
                    embeddingModel, ex.getMessage());
        }
    }

    /**
     * Retrieves exactly {@code top-k} chunks for the given domain using a metadata filter expression evaluated inside the
     * vector store – no over-fetch, no Java-side filtering.
     */
    private String retrieveTopKChunks(String question,
                                       DomainType domain,
                                       String groundingFilePath) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(defaultTopK)
                .filterExpression("domain == '" + domain.name() + "'")
                .build();

        List<Document> domainChunks = vectorStore.similaritySearch(request);

        if (domainChunks.isEmpty()) {
            logger.warn("[RAG] No chunks found for domain {} – falling back to full grounding", domain);
            return dataGroundingLoader.loadGroundingContent(groundingFilePath);
        }

        String context = domainChunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        logger.debug("[RAG] {} chunks retrieved for domain {} ({} chars)",
                domainChunks.size(), domain, context.length());
        return context;
    }

    private void loadDomainFile(String classpathPath, String domainName) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        String rawContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<Document> chunks = chunkIntoDocuments(rawContent, domainName, classpathPath);
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
            logger.info("[RAG] Embedded {} chunks for domain [{}] from {}",
                    chunks.size(), domainName, classpathPath);
        }
    }

    /**
     * Splits raw grounding text into paragraph-level {@link Document} chunks with domain and source metadata
     * for downstream filtering.
     */
    private List<Document> chunkIntoDocuments(String rawContent,
                                               String domainName,
                                               String sourcePath) {
        List<Document> documents = new ArrayList<>();
        String[] paragraphs = rawContent.split("(?m)^\\s*---\\s*$|\\n{2,}");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.length() < chunkMinLength || trimmed.startsWith("#!")) {
                continue;
            }
            documents.add(new Document(trimmed,
                    Map.of("domain", domainName, "source", sourcePath)));
        }
        return documents;
    }
}
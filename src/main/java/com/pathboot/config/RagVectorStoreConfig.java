package com.pathboot.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the {@link SimpleVectorStore} bean used for RAG across all domain agents.
 *
 * <p>The concrete type {@link SimpleVectorStore} is exposed (not just the {@link
 * org.springframework.ai.vectorstore.VectorStore} interface) so that
 * {@link com.pathboot.service.rag.RagGroundingService} can call
 * {@code save(File)} / {@code load(File)} to persist embeddings across restarts.</p>
 */
@Configuration
public class RagVectorStoreConfig {

    private static final Logger logger = LogManager.getLogger(RagVectorStoreConfig.class);

    /**
     * Creates the singleton {@link SimpleVectorStore} backed by the Ollama embedding model.
     *
     * @param embeddingModel auto-configured Ollama embedding model
     * @return in-memory vector store (populated / restored by {@link com.pathboot.service.rag.RagGroundingService})
     */
    @Bean
    public SimpleVectorStore domainVectorStore(EmbeddingModel embeddingModel) {
        logger.info("Creating SimpleVectorStore backed by Ollama embedding model");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
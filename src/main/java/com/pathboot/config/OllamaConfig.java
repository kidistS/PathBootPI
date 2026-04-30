package com.pathboot.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Ollama / Mistral LLM integration via Spring AI.
 *
 * <p>Spring AI auto - configures {@code OllamaChatModel} from {@code application.yml}
 * properties. Here we expose a singleton {@link ChatClient} bean backed by that model.</p>
 */
@Configuration
public class OllamaConfig {

    private static final Logger logger = LogManager.getLogger(OllamaConfig.class);

    /**
     * Provides a single {@link ChatClient} instance backed by the configured Ollama model.
     *
     * <p>Follows the <em>Singleton</em> pattern – Spring manages the lifecycle.</p>
     *
     * @param chatClientBuilder auto-wired by Spring AI's auto-configuration
     * @return a ready-to-use ChatClient
     */
    @Bean
    public ChatClient ollamaChatClient(ChatClient.Builder chatClientBuilder) {
        logger.info("Initialising Ollama ChatClient (Mistral model)");
        return chatClientBuilder.build();
    }
}


package com.pathboot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PathBootApplication {

    private static final Logger logger = LogManager.getLogger(PathBootApplication.class);

    public static void main(String[] args) {
        logger.info("=== Starting PathBoot PI Multilingual Q&A Application ===");
        SpringApplication.run(PathBootApplication.class, args);
    }

    /**
     * Fires only after the application context is fully refreshed AND the
     * embedded web server is ready to accept requests — guarantees the log
     * line never appears when context startup fails.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        logger.info("=== PathBoot PI started – Swagger: http://localhost:8080/swagger-ui.html ===");
    }
}


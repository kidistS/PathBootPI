package com.pathboot.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures asynchronous execution and scheduled task support.
 *
 * <ul>
 *   <li>{@link EnableScheduling} activates the {@code @Scheduled} annotation, which is
 *       used by {@link com.pathboot.session.UserSessionManager#evictStaleSessions()} to
 *       prevent unbounded growth of the in-memory session map.</li>
 *   <li>The custom {@link ThreadPoolTaskExecutor} is used for all {@code @Async} fire-and-forget
 *       persistence calls. Pool size is deliberately small (core=2, max=4) because the workload
 *       is I/O-bound SQLite writes that must not starve the main request threads.</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LogManager.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("pathboot-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // Instead of silently dropping tasks when the queue is full, run them on the
        // calling thread. This prevents undetected data loss under burst load.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        logger.info("Async task executor initialised – core: 2, max: 4, queue: 500, rejected: CallerRunsPolicy");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                logger.error("Uncaught exception in @Async method {}: {}",
                        method.getName(), ex.getMessage(), ex);
    }
}


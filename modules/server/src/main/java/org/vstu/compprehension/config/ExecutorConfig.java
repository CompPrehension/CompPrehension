package org.vstu.compprehension.config;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.vstu.compprehension.utils.threads.ContextAwarePoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig implements AsyncConfigurer {
    @Bean
    @Override
    public Executor getAsyncExecutor() {
        val executor = new ContextAwarePoolTaskExecutor();
        return executor;
    }
}

package org.vstu.compprehension.config;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.vstu.compprehension.utils.threads.ContextAwarePoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig extends AsyncConfigurerSupport {
    @Bean
    @Override
    public Executor getAsyncExecutor() {
        val executor = new ContextAwarePoolTaskExecutor();
        return executor;
    }
}

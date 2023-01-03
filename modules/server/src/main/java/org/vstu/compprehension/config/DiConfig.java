package org.vstu.compprehension.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapters.BackendTaskQueue;
import org.vstu.compprehension.adapters.CachedUserService;
import org.vstu.compprehension.adapters.RateLimitBackendDecorator;
import org.vstu.compprehension.adapters.UserServiceImpl;
import org.vstu.compprehension.models.businesslogic.backend.*;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.List;

@Configuration
public class DiConfig {
    @Bean
    @Qualifier("allBackends")
    @RequestScope
    List<Backend> getAllBackends(@Autowired @Lazy JenaBackend jenaBackend, @Autowired @Lazy PelletBackend pelletBackend, @Autowired BackendTaskQueue taskQueue) {
        return List.of(
              new RateLimitBackendDecorator(JenaBackend.BackendId, jenaBackend, taskQueue),
              new RateLimitBackendDecorator(PelletBackend.BackendId, pelletBackend, taskQueue)
        );
    }

    @Bean
    @SessionScope
    UserService getUserService(@Autowired UserRepository userRepository) {
        return new CachedUserService(new UserServiceImpl(userRepository));
    }
}

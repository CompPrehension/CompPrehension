package org.vstu.compprehension.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapters.*;
import org.vstu.compprehension.models.businesslogic.backend.*;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.repository.UserRepository;

import javax.inject.Singleton;
import java.util.List;

@Configuration
public class DiConfig {
    @Bean
    @Qualifier("allBackends")
    @RequestScope
    List<Backend> getAllBackends(
            @Autowired @Lazy JenaBackend jenaBackend,
            @Autowired @Lazy PelletBackend pelletBackend,
            @Autowired BackendTaskQueue taskQueue,
		    @Autowired Cache<String, JenaFactList> jenaCache) {
        return List.of(
              new RateLimitBackendDecorator(
                      JenaBackend.BackendId,
                      new SolutionCachingJenaBackendDecorator(jenaBackend, jenaCache),
                      taskQueue),
              new RateLimitBackendDecorator(PelletBackend.BackendId, pelletBackend, taskQueue)
        );
    }

    @Bean
    @SessionScope
    UserService getUserService(@Autowired UserRepository userRepository) {
        return new CachedUserService(new UserServiceImpl(userRepository));
    }

    @Bean
    @Singleton
    Cache<String, JenaFactList> getJenaSolveCache() {
        return CacheBuilder.from("maximumSize=30,expireAfterAccess=10m").build();
    }
}

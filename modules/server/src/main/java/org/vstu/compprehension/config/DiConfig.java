package org.vstu.compprehension.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapters.*;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.backend.PelletBackend;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.strategies.GradeConfidenceBaseStrategy;
import org.vstu.compprehension.strategies.GradeConfidenceBaseStrategy_Manual50Autogen50;
import org.vstu.compprehension.strategies.StaticStrategy;
import org.vstu.compprehension.strategies.Strategy;
import org.vstu.compprehension.utils.RandomProvider;

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
            @Autowired DecisionTreeReasonerBackend decisionTreeReasonerBackend,
            @Autowired BackendTaskQueue taskQueue,
            @Autowired Cache<String, JenaFactList> jenaCache)
    {
        return List.of(
            new RateLimitBackendDecorator(
                JenaBackend.BackendId,
                new SolutionCachingJenaBackendDecorator(jenaBackend, jenaCache),
                taskQueue
            ),
            decisionTreeReasonerBackend, //FIXME wrap with RateLimitBackendDecorator
            new RateLimitBackendDecorator(PelletBackend.BackendId, pelletBackend, taskQueue)
        );
    }


    @Bean
    @Singleton @Primary
    GradeConfidenceBaseStrategy getGradeConfidenceBaseStrategy(@Autowired DomainFactory domainFactory) {
        return new GradeConfidenceBaseStrategy(domainFactory);
    }
    @Bean
    @Singleton
    GradeConfidenceBaseStrategy_Manual50Autogen50 getGradeConfidenceBaseStrategy_Manual50Autogen50(@Autowired DomainFactory domainFactory, @Autowired RandomProvider randomProvider) {
        return new GradeConfidenceBaseStrategy_Manual50Autogen50(domainFactory, randomProvider);
    }
    @Bean
    @Singleton
    StaticStrategy getStaticStrategy(@Autowired DomainFactory domainFactory) {
        return new StaticStrategy(domainFactory);
    }
    @Bean
    @Singleton
    Strategy getStrategy(@Autowired DomainFactory domainFactory, @Autowired RandomProvider randomProvider) {
        return new Strategy(domainFactory, randomProvider);
    }

    @Bean
    @SessionScope
    UserService getUserService(@Autowired UserRepository userRepository) {
        return new CachedUserService(new UserServiceImpl(userRepository));
    }

    @Bean
    @Singleton
    QuestionBank getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionMetadataRepository metadataRepository,
            @Autowired QuestionDataRepository questionDataRepository,
            @Autowired QuestionGenerationRequestRepository generationRequestRepository) throws Exception {
        //var allDomains = domainRepository.findAll();
        return new QuestionBank(metadataRepository, questionDataRepository, new QuestionMetadataManager(metadataRepository), generationRequestRepository);
    }

    @Bean
    @Singleton
    Cache<String, JenaFactList> getJenaSolveCache() {
        return CacheBuilder.from("maximumSize=30,expireAfterAccess=10m").build();
    }
}

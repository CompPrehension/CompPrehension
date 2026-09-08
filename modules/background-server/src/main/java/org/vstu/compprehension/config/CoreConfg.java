package org.vstu.compprehension.config;

import org.vstu.compprehension.frontend.mappers.QuestionBankSearchStatsDtoMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiDeepLinkingContext;
import org.vstu.compprehension.adapter.UserServiceImpl;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.services.questionbank.QuestionBankImpl;
import org.vstu.compprehension.repositories.data.QuestionBankDataRepository;
import org.vstu.compprehension.service.BktService;
import org.vstu.compprehension.strategies.*;

import javax.inject.Singleton;
import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {
    @Bean
    @Singleton
    @ConditionalOnProperty(prefix = "bkt", name = "enabled", havingValue = "true")
    BktStrategy getBktStrategy(@Autowired BktService bktService, @Autowired DomainFactory domainFactory,
                               @Autowired ExerciseAttemptDataService exerciseAttemptService) {
        return new BktStrategy(bktService, domainFactory, exerciseAttemptService);
    }

    @Bean
    @Singleton @Primary
    GradeConfidenceBaseStrategy getGradeConfidenceBaseStrategy(@Autowired DomainFactory domainFactory,
                                                               @Autowired ExerciseAttemptDataService exerciseAttemptService) {
        return new GradeConfidenceBaseStrategy(domainFactory, exerciseAttemptService);
    }
    @Bean
    @Singleton
    GradeConfidenceBaseStrategy_Manual50Autogen50 getGradeConfidenceBaseStrategy_Manual50Autogen50(@Autowired DomainFactory domainFactory, @Autowired RandomProvider randomProvider,
                                                                                                    @Autowired ExerciseAttemptDataService exerciseAttemptService) {
        return new GradeConfidenceBaseStrategy_Manual50Autogen50(domainFactory, randomProvider, exerciseAttemptService);
    }
    @Bean
    @Singleton
    StaticStrategy getStaticStrategy(@Autowired DomainFactory domainFactory,
                                     @Autowired ExerciseAttemptDataService exerciseAttemptService) {
        return new StaticStrategy(domainFactory, exerciseAttemptService);
    }
    @Bean
    @Singleton
    Strategy getStrategy(@Autowired DomainFactory domainFactory, @Autowired RandomProvider randomProvider, @Autowired ExerciseAttemptDataService exerciseAttemptService) {
        return new Strategy(domainFactory, randomProvider, exerciseAttemptService);
    }

    @Bean
    @SessionScope
    UserDataService getUserService() {
        // Фоновому серверу пользователь не нужен: заданий от лица студента он не решает.
        return new UserServiceImpl();
    }

    @Bean
    @Singleton
    LtiContextProvider getLtiContextProvider() {
        // Background jobs have no LTI request context.
        return new LtiContextProvider() {
            @Override
            public Optional<LtiContext> getCurrentLtiContext() {
                return Optional.empty();
            }

            @Override
            public Optional<LtiDeepLinkingContext> getCurrentDeepLinkingContext() {
                return Optional.empty();
            }
        };
    }

    @Bean
    @Singleton
    GradePassbackService getGradePassbackService() {
        return (attempt, grade) -> { };
    }

    @Bean
    @Singleton
    QuestionBank getQuestionBank(@Autowired QuestionBankDataRepository bankDataRepository,
                                 @Autowired QuestionBankSearchStatsDtoMapper questionBankSearchStatsDtoMapper) {
        return new QuestionBankImpl(bankDataRepository, questionBankSearchStatsDtoMapper);
    }

    @Bean
    @Singleton
    Cache<String, JenaFactList> getJenaSolveCache() {
        return CacheBuilder.from("maximumSize=30,expireAfterAccess=10m").build();
    }
}

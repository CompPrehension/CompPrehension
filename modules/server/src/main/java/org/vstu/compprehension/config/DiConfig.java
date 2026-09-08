package org.vstu.compprehension.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.service.BktService;
import org.vstu.compprehension.repositories.data.UserDataRepository;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.adapters.*;
import org.vstu.compprehension.businesslogic.backend.Backend;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.businesslogic.backend.PelletBackend;
import org.vstu.compprehension.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.services.questionbank.QuestionBankImpl;
import org.vstu.compprehension.repositories.data.QuestionBankDataRepository;
import org.vstu.compprehension.strategies.*;
import org.vstu.compprehension.services.RandomProviderImpl;

import javax.inject.Singleton;
import java.util.List;

@Configuration
public class DiConfig {
    @Bean
    @Qualifier("allBackends")
    @RequestScope
    List<Backend<?, ?>> getAllBackends(
            @Autowired @Lazy JenaBackend jenaBackend,
            @Autowired @Lazy PelletBackend pelletBackend,
            @Autowired DecisionTreeReasonerBackend decisionTreeReasonerBackend,
            @Autowired BackendTaskQueue taskQueue,
            @Autowired Cache<String, JenaFactList> jenaCache)
    {
        return List.of(
            new RateLimitBackendDecorator<>(
                JenaBackend.BackendId,
                new SolutionCachingJenaBackendDecorator(jenaBackend, jenaCache),
                taskQueue
            ),
            decisionTreeReasonerBackend, //FIXME wrap with RateLimitBackendDecorator
            new RateLimitBackendDecorator<>(PelletBackend.BackendId, pelletBackend, taskQueue)
        );
    }

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
    UserDataService getUserService(@Autowired UserDataRepository userDataRepository,
                                   @Autowired EducationResourceService educationResourceService,
                                   @Autowired ExternalAccountService externalAccountService,
                                   @Autowired LtiContextProvider ltiContextProvider,
                                   @Autowired CourseDataService courseService,
                                   @Autowired RoleAssignmentService roleAssignmentService,
                                   @Autowired Mapper<UserAccountData, UserData> currentUserMapper) {
        return new CachedUserService(new UserServiceImpl(userDataRepository, educationResourceService, externalAccountService, ltiContextProvider, courseService, roleAssignmentService, currentUserMapper));
    }

    @Bean
    @Singleton
    QuestionBank getQuestionBank(@Autowired QuestionBankDataRepository bankDataRepository) {
        return new QuestionBankImpl(bankDataRepository);
    }
    
    @Bean
    @SessionScope
    RandomProvider getRandomProvider() {
        return new RandomProviderImpl();
    }

    @Bean
    @Singleton
    Cache<String, JenaFactList> getJenaSolveCache() {
        return CacheBuilder.from("maximumSize=30,expireAfterAccess=10m").build();
    }
}

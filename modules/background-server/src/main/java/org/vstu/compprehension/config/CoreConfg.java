package org.vstu.compprehension.config;

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
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapter.UserServiceImpl;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.service.BktService;
import org.vstu.compprehension.strategies.*;
import org.vstu.compprehension.utils.RandomProvider;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import javax.inject.Singleton;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {
    @Bean
    @Singleton
    @ConditionalOnProperty(prefix = "bkt", name = "enabled", havingValue = "true")
    BktStrategy getBktStrategy(@Autowired BktService bktService, @Autowired DomainFactory domainFactory) {
        return new BktStrategy(bktService, domainFactory);
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
        return new UserServiceImpl();
    }

    @Bean
    @Singleton
    QuestionBank getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionMetadataRepository metadataRepository,
            @Autowired QuestionDataRepository questionDataRepository,
            @Autowired QuestionGenerationRequestRepository generationRequestRepository,
            @Autowired QuestionMetadataSearchRequestRepository questionSearchRequestLogRepository,
            @Autowired TransactionScopeFactory transactionScopeFactory) throws Exception {
        //var allDomains = domainRepository.findAll();
        return new QuestionBank(metadataRepository, questionDataRepository, generationRequestRepository, questionSearchRequestLogRepository, transactionScopeFactory);
    }
    
    @Bean
    @Singleton
    RandomProvider getRandomProvider() {
        return new RandomProvider();
    }

    @Bean
    @Singleton
    Cache<String, JenaFactList> getJenaSolveCache() {
        return CacheBuilder.from("maximumSize=30,expireAfterAccess=10m").build();
    }
}

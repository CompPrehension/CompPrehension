package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bank-load-testing-batch")
@Getter
@Setter
@NoArgsConstructor
public class BankLoadTestingJobBatchConfig {
    private String cronSchedule = "never";

    Integer initialDelay;
    
    Integer generatorThresholdFrom = 0;
    Integer generatorThresholdTo = 50;
    Integer generatorThresholdStep = 5;
    
    Integer generatorAdditionalQuestionsToGenerateFrom = 0;
    Integer generatorAdditionalQuestionsToGenerateTo = 9;
    Integer generatorAdditionalQuestionsToGenerateStep = 3;

    boolean skipDelayForQuestionsWithoutGeneration = false;

    long exerciseId;
    int usersCount;

    Integer randomSeed;

    int exerciseStartDelayMin = 0;
    int exerciseStartDelayMax = 30;

    int postQuestionDelayMin = 3;
    int postQuestionDelayMax = 10;

    Double[] questionDelayRandomFactorization;
}

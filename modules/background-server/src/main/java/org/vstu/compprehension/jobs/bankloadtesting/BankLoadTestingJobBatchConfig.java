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

    // Integer retryAttempts = 1;
    Integer initialDelay = 0;
    
    Integer generatorThresholdFrom = 0;
    Integer generatorThresholdTo = 50;
    Integer generatorThresholdStep = 1;
    
    Integer generatorAdditionalQuestionsToGenerateFrom = 0;
    Integer generatorAdditionalQuestionsToGenerateTo = 9;
    Integer generatorAdditionalQuestionsToGenerateStep = 9;

    long exerciseId = 29;
    int usersCount = 7;

    Integer randomSeed = 111111;

    int exerciseStartDelayMin = 0;
    int exerciseStartDelayMax = 30;

    int postQuestionDelayMin = 3;
    int postQuestionDelayMax = 10;
}

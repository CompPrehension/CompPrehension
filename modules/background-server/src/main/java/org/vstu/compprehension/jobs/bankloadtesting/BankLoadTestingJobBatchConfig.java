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

    private Integer initialDelay;

    private Integer generatorThresholdFrom = 0;
    private Integer generatorThresholdTo = 50;
    private Integer generatorThresholdStep = 5;

    private Integer generatorAdditionalQuestionsToGenerateFrom = 0;
    private Integer generatorAdditionalQuestionsToGenerateTo = 9;
    private Integer generatorAdditionalQuestionsToGenerateStep = 3;

    private boolean skipDelayForQuestionsWithoutGeneration = false;

    private long exerciseId;
    private int usersCount;

    private Integer randomSeed;

    private int exerciseStartDelayMin = 0;
    private int exerciseStartDelayMax = 30;

    private int postQuestionDelayMin = 3;
    private int postQuestionDelayMax = 10;

    private Double[] questionDelayRandomFactorization;
}

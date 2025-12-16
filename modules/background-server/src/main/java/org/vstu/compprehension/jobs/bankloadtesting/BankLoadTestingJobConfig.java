package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bank-load-testing")
@Getter @Setter
@NoArgsConstructor
public class BankLoadTestingJobConfig {
    private String cronSchedule = "never";

    private long exerciseId;
    private int usersCount;

    private Integer randomSeed;
    private Integer generatorThreshold;
    private Integer generatorAdditionalQuestionsToGenerate;

    private boolean skipDelayForQuestionsWithoutGeneration = false;

    private int exerciseStartDelayMin = 0;
    private int exerciseStartDelayMax = 30;

    private int postQuestionDelayMin = 3;
    private int postQuestionDelayMax = 10;

    private Double[] questionDelayRandomFactorization;
}

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

    long exerciseId;
    int usersCount;
    
    Integer randomSeed;
    Integer generatorThreshold;
    Integer generatorAdditionalQuestionsToGenerate;

    boolean skipDelayForQuestionsWithoutGeneration = false;

    int exerciseStartDelayMin = 0;
    int exerciseStartDelayMax = 30;

    int postQuestionDelayMin = 3;
    int postQuestionDelayMax = 10;
}

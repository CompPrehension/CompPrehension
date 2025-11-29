package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bank-load-testing")
@Getter @Setter
@NoArgsConstructor
public class BankLoadTestingJobConfig {
    private String cronSchedule;

    long exerciseId;
    int usersCount = 15;

    int exerciseStartDelayMin = 0;
    int exerciseStartDelayMax = 30;

    int questionDurationMin = 20;
    int questionDurationMax = 60;

    int postQuestionDelayMin = 3;
    int postQuestionDelayMax = 10;
}

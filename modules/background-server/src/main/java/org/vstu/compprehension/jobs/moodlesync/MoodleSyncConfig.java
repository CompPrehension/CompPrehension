package org.vstu.compprehension.jobs.moodlesync;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "moodle-role-sync")
@Getter @Setter
@NoArgsConstructor
public class MoodleSyncConfig {
    private String cronSchedule = "once";
    private int coursesPerBatch = 20;
    private boolean abortOnEmptyResponse = true;
}

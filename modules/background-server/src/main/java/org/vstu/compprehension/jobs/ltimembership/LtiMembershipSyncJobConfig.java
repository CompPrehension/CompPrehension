package org.vstu.compprehension.jobs.ltimembership;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lti-membership-sync")
@Getter @Setter
@NoArgsConstructor
public class LtiMembershipSyncJobConfig {
    private String cronSchedule = "never";
}

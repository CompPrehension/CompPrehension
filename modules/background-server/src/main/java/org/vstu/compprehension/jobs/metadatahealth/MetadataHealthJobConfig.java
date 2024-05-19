package org.vstu.compprehension.jobs.metadatahealth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "metadata-health")
@Getter @Setter
@NoArgsConstructor
public class MetadataHealthJobConfig {
    private boolean runOnce;
    private String cronSchedule;
}

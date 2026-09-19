package org.vstu.compprehension.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "compprehension.education-resource")
@Getter
@Setter
public class EducationResourceTrustProperties {
    private List<String> trustedHosts = new ArrayList<>();
}

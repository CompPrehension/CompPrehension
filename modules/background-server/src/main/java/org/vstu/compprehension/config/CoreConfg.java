package org.vstu.compprehension.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {

}

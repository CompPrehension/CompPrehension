package org.vstu.compprehension.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
public class JpaConfig {

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = builder
                .dataSource(dataSource)
                .packages("org.vstu.compprehension")
                .build();

        factory.setEntityManagerFactoryInterface(EntityManagerFactory.class);
        return factory;
    }
}
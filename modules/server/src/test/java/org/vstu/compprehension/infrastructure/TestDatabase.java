package org.vstu.compprehension.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Контейнер с базой на весь запуск тестов, а не на контекст Spring.
 */
public final class TestDatabase {

    private static final MySQLContainer<?> CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("compph_test");

    static {
        CONTAINER.start();
    }

    private TestDatabase() {
    }

    public static void applyTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", CONTAINER::getDriverClassName);
    }
}

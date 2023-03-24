package org.vstu.compprehension.config;

import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;
import org.jobrunr.storage.sql.sqlite.SqLiteStorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Paths;

@Configuration
public class JobRunrConfig {
    /*
    @Bean
    public SQLiteDataSource dataSource() {
        final SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + Paths.get(System.getProperty("java.io.tmpdir"), "compph-background-service.db"));
        return dataSource;
    }
    */

    /*
    @Autowired
    @Qualifier("jobs.datasource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("spring.datasource")
    private DataSource dataSource2;

    @Bean
    public StorageProvider storageProvider() {
        return new InMemoryStorageProvider();
    }
    */
}

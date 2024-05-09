package org.vstu.compprehension.config.logs;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DbLogAppenderConfig
{
    @Autowired
    private Environment env;    

    @Value("${config.property.db-logging.enabled:false}")
    private boolean useDbLogging;

    @Value("${config.property.db-logging.level:INFO}")
    private Level logLevel;

    @Value("${config.property.db-logging.logger:root}")
    private String loggerName;

    @Value("${spring.application.name}")
    private String appName;

    @PostConstruct
    public void onStartUp()
    {
        if (!useDbLogging) {
            return;
        }

        String url = env.getProperty("spring.datasource.url");
        String userName = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        String fixedAppName = "'" + appName + "'";

        // Create a new connectionSource build from the Spring properties
        JdbcConnectionSource connectionSource = new JdbcConnectionSource(url, userName, password);

        // This is the mapping between the columns in the table and what to insert in it.
        ColumnConfig[] columnConfigs = new ColumnConfig[8];
        columnConfigs[0] = ColumnConfig.newBuilder().setName("request_id").setPattern("%X{correlationId}").setUnicode(true).build();
        columnConfigs[1] = ColumnConfig.newBuilder().setName("session_id").setPattern("%X{sessionId}").setUnicode(true).build();
        columnConfigs[2] = ColumnConfig.newBuilder().setName("date").setPattern("%d{yyyy-MM-dd HH:mm:ss}{GMT+0}").build();
        columnConfigs[3] = ColumnConfig.newBuilder().setName("level").setPattern("%level").setUnicode(true).build();
        columnConfigs[4] = ColumnConfig.newBuilder().setName("message").setPattern("%message").setUnicode(true).build();
        columnConfigs[5] = ColumnConfig.newBuilder().setName("payload").setPattern("%ex{full}").setUnicode(true).build();
        columnConfigs[6] = ColumnConfig.newBuilder().setName("user_id").setPattern("%X{userId}").setUnicode(true).build();
        columnConfigs[7] = ColumnConfig.newBuilder().setName("app").setLiteral(fixedAppName).setUnicode(true).build();

        // filter for the appender
        ThresholdFilter filter = ThresholdFilter.createFilter(logLevel, null, null);

        // The creation of the new database appender passing:
        // - the name of the appender
        // - ignore exceptions encountered when appending events are logged
        // - the filter created previously
        // - the connectionSource,
        // - log buffer size,
        // - the name of the table
        // - the config of the columns.
        var dbAppender = JdbcAppender.newBuilder().setBufferSize(1).setColumnConfigs(columnConfigs).setColumnMappings()
                .setConnectionSource(connectionSource).setTableName("logs").setName("DbAppender").setIgnoreExceptions(true).setFilter(filter).build();

        // start the appender, and this is it...
        var logger = loggerName.equalsIgnoreCase("root") ? (Logger)LogManager.getRootLogger() : (Logger)LogManager.getLogger(loggerName);
        logger.addAppender(dbAppender);
        dbAppender.start();
    }
}

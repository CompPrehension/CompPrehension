package org.vstu.compprehension.config.logs;

import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
public class LogConfig
{
    @Autowired
    private Environment env;

    @PostConstruct
    public void onStartUp()
    {
        val context = (LoggerContext) LogManager.getContext(false);
        val configuration = context.getConfiguration();
        val useDbAppender = configuration.getStrSubstitutor().getVariableResolver().lookup("useDbAppender");
        if (!useDbAppender.equalsIgnoreCase("true")) {
            return;
        }

        String url = env.getProperty("spring.datasource.url");
        String userName = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        // Create a new connectionSource build from the Spring properties
        JdbcConnectionSource connectionSource = new JdbcConnectionSource(url, userName, password);

        // This is the mapping between the columns in the table and what to insert in it.
        ColumnConfig[] columnConfigs = new ColumnConfig[7];
        columnConfigs[0] = ColumnConfig.newBuilder().setName("request_id").setPattern("%X{correlationId}").setUnicode(true).build();
        columnConfigs[1] = ColumnConfig.newBuilder().setName("session_id").setPattern("%X{sessionId}").setUnicode(true).build();
        columnConfigs[2] = ColumnConfig.newBuilder().setName("date").setEventTimestamp(true).build();
        columnConfigs[3] = ColumnConfig.newBuilder().setName("level").setPattern("%level").setUnicode(true).build();
        columnConfigs[4] = ColumnConfig.newBuilder().setName("message").setPattern("%message").setUnicode(true).build();
        columnConfigs[5] = ColumnConfig.newBuilder().setName("payload").setPattern("%ex{full}").setUnicode(true).build();
        columnConfigs[6] = ColumnConfig.newBuilder().setName("user_id").setPattern("%X{userId}").setUnicode(true).build();

        // filter for the appender
        ThresholdFilter filter = ThresholdFilter.createFilter(Level.INFO, null, null);

        // The creation of the new database appender passing:
        // - the name of the appender
        // - ignore exceptions encountered when appending events are logged
        // - the filter created previously
        // - the connectionSource,
        // - log buffer size,
        // - the name of the table
        // - the config of the columns.
        val dbAppender = JdbcAppender.newBuilder().setBufferSize(1).setColumnConfigs(columnConfigs).setColumnMappings()
                .setConnectionSource(connectionSource).setTableName("logs").withName("DbAppender").withIgnoreExceptions(true).withFilter(filter).build();

        // start the appender, and this is it...
        dbAppender.start();
        ((Logger) LogManager.getRootLogger()).addAppender(dbAppender);
    }
}

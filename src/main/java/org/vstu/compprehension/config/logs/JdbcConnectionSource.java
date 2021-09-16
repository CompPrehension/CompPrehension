package org.vstu.compprehension.config.logs;


import lombok.val;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcConnectionSource implements ConnectionSource
{
    private DataSource dataSource;

    public JdbcConnectionSource(String dbUrl, String userName, String password)
    {
        val properties = new Properties();
        properties.setProperty("user", userName);
        properties.setProperty("password", password);

        val connectionFactory = new DriverManagerConnectionFactory(dbUrl, properties);
        val poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        val connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        poolableConnectionFactory.setValidationQuery("SELECT 1");
        poolableConnectionFactory.setValidationQueryTimeout(3);
        poolableConnectionFactory.setDefaultReadOnly(false);
        poolableConnectionFactory.setDefaultAutoCommit(false);
        poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        this.dataSource = new PoolingDataSource<>(connectionPool);
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }
}

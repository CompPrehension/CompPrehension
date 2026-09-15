package org.vstu.compprehension.tools.db;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

public class DiffMySQLDialect extends MySQLDialect {
    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);
        typeContributions.getTypeConfiguration().getDdlTypeRegistry()
                .addDescriptor(new DdlTypeImpl(SqlTypes.OTHER, "json", this));
    }
}

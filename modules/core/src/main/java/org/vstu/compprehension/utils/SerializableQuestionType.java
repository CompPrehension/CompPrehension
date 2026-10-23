package org.vstu.compprehension.utils;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class SerializableQuestionType implements UserType<SerializableQuestion> {
    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<SerializableQuestion> returnedClass() {
        return SerializableQuestion.class;
    }

    @Override
    public boolean equals(SerializableQuestion x, SerializableQuestion y) throws HibernateException {
        return Objects.equals(x, y) || (x != null && x.equals(y));
    }

    @Override
    public int hashCode(SerializableQuestion x) throws HibernateException {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, SerializableQuestion value, int index, WrapperOptions session) throws HibernateException, SQLException {
        if (value != null) {
            st.setString(index, SerializableQuestion.serializeToString(value));
        } else {
            st.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public SerializableQuestion deepCopy(SerializableQuestion value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(SerializableQuestion value) throws HibernateException {
        return value != null ? SerializableQuestion.serializeToString(value) : null;
    }

    @Override
    public SerializableQuestion assemble(Serializable cached, Object owner) throws HibernateException {
        return cached != null ? SerializableQuestion.deserializeFromString((String) cached) : null;
    }

    @Override
    public SerializableQuestion replace(SerializableQuestion original, SerializableQuestion target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public SerializableQuestion nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        String json = rs.getString(position);
        return json != null ? SerializableQuestion.deserializeFromString(json) : null;
    }
}

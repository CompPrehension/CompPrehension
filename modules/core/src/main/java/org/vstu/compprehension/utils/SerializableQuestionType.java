package org.vstu.compprehension.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class SerializableQuestionType implements UserType<SerializableQuestion> {

    private static final Gson gson = new GsonBuilder().create();

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
    public void nullSafeSet(PreparedStatement st, SerializableQuestion value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value != null) {
            st.setString(index, gson.toJson(value));
        } else {
            st.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public SerializableQuestion deepCopy(SerializableQuestion value) throws HibernateException {
        if (value == null) {
            return null;
        }
        String json = gson.toJson(value);
        return gson.fromJson(json, SerializableQuestion.class);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(SerializableQuestion value) throws HibernateException {
        return (Serializable) deepCopy(value);
    }

    @Override
    public SerializableQuestion assemble(Serializable cached, Object owner) throws HibernateException {
        return (SerializableQuestion) cached;
    }

    @Override
    public SerializableQuestion replace(SerializableQuestion original, SerializableQuestion target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

    @Override
    public SerializableQuestion nullSafeGet(ResultSet rs, int columnIndex, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String json = rs.getString(columnIndex);
        return json != null ? gson.fromJson(json, SerializableQuestion.class) : null;
    }
}

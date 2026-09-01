package org.vstu.compprehension.models.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.models.businesslogic.auth.Role;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role r) {
        return (r == null || r == SystemRole.UNKNOWN) ? null : r.id();
    }

    @Override
    public Role convertToEntityAttribute(String s) {
        if (s == null) return null;
        try {
            return SystemRole.valueOf(s);
        } catch (IllegalArgumentException e) {
            return SystemRole.UNKNOWN;
        }
    }
}

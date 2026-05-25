package org.vstu.compprehension.models.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.vstu.compprehension.models.entities.EnumData.Role;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role r) {
        return (r == null || r == Role.UNKNOWN) ? null : r.name();
    }

    @Override
    public Role convertToEntityAttribute(String s) {
        if (s == null) return null;
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Role.UNKNOWN;
        }
    }
}

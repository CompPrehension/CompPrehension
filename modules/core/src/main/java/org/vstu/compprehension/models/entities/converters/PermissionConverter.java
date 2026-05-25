package org.vstu.compprehension.models.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.vstu.compprehension.models.entities.EnumData.Permission;

@Converter(autoApply = true)
public class PermissionConverter implements AttributeConverter<Permission, String> {
    @Override
    public String convertToDatabaseColumn(Permission p) {
        return (p == null || p == Permission.UNKNOWN) ? null : p.name();
    }

    @Override
    public Permission convertToEntityAttribute(String s) {
        if (s == null) return null;
        try {
            return Permission.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Permission.UNKNOWN;
        }
    }
}

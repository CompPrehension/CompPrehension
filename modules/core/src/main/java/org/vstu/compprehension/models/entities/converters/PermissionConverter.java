package org.vstu.compprehension.models.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.businesslogic.auth.Permission;

@Converter(autoApply = true)
public class PermissionConverter implements AttributeConverter<Permission, String> {
    @Override
    public String convertToDatabaseColumn(Permission p) {
        return (p == null || p == SystemPermission.UNKNOWN) ? null : p.id();
    }

    @Override
    public Permission convertToEntityAttribute(String s) {
        if (s == null) return null;
        try {
            return SystemPermission.valueOf(s);
        } catch (IllegalArgumentException e) {
            return SystemPermission.UNKNOWN;
        }
    }
}

package org.vstu.compprehension.models.businesslogic.auth;

public record SystemRole(String name, String displayName, SystemPermission[] permissions) {}

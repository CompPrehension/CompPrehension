package org.vstu.compprehension.models.businesslogic.auth;

public record SystemRole(String Name, String DisplayName, SystemPermission[] Permissions) {}

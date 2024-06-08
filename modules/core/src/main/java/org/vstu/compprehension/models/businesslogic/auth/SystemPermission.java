package org.vstu.compprehension.models.businesslogic.auth;

import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.List;

public record SystemPermission(String Name, String DisplayName, List<PermissionScopeKind> AllowedScopes) {}

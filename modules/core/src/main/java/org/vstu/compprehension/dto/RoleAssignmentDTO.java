package org.vstu.compprehension.dto;

import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.Optional;

public record RoleAssignmentDTO(String roleName, PermissionScopeKind permissionScopeKind, Optional<Long> ownerId) { }

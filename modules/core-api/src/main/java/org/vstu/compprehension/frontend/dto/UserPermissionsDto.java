package org.vstu.compprehension.frontend.dto;

public record UserPermissionsDto(
        boolean canViewGlobalPool,
        boolean isLtiMode,
        boolean canRegisterLms
) {
}

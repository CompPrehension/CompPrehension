package org.vstu.compprehension.data.permission;

/** Что пользователю разрешено вне какого-либо курса. */
public record UserPermissionsData(boolean canViewGlobalPool) {
}

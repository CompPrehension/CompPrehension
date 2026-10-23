package org.vstu.compprehension.data.permission;

/** Что пользователю разрешено делать с конкретным упражнением на странице курса. */
public record ExerciseCardPermissionsData(
        boolean canEdit,
        boolean canDelete,
        boolean canCloneToCourse,
        boolean canCopyToGlobalPool,
        boolean canUnlinkFromCourse) {
}

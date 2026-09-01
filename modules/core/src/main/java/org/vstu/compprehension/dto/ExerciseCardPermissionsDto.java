package org.vstu.compprehension.dto;

public record ExerciseCardPermissionsDto(
        boolean canEdit,
        boolean canDelete,
        boolean canCloneToCourse,
        boolean canCopyToGlobalPool,
        boolean canUnlinkFromCourse
) {
}

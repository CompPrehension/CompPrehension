package org.vstu.compprehension.frontend.dto;

public record ExerciseListPermissionsDto(
        boolean canCreateExercise,
        boolean canImportInherit,
        boolean canImportClone
) {
}

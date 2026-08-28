package org.vstu.compprehension.dto;

public record ExerciseListPermissionsDto(
        boolean canCreateExercise,
        boolean canImportInherit,
        boolean canImportClone
) {
}

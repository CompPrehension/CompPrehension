package org.vstu.compprehension.data.permission;

/** Что пользователю разрешено делать со списком упражнений. */
public record ExerciseListPermissionsData(
        boolean canCreateExercise,
        boolean canImportInherit,
        boolean canImportClone) {
}

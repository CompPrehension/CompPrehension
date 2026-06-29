package org.vstu.compprehension.models.entities.EnumData;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

import static org.vstu.compprehension.models.entities.EnumData.Permission.*;

@Getter
public enum Role {
    UNKNOWN(Set.of()),

    GLOBAL_ADMIN(EnumSet.complementOf(EnumSet.of(Permission.UNKNOWN))),

    EDUCATION_RESOURCE_ADMIN(EnumSet.of(
        VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE,
        VIEW_EXERCISE, CREATE_EXERCISE, EDIT_EXERCISE, DELETE_EXERCISE,
        SOLVE_EXERCISE
    )),

    TEACHER(EnumSet.of(
        VIEW_COURSE, EDIT_COURSE,
        VIEW_EXERCISE, CREATE_EXERCISE, EDIT_EXERCISE, DELETE_EXERCISE,
        SOLVE_EXERCISE
    )),

    // Повторяет роль Moodle "Non-editing teacher": только просмотр и решение. Намеренно
    // отделена от STUDENT, чтобы для не-студентов можно было подавлять отправку оценок (grade passback).
    ASSISTANT(EnumSet.of(
        VIEW_COURSE, VIEW_EXERCISE, SOLVE_EXERCISE
    )),

    STUDENT(EnumSet.of(VIEW_COURSE, VIEW_EXERCISE, SOLVE_EXERCISE));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

}

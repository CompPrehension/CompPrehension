package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;

/** Упражнение, попавшее в курс: связь обязана быть подгружена вместе с упражнением. */
@Component
class CourseExerciseMapper implements Mapper<ExerciseCourseLinkEntity, CourseExerciseData> {

    @Override
    public @NotNull CourseExerciseData map(@NotNull ExerciseCourseLinkEntity source) {
        ExerciseEntity exercise = Strict.required(source.getExercise(), "exercise", "exercise course link");
        long exerciseId = Strict.required(exercise.getId(), "id", "exercise of course link");
        return new CourseExerciseData(
                exerciseId,
                Strict.required(exercise.getName(), "name", "exercise " + exerciseId));
    }
}

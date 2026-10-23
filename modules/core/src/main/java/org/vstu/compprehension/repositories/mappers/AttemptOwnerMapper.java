package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptOwner;

@Component
class AttemptOwnerMapper implements Mapper<AttemptOwner, AttemptOwnerData> {

    @Override
    public @NotNull AttemptOwnerData map(@NotNull AttemptOwner source) {
        return new AttemptOwnerData(source.getUserId(), source.getCourseId());
    }
}

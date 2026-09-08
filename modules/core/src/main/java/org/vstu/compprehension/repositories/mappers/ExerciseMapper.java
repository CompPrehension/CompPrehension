package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;

import java.util.List;

/** Упражнение целиком. */
@Component
class ExerciseMapper implements Mapper<ExerciseEntity, ExerciseData> {

    @Override
    public @NotNull ExerciseData map(@NotNull ExerciseEntity source) {
        long id = Strict.required(source.getId(), "id", "exercise");
        String owner = "exercise " + id;
        // getDomain() ленивый, но getName() - это первичный ключ домена,
        // и его прокси отдаёт сам, без запроса.
        var domain = Strict.required(source.getDomain(), "domain", owner);
        return new ExerciseData(
                id,
                Strict.required(source.getName(), "name", owner),
                Strict.required(domain.getName(), "domain.name", owner),
                Strict.required(source.getBackendId(), "backendId", owner),
                Strict.required(source.getStrategyId(), "strategyId", owner),
                Strict.required(source.getOptions(), "options", owner),
                List.copyOf(Strict.required(source.getStages(), "stages", owner)),
                List.copyOf(source.getTags()),
                source.isPublic());
    }
}

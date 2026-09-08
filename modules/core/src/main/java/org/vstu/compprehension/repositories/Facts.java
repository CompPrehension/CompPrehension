package org.vstu.compprehension.repositories;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.BackendFactData;

import java.util.ArrayList;
import java.util.List;

/**
 * Копирование фактов между моделью и сущностью.
 * <p>
 * Список фактов лежит в json-колонке и хранится одним объектом: и вопрос, и нарушение
 * должны получить собственную копию, иначе правка в одном месте уедет в другое.
 */
public final class Facts {

    private Facts() {
    }

    /** Изменяемая копия; отсутствие фактов читается как пустой список. */
    public static @NotNull List<BackendFactData> copy(@Nullable List<BackendFactData> facts) {
        return facts == null ? new ArrayList<>() : new ArrayList<>(facts);
    }
}

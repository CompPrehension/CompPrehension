package org.vstu.compprehension.businesslogic.strategies.settings;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.Language;

import java.util.List;

/** Форма настроек стратегии: поля, которые преподаватель заполняет в упражнении. */
public record StrategySettingsSchema(@NotNull List<Field> fields) {

    /** Поле формы; имя совпадает с ключом в JSON настроек. */
    public sealed interface Field permits Flag, Numeric, Choice, Group {
        @NotNull String name();

        @NotNull Label label();
    }

    public record Flag(@NotNull String name, @NotNull Label label) implements Field {
    }

    public record Numeric(@NotNull String name, @NotNull Label label, int min, int max) implements Field {
    }

    public record Choice(@NotNull String name, @NotNull Label label, @NotNull List<Option> options) implements Field {
    }

    public record Group(@NotNull String name, @NotNull Label label, @NotNull List<Field> fields) implements Field {
    }

    /** Вариант значения поля выбора; значение совпадает с хранимым в JSON. */
    public record Option(@NotNull String value, @NotNull Label label) {
    }

    public record Label(@NotNull String ru, @NotNull String en) {

        /** Польской подписи нет — для него английская. */
        public @NotNull String toText(@NotNull Language language) {
            return language == Language.RUSSIAN ? ru : en;
        }
    }
}

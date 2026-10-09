package org.vstu.compprehension.frontend.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/** Форма настроек стратегии в упражнении и значения, которыми её заполнить для нового упражнения. */
public record StrategySettingsDto(@NotNull List<Field> fields, @NotNull Map<String, Object> defaults) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Flag.class, name = "FLAG"),
            @JsonSubTypes.Type(value = Numeric.class, name = "NUMERIC"),
            @JsonSubTypes.Type(value = Choice.class, name = "CHOICE"),
            @JsonSubTypes.Type(value = Group.class, name = "GROUP"),
    })
    public sealed interface Field permits Flag, Numeric, Choice, Group {
        @NotNull String name();

        @NotNull String label();
    }

    public record Flag(@NotNull String name, @NotNull String label) implements Field {
    }

    public record Numeric(@NotNull String name, @NotNull String label, int min, int max) implements Field {
    }

    public record Choice(@NotNull String name, @NotNull String label, @NotNull List<Option> options) implements Field {
    }

    public record Group(@NotNull String name, @NotNull String label, @NotNull List<Field> fields) implements Field {
    }

    public record Option(@NotNull String value, @NotNull String label) {
    }
}

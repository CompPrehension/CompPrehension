package org.vstu.compprehension.businesslogic.strategies.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Choice;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Flag;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Group;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Label;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Numeric;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema.Option;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Тип настроек стратегии: значения по умолчанию, форма для преподавателя и чтение настроек упражнения. */
public final class StrategySettingsType<S extends Record & StrategySettings> {

    // Неизвестное поле — опечатка или настройка другой стратегии: такие настройки не принимаются.
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    private static final TypeReference<Map<String, Object>> VALUES = new TypeReference<>() {
    };

    private final @NotNull Class<S> type;
    private final @NotNull ObjectNode defaults;
    @Getter
    private final @NotNull StrategySettingsSchema schema;

    public StrategySettingsType(@NotNull Class<S> type, @NotNull S defaults) {
        this.type = type;
        this.defaults = MAPPER.valueToTree(defaults);
        this.schema = new StrategySettingsSchema(describeComponents(type));
    }

    /** Значения по умолчанию в том виде, в каком настройки хранятся в упражнении. */
    public @NotNull Map<String, Object> getDefaults() {
        return MAPPER.convertValue(defaults, VALUES);
    }

    /** Настройки упражнения; поля, которых нет среди сохранённых, берутся по умолчанию. */
    public @NotNull S read(@Nullable Map<String, Object> stored) {
        var merged = defaults.deepCopy();
        if (stored != null) {
            mergeInto(merged, MAPPER.valueToTree(stored));
        }
        checkRanges(schema.fields(), merged);
        try {
            return MAPPER.treeToValue(merged, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid strategy settings: " + e.getOriginalMessage(), e);
        }
    }

    /** Настройки упражнения в том виде, в каком их хранят: проверенные и со всеми полями. */
    public @NotNull Map<String, Object> normalize(@Nullable Map<String, Object> stored) {
        return MAPPER.convertValue(read(stored), VALUES);
    }

    private static void mergeInto(@NotNull ObjectNode target, @NotNull ObjectNode source) {
        source.properties().forEach(entry -> {
            if (target.get(entry.getKey()) instanceof ObjectNode targetGroup && entry.getValue() instanceof ObjectNode sourceGroup) {
                mergeInto(targetGroup, sourceGroup);
            } else {
                target.set(entry.getKey(), entry.getValue().deepCopy());
            }
        });
    }

    private static void checkRanges(@NotNull List<StrategySettingsSchema.Field> fields, @NotNull ObjectNode values) {
        for (var field : fields) {
            var value = values.get(field.name());
            switch (field) {
                case Numeric numeric when value != null && value.isInt()
                        && (value.intValue() < numeric.min() || value.intValue() > numeric.max()) ->
                        throw new IllegalArgumentException("Strategy setting '" + field.name() + "' must be within ["
                                + numeric.min() + ", " + numeric.max() + "], got: " + value);
                case Group group when value instanceof ObjectNode groupValues -> checkRanges(group.fields(), groupValues);
                default -> {
                }
            }
        }
    }

    private static @NotNull List<StrategySettingsSchema.Field> describeComponents(@NotNull Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(StrategySettingsType::describeComponent)
                .toList();
    }

    private static @NotNull StrategySettingsSchema.Field describeComponent(@NotNull RecordComponent component) {
        var label = requireLabel(component.getAnnotation(Setting.class), component);
        var name = component.getName();
        var valueType = component.getType();
        if (valueType == boolean.class) {
            return new Flag(name, label);
        }
        if (valueType == int.class) {
            var setting = component.getAnnotation(Setting.class);
            return new Numeric(name, label, setting.min(), setting.max());
        }
        if (valueType.isEnum()) {
            return new Choice(name, label, describeConstants(valueType));
        }
        if (valueType.isRecord()) {
            return new Group(name, label, describeComponents(valueType));
        }
        throw new IllegalStateException("Strategy setting " + component + " has unsupported type " + valueType);
    }

    private static @NotNull List<Option> describeConstants(@NotNull Class<?> enumType) {
        return Arrays.stream(enumType.getFields())
                .filter(java.lang.reflect.Field::isEnumConstant)
                .map(constant -> new Option(constant.getName(), requireLabel(constant.getAnnotation(Setting.class), constant)))
                .toList();
    }

    private static @NotNull Label requireLabel(@Nullable Setting setting, @NotNull Object annotated) {
        if (setting == null) {
            throw new IllegalStateException("Strategy setting " + annotated + " has no @Setting label");
        }
        return new Label(setting.ru(), setting.en());
    }
}

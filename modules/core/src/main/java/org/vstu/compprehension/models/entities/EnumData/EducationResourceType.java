package org.vstu.compprehension.models.entities.EnumData;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EducationResourceType {
    MOODLE,
    UNKNOWN;

    private static final Map<String, EducationResourceType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(v -> v.name().toLowerCase(), Function.identity()));

    public static EducationResourceType fromString(String value) {
        if (value == null) return UNKNOWN;
        return BY_NAME.getOrDefault(value.trim().toLowerCase(), UNKNOWN);
    }
}

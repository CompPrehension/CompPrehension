package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SkillState {

    MASTERED("mastered"),
    UNMASTERED("unmastered");

    private final String value;

    public static SkillState fromValue(String value) {
        if (value == null) {
            return UNMASTERED;
        }
        return Arrays.stream(values())
                .filter(state -> value.equals(state.value))
                .findFirst()
                .orElse(UNMASTERED);
    }
}

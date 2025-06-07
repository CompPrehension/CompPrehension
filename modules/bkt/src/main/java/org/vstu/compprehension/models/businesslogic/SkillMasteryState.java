package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SkillMasteryState {

    MASTERED("mastered"),
    UNMASTERED("unmastered");

    private final String value;

    public static SkillMasteryState fromValue(String value) {
        if (value == null) {
            return UNMASTERED;
        }
        return Arrays.stream(values())
                .filter(state -> value.equals(state.value))
                .findFirst()
                .orElse(UNMASTERED);
    }
}

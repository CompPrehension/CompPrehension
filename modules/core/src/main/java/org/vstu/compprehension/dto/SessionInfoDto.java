package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data @Jacksonized
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class SessionInfoDto {
    private String sessionId;
    private ExerciseInfoDto exercise;
    private UserInfoDto user;
    private String language;
}


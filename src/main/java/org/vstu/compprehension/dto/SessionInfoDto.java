package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SessionInfoDto {
    private String sessionId;
    private Long attemptId;
    private Long[] questionIds;
    private UserInfoDto user;
    private String language;
}


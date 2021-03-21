package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SessionInfoDto {
    private String sessionId;
    private Long[] attemptIds;
    private UserInfoDto user;
    private String language;
}


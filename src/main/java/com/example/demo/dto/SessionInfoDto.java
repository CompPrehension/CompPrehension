package com.example.demo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SessionInfoDto {
    private String sessionId;
    private String[] attemptIds;
    private UserInfoDto user;
    private Date expired;
}


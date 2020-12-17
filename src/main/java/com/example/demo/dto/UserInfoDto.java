package com.example.demo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserInfoDto {
    private String id;
    private String displayName;
    private String email;
    private List<String> roles = new ArrayList<>(0);
}

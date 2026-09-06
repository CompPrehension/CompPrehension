package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserInfoDto {
    private Long id;
    private String displayName;
    private String email;
    private String language;
    private List<String> roles;
}

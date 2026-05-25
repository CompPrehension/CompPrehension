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
    /** Role names the user holds in the requested context (course scope if courseId given, else global). */
    private List<String> roles;
}

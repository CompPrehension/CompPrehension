package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.EnumData.Role;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserInfoDto {
    private Long id;
    private String displayName;
    private String email;
    private List<Role> roles;
}

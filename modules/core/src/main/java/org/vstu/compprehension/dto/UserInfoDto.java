package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.EnumData.Language;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserInfoDto {
    private Long id;
    private String displayName;
    private String email;
    private String language;
}

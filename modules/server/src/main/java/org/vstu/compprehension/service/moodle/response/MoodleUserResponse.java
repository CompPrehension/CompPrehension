package org.vstu.compprehension.service.moodle.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ответ core_user_get_users_by_field — элемент массива.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleUserResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("firstname")
    private String firstname;
    @JsonProperty("lastname")
    private String lastname;
    @JsonProperty("fullname")
    private String fullname;
    @JsonProperty("email")
    private String email;
}

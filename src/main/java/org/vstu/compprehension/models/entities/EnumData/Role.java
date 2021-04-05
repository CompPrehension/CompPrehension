package org.vstu.compprehension.models.entities.EnumData;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role{
    @JsonProperty("STUDENT")
    STUDENT,
    @JsonProperty("TEACHER")
    TEACHER,
    @JsonProperty("ADMIN")
    ADMIN;
}

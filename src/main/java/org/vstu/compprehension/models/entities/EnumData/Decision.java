package org.vstu.compprehension.models.entities.EnumData;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Decision {
    @JsonProperty("CONTINUE")
    CONTINUE,
    @JsonProperty("FINISH")
    FINISH;
}

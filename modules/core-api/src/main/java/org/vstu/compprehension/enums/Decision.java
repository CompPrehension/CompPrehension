package org.vstu.compprehension.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Decision {
    @JsonProperty("CONTINUE")
    CONTINUE,
    @JsonProperty("FINISH")
    FINISH;
}

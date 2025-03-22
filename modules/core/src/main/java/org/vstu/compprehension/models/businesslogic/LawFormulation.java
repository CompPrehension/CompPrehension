package org.vstu.compprehension.models.businesslogic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LawFormulation {
    private String formulation;
    private String name;
    private String backend;

    private Object parsedCache = null;
}

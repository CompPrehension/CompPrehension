package org.vstu.compprehension.models.businesslogic;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Tag {
    private String name;

    public Tag(String name) {
        this.name = name;
    }
}

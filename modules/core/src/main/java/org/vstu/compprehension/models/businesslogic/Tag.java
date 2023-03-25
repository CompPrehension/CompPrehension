package org.vstu.compprehension.models.businesslogic;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Tag {
    private String name;

    /** ID-like bit of the concept for a bitmask combining several Concepts */
    long bitmask;

    public Tag(String name) {
        this.name = name;
    }
}

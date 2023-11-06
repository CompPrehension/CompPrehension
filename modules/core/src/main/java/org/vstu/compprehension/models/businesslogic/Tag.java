package org.vstu.compprehension.models.businesslogic;

import lombok.Value;

@Value
public class Tag {
    String name;
    /** ID-like bit of the concept for a bitmask combining several Concepts */
    long bitmask;
}

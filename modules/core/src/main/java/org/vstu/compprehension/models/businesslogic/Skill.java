package org.vstu.compprehension.models.businesslogic;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Skill implements TreeNodeWithBitmask {
    public String name;

    @Setter
    public long bitmask;

    @Setter
    int sortOrder = 999;

    @Override
    public long getSubTreeBitmask() {
        return bitmask;
    }
}

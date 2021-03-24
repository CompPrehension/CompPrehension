package org.vstu.compprehension.models.businesslogic;

import java.util.List;

public class PositiveLaw extends Law {
    public PositiveLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags) {
        super(name, lawFormulations, concepts, tags);
    }

    @Override
    public boolean isPositiveLaw() {
        return true;
    }
}

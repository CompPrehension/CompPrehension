package org.vstu.compprehension.models.businesslogic;

import java.util.List;

public class PositiveLaw extends Law {
    public PositiveLaw(String name, String displayName, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags) {
        super(name, displayName, lawFormulations, concepts, tags, DEFAULT_SALIENCE);
    }

    @Override
    public boolean isPositiveLaw() {
        return true;
    }
}

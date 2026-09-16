package org.vstu.compprehension.businesslogic;

import java.util.List;

public final class PositiveLaw extends Law {
    public PositiveLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags) {
        super(name, lawFormulations, concepts, tags, DEFAULT_SALIENCE);
    }

    public PositiveLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags, int salience) {
        super(name, lawFormulations, concepts, tags, salience);
    }
}

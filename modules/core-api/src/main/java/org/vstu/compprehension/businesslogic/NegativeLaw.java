package org.vstu.compprehension.businesslogic;

import java.util.List;

public final class NegativeLaw extends Law {
    public NegativeLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags) {
        super(name, lawFormulations, concepts, tags, DEFAULT_SALIENCE);
    }

    public NegativeLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags, int salience) {
        super(name, lawFormulations, concepts, tags, salience);
    }
}

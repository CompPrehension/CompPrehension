package org.vstu.compprehension.models.businesslogic;

import lombok.Getter;

import java.util.List;

public class NegativeLaw extends Law {
    @Getter
    String positiveLaw;

    public NegativeLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags, String positiveLaw) {
        super(name, lawFormulations, concepts, tags, DEFAULT_SALIENCE);
        this.positiveLaw = positiveLaw;
    }

    public NegativeLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags, String positiveLaw, int salience) {
        super(name, lawFormulations, concepts, tags, salience);
        this.positiveLaw = positiveLaw;
    }

    @Override
    public boolean isPositiveLaw() {
        return false;
    }
}

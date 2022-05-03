package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public abstract class Law {

    static int DEFAULT_SALIENCE = 0;

    @Getter
    String name;
    @Getter
    List<LawFormulation> formulations;
    @Getter
    List<Concept> concepts;
    @Getter
    List<Tag> tags;

    /**
     * Priority of the law. Higher value means higher priority,
     * By default salience is set to 0.
     */
    @Getter
    int salience = DEFAULT_SALIENCE;

    public abstract boolean isPositiveLaw();
}

package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public abstract class Law implements TreeNodeWithBitmask {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static final int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag enables a concept to be selected as TARGET at exercise configuration page. */
    public static final int FLAG_TARGET_ENABLED = 2;

    /** All flags are OFF by default */
    public static final int DEFAULT_FLAGS = 0;


    static final int DEFAULT_SALIENCE = 0;

    @Getter
    String name;
    @Getter
    int bitflags;
    @Getter @Setter
    long bitmask = 0;
    @Getter
    List<LawFormulation> formulations;
    @Getter
    List<Concept> concepts;
    @Getter
    List<Tag> tags;

    /**
     * Names of laws that should be enabled automatically when this law is added/enabled.
     */
    @Getter
    List<String> impliesLaws;

    /** Cached references to Law instances */
    @Getter @Setter
    Collection<Law> lawsImplied;

    /**
     * Priority of the law. Higher value means higher priority,
     * By default salience is set to 0.
     */
    @Getter
    int salience;

    public Law(String name, List<LawFormulation> formulations, List<Concept> concepts, List<Tag> tags, int salience) {
        this.name = name;
        this.formulations = formulations;
        this.concepts = concepts;
        this.tags = tags;
        this.salience = salience;
        // default values
        this.bitflags = DEFAULT_FLAGS;
        this.impliesLaws = null;
    }

    public abstract boolean isPositiveLaw();

    public boolean hasFlag(int flagCode) {
    	return (bitflags & flagCode) != 0;
    }

    Long subTreeBitmaskCache = null;

    /**
     * @return bits of this law and all lawsImplied
     */
    public long getSubTreeBitmask() {
        if (lawsImplied == null)
            return bitmask;

        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        long result =
                bitmask | (lawsImplied.stream().map(Law::getSubTreeBitmask).reduce((a, b) -> a | b).orElse(0L));
        return subTreeBitmaskCache = result;
    }

}

package org.vstu.compprehension.businesslogic;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;


public sealed abstract class Law implements TreeNodeWithBitmask permits PositiveLaw, NegativeLaw {

    static final int DEFAULT_SALIENCE = 0;

    @Getter
    String name;
    @Getter(AccessLevel.NONE)
    Set<DomainItemFlag> flags;
    @Getter
    long bitmask = 0;
    @Getter
    @ToString.Exclude
    List<LawFormulation> formulations;
    @Getter
    @ToString.Exclude
    List<Concept> concepts;
    @Getter
    List<Tag> tags;

    @Getter
    int sortOrder = 999;

    /**
     * Names of "base" laws that should be enabled automatically when this law is added/enabled.
     */
    @Getter
    List<String> impliesLaws;

    @Getter
    @ToString.Exclude
    Set<Law> lawsImplied = Set.of();

    @ToString.Exclude
    @Getter
    Set<Law> childLaws = Set.of();

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
        this.flags = Set.of();
        this.impliesLaws = null;
    }

    void setBitmask(long bitmask) {
        this.bitmask = bitmask;
        this.subTreeBitmaskCache = null;
    }

    void setLawsImplied(Set<Law> lawsImplied) {
        this.lawsImplied = Set.copyOf(lawsImplied);
        this.subTreeBitmaskCache = null;
    }

    void setChildLaws(Set<Law> childLaws) {
        this.childLaws = Set.copyOf(childLaws);
    }

    public Set<DomainItemFlag> getFlags() {
        return flags == null ? Set.of() : flags;
    }

    public boolean hasFlag(DomainItemFlag flag) {
        return getFlags().contains(flag);
    }

    public boolean hasFlags(DomainItemFlag... requiredFlags) {
        var flags = getFlags();
        for (var flag : requiredFlags) {
            if (!flags.contains(flag)) {
                return false;
            }
        }
        return true;
    }

    Long subTreeBitmaskCache = null;

    /**
     * @return bits of this law and all lawsImplied
     */
    public long getSubTreeBitmask() {
        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        return subTreeBitmaskCache =
                bitmask | (lawsImplied.stream().map(Law::getSubTreeBitmask).reduce((a, b) -> a | b).orElse(0L));
    }

    public static long combineToBitmask(@Nullable Iterable<Law> laws) {
        if (laws == null)
            return 0;

        long lawBitmask = 0;
        // Note: violations are not positive laws.
        for (Law t : laws) {
            long newBit = t.getBitmask();
            if (newBit == 0) {
                // make use of children
                newBit = t.getSubTreeBitmask();
            }
            lawBitmask |= newBit;
        }
        return lawBitmask;
    }
}

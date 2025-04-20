package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.Setter;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.nodes.Node;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Tree mutation template
 */
public abstract class Mutation {
    @Setter
    protected boolean makeClone = true;
    protected Random random;

    public Mutation() {}

    public Mutation(boolean makeClone) {
        this.makeClone = makeClone;
    }

    public abstract boolean isEligible(Node.Info child);
    protected abstract void perform(MeaningTree origin, Node.Info info);

    public MeaningTree apply(MeaningTree origin, Node.Info info) {
        MeaningTree target;
        if (makeClone) {
            target = origin.clone();
        } else {
            target = origin;
        }
        random = new Random(origin.hashCode());
        perform(target, target.getNodeById(info.id()));
        return target;
    }

    public Mutation combine(Mutation other) {
        final Mutation source = this;
        return new Mutation(source.makeClone || other.makeClone) {
            @Override
            public boolean isEligible(Node.Info child) {
                return other.isEligible(child) || source.isEligible(child);
            }

            @Override
            protected void perform(MeaningTree origin, Node.Info info) {
                source.random = this.random;
                other.random = this.random;
                if (source.isEligible(info)) source.perform(origin, info);
                if (other.isEligible(info)) other.perform(origin, info);
            }
        };
    }

    Mutation combine(Mutation other, BiFunction<Mutation, Mutation, Mutation> selector) {
        final Mutation source = this;
        return new Mutation(source.makeClone || other.makeClone) {
            @Override
            public boolean isEligible(Node.Info child) {
                return other.isEligible(child) || source.isEligible(child);
            }

            @Override
            protected void perform(MeaningTree origin, Node.Info info) {
                source.random = this.random;
                other.random = this.random;
                Mutation selected = selector.apply(source, other);
                if (selected.isEligible(info)) {
                    selected.perform(origin, info);
                } else if (selected == source && other.isEligible(info)) {
                    other.perform(origin, info);
                } else if (selected == other && source.isEligible(info)) {
                    source.perform(origin, info);
                }
            }
        };
    }
}

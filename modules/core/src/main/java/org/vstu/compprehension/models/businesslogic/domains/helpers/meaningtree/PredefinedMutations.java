package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.nodes.Expression;
import org.vstu.meaningtree.nodes.Node;
import org.vstu.meaningtree.nodes.expressions.ParenthesizedExpression;
import org.vstu.meaningtree.nodes.expressions.UnaryExpression;
import org.vstu.meaningtree.nodes.expressions.bitwise.InversionOp;
import org.vstu.meaningtree.nodes.expressions.identifiers.SimpleIdentifier;
import org.vstu.meaningtree.nodes.expressions.other.AssignmentExpression;
import org.vstu.meaningtree.nodes.expressions.other.IndexExpression;
import org.vstu.meaningtree.nodes.expressions.other.MemberAccess;
import org.vstu.meaningtree.nodes.expressions.other.TernaryOperator;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerPackOp;
import org.vstu.meaningtree.nodes.expressions.pointers.PointerUnpackOp;
import org.vstu.meaningtree.nodes.expressions.unary.PostfixDecrementOp;
import org.vstu.meaningtree.nodes.expressions.unary.PostfixIncrementOp;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

class PredefinedMutations {

    static final Mutation parenRemove = new Mutation() {
        @Override
        public boolean isEligible(Node.Info child) {
            return child.node() instanceof ParenthesizedExpression;
        }

        @Override
        protected void perform(MeaningTree origin, Node.Info info) {
            origin.substitute(info.id(), ((ParenthesizedExpression)info.node()).getExpression());
        }
    };

    static final Mutation ternaryComplexify = new Mutation() {
        @Override
        public boolean isEligible(Node.Info child) {
            return child.node() instanceof TernaryOperator t &&
                    !(t.getThenExpr() instanceof TernaryOperator) && !(t.getElseExpr() instanceof TernaryOperator);
        }

        @Override
        protected void perform(MeaningTree origin, Node.Info info) {
            TernaryOperator op = (TernaryOperator) info.node();
            Expression cond = op.getCondition().tryInvert();
            SimpleIdentifier id = new SimpleIdentifier("m" + (char) ('a' + random.nextInt(26)) + random.nextInt(10));
            origin.substitute(info.id(), new TernaryOperator(cond, id, op));
        }
    };

    private static final Mutation addPostfixToPointerMutation = new Mutation() {
        @Override
        public boolean isEligible(Node.Info child) {
            return child.parent() instanceof PointerUnpackOp &&
                    !(child.node() instanceof PostfixDecrementOp || child.node() instanceof PostfixIncrementOp);
        }

        @Override
        protected void perform(MeaningTree origin, Node.Info info) {
            boolean isSub = random.nextBoolean();
            origin.substitute(info.id(), isSub ? new PostfixDecrementOp((Expression) info.node()) :
                    new PostfixIncrementOp((Expression) info.node()));
        }
    };

    private static final Mutation addPointerToPostfixMutation = new Mutation() {
        @Override
        public boolean isEligible(Node.Info child) {
            return (child.node() instanceof PostfixDecrementOp || child.node() instanceof PostfixIncrementOp)
                    && !(child.parent() instanceof PointerUnpackOp) && !(child.parent() instanceof PointerPackOp);
        }

        @Override
        protected void perform(MeaningTree origin, Node.Info info) {
            UnaryExpression unaryExpr = (UnaryExpression) info.node();
            origin.substitute(info.id(), new PointerUnpackOp(unaryExpr));
        }
    };

    private static final Mutation unaryAppend = new Mutation() {
        @Override
        public boolean isEligible(Node.Info child) {
            for (Class<? extends Expression> clazz : List.of(MemberAccess.class, IndexExpression.class)) {
                if (clazz.isInstance(child.node()) &&
                        !(child.parent() instanceof AssignmentExpression &&
                                child.readableFieldName().equals("left"))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void perform(MeaningTree origin, Node.Info info) {
            Expression expr = (Expression) info.node();
            Expression changed;
            if (info.node() instanceof IndexExpression) {
                changed = new PointerPackOp(expr);
            } else {
                int option = random.nextInt(3);
                changed = switch (option) {
                    case 1 -> new InversionOp(expr);
                    case 2 -> new PointerPackOp(expr);
                    default -> new PointerUnpackOp(expr);
                };
            }
            origin.substitute(info.id(), changed);
        }
    };

    private static final BiFunction<Mutation, Mutation, Mutation> combiner =
            (mut1, mut2) -> new Random().nextBoolean() ? mut2 : mut1;


    static final Mutation unaryMutation = unaryAppend
            .combine(addPointerToPostfixMutation, combiner)
            .combine(addPostfixToPointerMutation, combiner);

}

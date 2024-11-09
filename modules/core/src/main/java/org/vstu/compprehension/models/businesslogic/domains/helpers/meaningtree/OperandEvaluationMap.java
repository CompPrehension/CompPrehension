package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.apache.commons.lang3.tuple.Pair;
import org.vstu.meaningtree.utils.tokens.*;

import java.util.*;
import java.util.stream.Collectors;

public class OperandEvaluationMap {
    private record DisposableGroup(OperatorToken operator, TokenGroup rest, boolean partialEval) {};
    private record DisposableIndex(int index, int alt) {};

    private MeaningTreeOrderQuestionBuilder builder;
    private final List<DisposableGroup> possibleDisposableOperands = new ArrayList<>(); // группы токенов, которые частично или полностью могут не вычисляться

    private final Map<DisposableIndex, Set<String>> groupViolations = new HashMap<>(); // [индекс отключаемой группы, номер альтернативы] -> ошибки

    private Set<String> initialDisposedViolations;
    private TokenList initialDisposedTokens;

    OperandEvaluationMap(MeaningTreeOrderQuestionBuilder builder) {
        this.builder = builder;
        findDisposableGroups();
        calculateInitialViolations();
        calculateViolations();
        filterViolations();
    }

    public Map<Integer, Boolean> generate() {
        Map<Integer, Boolean> values = new HashMap<>();
        for (DisposableIndex index : groupViolations.keySet()) {
            DisposableGroup grp = possibleDisposableOperands.get(index.index);
            if (grp.partialEval) {
                values.put(builder.tokens.indexOf(grp.operator), index.alt == 0);
            } else {
                values.put(builder.tokens.indexOf(grp.operator), true);
            }
        }
        return values;
    }

    private void filterViolations() {
        Set<DisposableIndex> filteredAndLimited = groupViolations.entrySet().stream()
                .flatMap(entry1 -> groupViolations.entrySet().stream()
                        .filter(entry2 -> !entry1.getKey().equals(entry2.getKey()))
                        .map(entry2 -> {
                            Set<String> difference = new HashSet<>(entry1.getValue());
                            difference.removeAll(entry2.getValue());
                            return new AbstractMap.SimpleEntry<>(entry1.getKey(), difference);
                        })
                        .filter(e -> e.getValue().size() > 2)
                )
                .distinct()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .skip((int) (0.4 * groupViolations.size()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<DisposableIndex> keys = new HashSet<>(groupViolations.keySet());
        keys.removeAll(filteredAndLimited);
        for (DisposableIndex key : keys) {
            groupViolations.remove(key);
        }
    }

    private TokenList clearTokens(TokenList tokens) {
        return new TokenList(tokens.stream().filter(Objects::nonNull).toList());
    }

    private void calculateInitialViolations() {
        TokenList tokens = builder.tokens;
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableGroup grp = possibleDisposableOperands.get(i);
            if (grp.partialEval) {
                TokenList op = grp.rest.findOperands(grp.rest.source.indexOf(grp.operator)).get(OperandPosition.RIGHT).copyToList();
                tokens = tokens.replace(grp.rest, null);
                tokens.setAll(grp.rest.start, op);
            } else {
                tokens = tokens.replace(grp.rest, null);
                tokens.set(builder.tokens.indexOf(grp.operator), null);
            }
        }
        initialDisposedViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(clearTokens(tokens));
        initialDisposedTokens = tokens;
    }

    private void calculateViolations() {
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableGroup grp = possibleDisposableOperands.get(i);
            calcDisposableGroupDiff(i, grp, initialDisposedViolations, initialDisposedTokens);
        }
    }

    private void calcDisposableGroupDiff(int i, DisposableGroup grp,
                                         Set<String> initial, TokenList tokens) {
        if (grp.partialEval) {
            var operands = grp.rest.findOperands(grp.rest.source.indexOf(grp.operator));
            for (int k = 0; k < 2; k++) {
                TokenList variant = tokens.clone();
                variant.setAll(grp.rest.start, operands.get(k == 0 ? OperandPosition.CENTER : OperandPosition.RIGHT).copyToList());
                Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(variant);
                currentViolations.removeAll(initial);
                groupViolations.put(new DisposableIndex(i, k), currentViolations);
            }
        } else {
            TokenList variant = tokens.clone();
            variant.set(grp.rest.start - 1, grp.operator);
            variant.setAll(grp.rest.start, grp.rest.copyToList());
            Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(clearTokens(variant));
            currentViolations.removeAll(initial);
            groupViolations.put(new DisposableIndex(i, 0), currentViolations);
        }
    }

    private void findDisposableGroups() {
        HashSet<OperatorToken> visited = new HashSet<>();
        for (int i = 0; i < builder.tokens.size(); i++) {
            Token t = builder.tokens.get(i);
            if (t instanceof OperatorToken op && op.additionalOpType != OperatorType.OTHER && !visited.contains(op)) {
                Map<OperandPosition, TokenGroup> operands = builder.tokens.findOperands(i);
                if (op.arity == OperatorArity.TERNARY) {
                    possibleDisposableOperands.add(new DisposableGroup(op, new TokenGroup(
                            operands.get(OperandPosition.LEFT).start,
                            operands.get(OperandPosition.RIGHT).stop,
                            builder.tokens
                    ), true));
                } else if (op.arity == OperatorArity.BINARY) {
                    Pair<Integer, Token> foundRightmost = builder.tokens.findOperands(i).get(OperandPosition.LEFT)
                            .getRightmostToken(op.value);
                    if (foundRightmost != null && ((OperandToken)foundRightmost.getValue()).baseEquals(op)) {
                        // Chaining binary operator
                        List<OperatorToken> chain = new ArrayList<>();
                        chain.add(op);
                        visited.add(op);
                        while (chain.getLast().operandOf() != null && chain.getLast().operandOf().baseEquals(op)) {
                            chain.add(chain.getLast().operandOf());
                        }
                        possibleDisposableOperands.add(new DisposableGroup(op, new ChainedTokenGroup(builder.tokens,
                                chain.stream().map((OperatorToken opTok) ->
                                        builder.tokens.findOperands(
                                                builder.tokens.indexOf(opTok)).get(OperandPosition.RIGHT)).toList().toArray(new TokenGroup[0])),
                                false
                        ));
                    } else {
                        possibleDisposableOperands.add(new DisposableGroup(op, operands.get(OperandPosition.RIGHT), false));
                    }
                }
                visited.add(op);
            }
        }
    }
}

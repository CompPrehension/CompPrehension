package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.languages.LanguageTranslator;
import org.vstu.meaningtree.nodes.Expression;
import org.vstu.meaningtree.nodes.Node;
import org.vstu.meaningtree.nodes.expressions.BinaryExpression;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitAndOp;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitOrOp;
import org.vstu.meaningtree.nodes.expressions.other.TernaryOperator;
import org.vstu.meaningtree.utils.NodeLabel;
import org.vstu.meaningtree.utils.tokens.TokenList;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс предназначен для генерации уникальных значений операндов в вопросе, которые влияют на вычисление выражения.
 * Работает по принципу поиска в выражении узлов AST, которые могут не вычисляться в вопросе из-за определенных значений операндов
 * В зависимости от найденных групп генерируются значения операндов, создающие уникальные по возможным типам ошибок и концептов вопросы
 */
@Log4j2
public class OperandRuntimeValueGenerator {

    /**
     * Информация о возможного отключаемого участка в дереве, представляющее узел.
     * Представляет собой информацию о тернарном операторе или операторе И/ИЛИ
     * partialEval - если true, то в операторе вычислится только один операнд всегда (независимо от значения) (тернарный)
     * varRequiredForEval - значение, которое должен иметь левый операнд, чтобы быть вычислен (только для логических операторов)
     */
    private record DisposableNodeInfo(Node.Info node, boolean partialEval,
                                      boolean valRequiredForEval,
                                      boolean blockRemoval) {
        public long getNodeId() {
            return node.node().getId();
        }

        public Node getNode() {
            return node.node();
        }
    };

    /**
     * Специальный класс, который указывает на индекс из списка List<DisposableNodeInfo>.
     * Суть в том, что у тернарного оператора вычисляет обязательно какой-то из операндов.
     * Поэтому поле alt (от 0) указывает какой именно, если это тернарный оператор. Для других операторов alt = 0
     */
    private record DisposableIndex(DisposableNodeInfo id, int alt) {
        public long getNodeId() {
            return id.getNodeId();
        }

        public Node getNode() {
            return id.getNode();
        }
    };

    private MeaningTreeOrderQuestionBuilder builder;

    /**
     * Участки, которые можно отключить (не вычислять при определенном значении операнда) частично или полностью
     */
    private final List<DisposableNodeInfo> possibleDisposableOperands = new ArrayList<>();

    /**
     *  [отключаемая группа, номер альтернативы] -> уникальные типы ошибок студента, если эта группа вычислится
     */
    private final SequencedMap<DisposableIndex, Set<String>> groupFeatures = new LinkedHashMap<>();

    /**
     * Зависимости между узлами. Пример: a && b && c. Здесь c не имеет значения, если b = false, следовательно, его вычислимость зависит от b
     * А вариативность вопроса, где этот операнд с разными значениями в любом случае не вычислится, не нужна
     */
    private final HashMap<Long, Long> deps = new HashMap<>();

    /**
     * Изначальные типы ошибок, когда все потенциально не вычисляемые зоны отключены,
     * а те, что не отключаются полностью переведены в стандартное положение (у тернарного - false)
     */
    private Set<String> initialDisposedFeatures;

    /**
     * Дерево, где все потенциально не вычисляемые участки переведены в значение,
     * при котором эти участки не вычислятся
     */
    private MeaningTree initialTree;

    private SupportedLanguage language;

    private LanguageTranslator languageTranslator;

    OperandRuntimeValueGenerator(MeaningTreeOrderQuestionBuilder builder, SupportedLanguage language) {
        this.builder = builder;
        initialTree = builder.sourceExpressionTree.clone();
        this.language = language;

        log.info("Processing question for {} values generation: {}", language.toString(), builder.rawTranslatedCode);

        try {
            languageTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }

        findDisposableGroups();
        calculateInitialFeatures();
        calculateFeatures();
        filterFeatures();
    }

    /**
     * Сгенерировать уникальный набор значений, чтобы сделать разнообразные вопросы
     * @return список из пар: общее дерево, его хэш
     */
    public List<Pair<MeaningTree, Integer>> generate() {
        // Предпочтительные значения для операндов, которые увеличат число возможных типов ошибок
        MeaningTree preferred = initialTree.clone();
        int initialHash = preferred.hashCode();
        List<Pair<Integer, Boolean>> preferredValues = new ArrayList<>();

        // Заполнение начальных данных
        for (Node.Info nodeInfo : preferred) {
            Optional<DisposableIndex> foundDisposable = findDisposableIndex(nodeInfo.node().getId());
            if (foundDisposable.isPresent()) {
                DisposableIndex grp = foundDisposable.get();
                if (grp.id().partialEval && nodeInfo.node() instanceof TernaryOperator ternary) {
                    ternary.getCondition().setAssignedValueTag(grp.alt == 0);
                    ternary.getCondition().removeLabel(NodeLabel.DUMMY);
                    preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), grp.alt == 0));
                } else {
                    BinaryExpression expr = (BinaryExpression) nodeInfo.node();
                    expr.getLeft().removeLabel(NodeLabel.DUMMY);
                    expr.getLeft().setAssignedValueTag(!grp.id().valRequiredForEval);
                    preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), grp.id().valRequiredForEval));
                }
            }
        }

        // Больше 256 комбинаций не экономно по памяти и производительности
        if (groupFeatures.size() > 8) {
            log.info("Too many values can be generated for question. Generated questions will be shorten");
            return List.of(new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash)));
        }

        // Здесь будут отфильтрованные комбинации, без бесполезных комбинаций
        Set<List<Boolean>> combinations = new HashSet<>();

        for (boolean[] array : generateCombinations(groupFeatures.size())) {
            for (int i = 0; i < array.length; i++) {
                // Приводим одинаковые по смыслу комбинации к одному виду, чтобы они не дублировались в результирующем множестве
                DisposableIndex tokenIndex = groupFeatures.sequencedKeySet().stream().toList().get(i);

                for (int j = i + 1; j < array.length; j++) {
                    DisposableIndex nextTokenIndex = groupFeatures.sequencedKeySet().stream().toList().get(j);
                    // Два оператора зависимы друг от друга (один операнд другого)
                    // и поэтому в зависимом операторе значение не должно противоречить независимому
                    if (array[i] != array[j]
                            && isTransitiveDependency(nextTokenIndex.getNodeId(),
                                ((BinaryExpression)tokenIndex.getNode()).getLeft().getId())
                    ) {
                        array[i] = array[j];
                    } else if (array[j] != array[i] && isTransitiveDependency(tokenIndex.getNodeId(),
                            ((BinaryExpression)nextTokenIndex.getNode()).getLeft().getId())) {
                        array[j] = array[i];
                    }
                }
            }
            List<Boolean> combination = new ArrayList<>();
            for (boolean val : array) {
                combination.add(val);
            }
            combinations.add(combination);
        }

        List<List<Boolean>> values = new ArrayList<>(combinations);
        Collections.shuffle(values, new Random(initialHash));
        values = values.stream().limit(10).toList();

        List<Pair<MeaningTree, Integer>> result = new ArrayList<>();
        for (List<Boolean> combination : values) {
            result.add(makeTreeFromValues(initialTree, combination));
        }
        if (result.stream().map((e) -> e.getLeft().hashCode()).noneMatch((e) -> e == preferred.hashCode())) {
            result.add(new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash)));
        }

        return result;
    }

    // Собрать дерево, в котором потенциально не вычисляемые участки будут иметь заданную комбинацию значений
    private Pair<MeaningTree, Integer> makeTreeFromValues(MeaningTree initialTree, List<Boolean> combination) {
        MeaningTree preferred = initialTree.clone();
        int initialHash = preferred.hashCode();
        List<Pair<Integer, Boolean>> preferredValues = new ArrayList<>();

        // Заполнение начальных данных
        for (Node.Info nodeInfo : preferred) {
            Optional<DisposableIndex> foundDisposable = findDisposableIndex(nodeInfo.node().getId());
            if (foundDisposable.isPresent()) {
                DisposableIndex grp = foundDisposable.get();
                int index = groupFeatures.sequencedKeySet().stream().toList().indexOf(grp);
                if (nodeInfo.node() instanceof TernaryOperator ternary) {
                    ternary.getCondition().setAssignedValueTag(combination.get(index));
                } else if (nodeInfo.node() instanceof BinaryExpression expr) {
                    expr.getLeft().setAssignedValueTag(combination.get(index));
                }
                preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), combination.get(index)));
            }
        }
        return new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash));
    }

    // Отобрать типы ошибок, которые будут полезны в вопросе
    private void filterFeatures() {
        Set<DisposableIndex> deleteCandidates = new HashSet<>();

        long nonPartialCount = groupFeatures.entrySet().stream()
                .filter((e) -> !e.getKey().id.partialEval
                        && !e.getKey().id.blockRemoval).count();

        HashMap<DisposableNodeInfo, List<DisposableIndex>> partialEvalMap = new HashMap<>();
        for (DisposableIndex index : groupFeatures.keySet()) {
            if (index.id.partialEval) {
                if (!partialEvalMap.containsKey(index.id)) {
                    partialEvalMap.put(index.id, new ArrayList<>(2));
                    partialEvalMap.get(index.id).add(null);
                    partialEvalMap.get(index.id).add(null);
                }
                partialEvalMap.get(index.id).set(index.alt, index);
            }
        }

        // Для тернарного (всегда частично вычисляемого)
        for (Map.Entry<DisposableNodeInfo, List<DisposableIndex>> partialEvalElem : partialEvalMap.entrySet()) {
            Set<String> features1 = new HashSet<>(groupFeatures.get(partialEvalElem.getValue().getFirst()));
            Set<String> features2 = new HashSet<>(groupFeatures.get(partialEvalElem.getValue().get(1)));

            Set<String> intersection = new HashSet<>(features1);
            intersection.retainAll(features2);

            features1.removeAll(intersection);
            features2.removeAll(intersection);

            if (features1.size() < features2.size()) {
                deleteCandidates.add(partialEvalElem.getValue().getFirst());
            } else {
                deleteCandidates.add(partialEvalElem.getValue().get(1));
            }
        }

        // Если больше 10 отключаемых групп, отсечем 25%
        Set<DisposableIndex> redundant = groupFeatures.keySet().stream()
                .filter((e) -> !e.id.partialEval)
                .skip((long) (0.75 * nonPartialCount)).collect(Collectors.toSet());

        if (nonPartialCount > 10) deleteCandidates.addAll(redundant);

        for (var entry : groupFeatures.entrySet()) {
            // для остальных операндов (не тернарный) отбираем только то, что полезно (больше 2 фич), но только если отключаемых групп больше 5
            if (!entry.getKey().id.partialEval && entry.getValue().size() < 3 && nonPartialCount > 4) {
                deleteCandidates.add(entry.getKey());
            }
        }

        for (DisposableIndex key : deleteCandidates) {
            if (!key.id.blockRemoval) {
                groupFeatures.remove(key);
            }
        }
    }

    private Optional<DisposableNodeInfo> findDisposable(long compareId) {
        return possibleDisposableOperands
                .stream()
                .filter((DisposableNodeInfo id) -> id.node.node().getId() == compareId)
                .findFirst();
    }

    private Optional<DisposableIndex> findDisposableIndex(long compareId) {
        return groupFeatures.keySet()
                .stream()
                .filter((DisposableIndex disposable) -> disposable.id().node.node().getId() == compareId)
                .findFirst();
    }

    // Зависит ли один узел от другого транзитивно
    private boolean isTransitiveDependency(long child, long parent) {
        long base = child;
        do {
            base = deps.getOrDefault(base, -1L);
        } while (base != -1L && base != parent);

        return base != -1L;
    }

    // Подсчитать, какие токены и типы ошибок будут у выражения, если отключить все не вычисляемые группы
    private void calculateInitialFeatures() {
        MeaningTree calculationTree = initialTree.clone();

        for (Node.Info node : calculationTree) {
            Optional<DisposableNodeInfo> foundDisposable = findDisposable(node.node().getId());
            if (foundDisposable.isPresent()) {
                DisposableNodeInfo disposable = foundDisposable.get();
                if (disposable.partialEval && node.node() instanceof TernaryOperator ternary) {
                    if (node.parent() != null) {
                        node.parent().substituteNodeChildren(disposable.node.fieldName(),
                                ternary.getElseExpr(),
                                disposable.node.pos() == -1 ? null : disposable.node.pos());
                    } else {
                        calculationTree.changeRoot(ternary.getElseExpr());
                    }
                } else if (node.node() instanceof BinaryExpression binOp) {
                    log.debug("Possible disposable question element: {}", binOp.getRight());
                    if (node.parent() != null) {
                        node.parent().substituteNodeChildren(disposable.node.fieldName(),
                                binOp.getLeft(),
                                disposable.node.pos() == -1 ? null : disposable.node.pos()
                        );
                    } else {
                        calculationTree.changeRoot(binOp.getLeft());
                    }

                }
            }
        }
        for (Node.Info node : initialTree) {
            Optional<DisposableNodeInfo> foundDisposable = findDisposable(node.node().getId());
            if (foundDisposable.isPresent()) {
                if (node.node() instanceof BinaryExpression binOp) {
                    binOp.getLeft().setAssignedValueTag(!(binOp instanceof ShortCircuitAndOp));
                } else if (node.node() instanceof TernaryOperator ternary) {
                    ternary.getCondition().setAssignedValueTag(false);
                }
            }
        }
        log.debug("Question with disposed groups of tokens (initial): {}", languageTranslator.getCode(calculationTree));
        TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(calculationTree);
        initialDisposedFeatures = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
        initialDisposedFeatures.addAll(MeaningTreeOrderQuestionBuilder.findSkills(tokens, language));
    }

    // Посчитать по отключаемым зонам возможные типы ошибок в них
    private void calculateFeatures() {
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableNodeInfo grp = possibleDisposableOperands.get(i);
            calcDisposableGroupDiff(grp, initialDisposedFeatures);
        }
    }

    // Посчитать разницу между возможными ошибками в изначальном варианте (где все отключаемые группы не вычисляются) и с включенной только этой группой
    private void calcDisposableGroupDiff(DisposableNodeInfo id, Set<String> initial) {
        if (id.partialEval && id.node.node() instanceof TernaryOperator ternary) {
            for (int k = 0; k < 2; k++) {
                Node target = k == 0 ? ternary.getThenExpr() : ternary.getElseExpr();
                TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(target);
                Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
                currentViolations.addAll(MeaningTreeOrderQuestionBuilder.findSkills(tokens, language));
                currentViolations.removeAll(initial);
                groupFeatures.put(new DisposableIndex(id, k), currentViolations);
            }
        } else if (id.node.node() instanceof BinaryExpression binOp) {
            TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(binOp.getRight());
            Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
            currentViolations.addAll(MeaningTreeOrderQuestionBuilder.findSkills(tokens, language));
            currentViolations.removeAll(initial);
            groupFeatures.put(new DisposableIndex(id, 0), currentViolations);
        }
    }

    // Найти отключаемые группы, т.е. те, что полностью или частично не могут быть вычислены
    private void findDisposableGroups() {
        for (Node.Info nodeInfo : initialTree) {
            Node node = nodeInfo.node();
            if (isDisposableNode(node) && node instanceof BinaryExpression binOp) {
                possibleDisposableOperands.add(
                        new DisposableNodeInfo(nodeInfo, false, binOp instanceof ShortCircuitAndOp, false)
                );
            } else if (isDisposableNode(node) && node instanceof TernaryOperator){
                possibleDisposableOperands.add(
                        new DisposableNodeInfo(nodeInfo, true, false, false)
                );
            }
            if (nodeInfo.parent() != null) deps.put(node.getId(), nodeInfo.parent().getId());
        }
    }

    private boolean isDisposableNode(Node node) {
        return node instanceof ShortCircuitAndOp || node instanceof ShortCircuitOrOp || node instanceof TernaryOperator;
    }

    // Комбинаторика
    private static List<boolean[]> generateCombinations(int length) {
        if (length == 0) {
            return List.of();
        }
        List<boolean[]> combinations = new ArrayList<>();
        boolean[] current = new boolean[length];
        generateRecursively(combinations, current, 0);
        return combinations;
    }

    private static void generateRecursively(List<boolean[]> combinations, boolean[] current, int index) {
        if (index == current.length) {
            // Добавляем копию текущего массива в список
            combinations.add(current.clone());
            return;
        }

        // Устанавливаем текущий индекс в false и рекурсивно продолжаем
        current[index] = false;
        generateRecursively(combinations, current, index + 1);

        // Устанавливаем текущий индекс в true и рекурсивно продолжаем
        current[index] = true;
        generateRecursively(combinations, current, index + 1);
    }
}

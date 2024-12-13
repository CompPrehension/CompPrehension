package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.languages.LanguageTranslator;
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
public class OperandEvaluationMap {

    /**
     * Информация о возможного отключаемого участка в дереве, представляющее узел.
     * Представляет собой информацию о тернарном операторе или операторе И/ИЛИ
     */
    private record DisposableNodeInfo(Node.Info node, boolean partialEval, boolean valRequiredForEval) {
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
    private final SequencedMap<DisposableIndex, Set<String>> groupViolations = new LinkedHashMap<>();

    /**
     * Зависимости между узлами. Пример: a && b && c. Здесь c не имеет значения, если b = false, следовательно, его вычислимость зависит от b
     * А вариативность вопроса, где этот операнд с разными значениями в любом случае не вычислится, не нужна
     */
    private final HashMap<Long, Long> deps = new HashMap<>();

    /**
     * Изначальные типы ошибок, когда все потенциально не вычисляемые зоны отключены,
     * а те, что не отключаются полностью переведены в стандартное положение (у тернарного - false)
     */
    private Set<String> initialDisposedViolations;

    /**
     * Дерево, где все потенциально не вычисляемые участки переведены в значение,
     * при котором эти участки не вычислятся
     */
    private MeaningTree initialTree;

    private LanguageTranslator languageTranslator;

    OperandEvaluationMap(MeaningTreeOrderQuestionBuilder builder, SupportedLanguage language) {
        this.builder = builder;
        initialTree = builder.sourceExpressionTree.clone();

        log.info("Processing question for {} values generation: {}", language.toString(), builder.rawTranslatedCode);

        try {
            languageTranslator = language.createTranslator(new MeaningTreeDefaultExpressionConfig());
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }

        findDisposableGroups();
        calculateInitialViolations();
        calculateViolations();
        filterViolations();
    }

    /**
     * Сгенерировать уникальный набор значений, чтобы сделать разнообразные вопросы
     * @return словарь из ключа - позиции токена, которым нужно присвоить значение true/false
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
                if (grp.id().partialEval) {
                    nodeInfo.node().setAssignedValueTag(grp.alt == 0);
                    nodeInfo.node().removeLabel(NodeLabel.DUMMY);
                    preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), grp.alt == 0));
                } else {
                    nodeInfo.node().removeLabel(NodeLabel.DUMMY);
                    nodeInfo.node().setAssignedValueTag(!grp.id().valRequiredForEval);
                    preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), grp.id().valRequiredForEval));
                }
            }
        }

        // Больше 128 комбинаций не экономно по памяти и производительности
        if (groupViolations.size() > 7) {
            log.info("Too many values can be generated for question. Generated questions will be shorten");
            return List.of(new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash)));
        }

        // Здесь будут отфильтрованные комбинации, без бесполезных комбинаций
        Set<boolean[]> combinations = new HashSet<>();

        for (boolean[] array : generateCombinations(groupViolations.size())) {
            boolean[] arrayCopy = new boolean[array.length];
            System.arraycopy(array, 0, arrayCopy, 0, array.length);

            for (int i = 0; i < array.length; i++) {
                // Приводим одинаковые по смыслу комбинации к одному виду, чтобы они не дублировались в результирующем множестве
                DisposableIndex tokenIndex = groupViolations.sequencedKeySet().stream().toList().get(i);
                for (int j = i + 1; j < array.length; j++) {
                    DisposableIndex nextTokenIndex = groupViolations.sequencedKeySet().stream().toList().get(i);
                    if (tokenIndex.id().valRequiredForEval() != array[j]
                            && isTransitiveDependency(nextTokenIndex.getNodeId(), tokenIndex.getNodeId())) {
                        array[j] = false;
                    }
                }
            }
            combinations.add(arrayCopy);
        }

        List<boolean[]> values = new ArrayList<>(combinations.stream().limit(10).toList());
        List<Pair<MeaningTree, Integer>> result = new ArrayList<>();
        for (boolean[] combination : values) {
            result.add(makeTreeFromValues(initialTree, combination));
        }
        result.add(new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash)));

        return result;
    }

    // Собрать дерево, в котором потенциально не вычисляемые участки будут иметь заданную комбинацию значений
    private Pair<MeaningTree, Integer> makeTreeFromValues(MeaningTree initialTree, boolean[] combination) {
        MeaningTree preferred = initialTree.clone();
        int initialHash = preferred.hashCode();
        List<Pair<Integer, Boolean>> preferredValues = new ArrayList<>();

        // Заполнение начальных данных
        for (Node.Info nodeInfo : preferred) {
            Optional<DisposableIndex> foundDisposable = findDisposableIndex(nodeInfo.node().getId());
            if (foundDisposable.isPresent()) {
                DisposableIndex grp = foundDisposable.get();
                int index = groupViolations.sequencedKeySet().stream().toList().indexOf(grp);
                nodeInfo.node().setAssignedValueTag(combination[index]);
                preferredValues.add(new ImmutablePair<>(grp.getNode().hashCode(), combination[index]));
            }
        }
        return new ImmutablePair<>(preferred, Objects.hash(preferredValues, initialHash));
    }

    // Отобрать типы ошибок, которые будут полезны в вопросе
    private void filterViolations() {
        Set<DisposableIndex> filteredAndLimited = groupViolations.entrySet().stream()
                .flatMap(entry1 -> groupViolations.entrySet().stream()
                        .filter(entry2 -> !entry1.getKey().equals(entry2.getKey()))
                        .map(entry2 -> {
                            Set<String> difference = new HashSet<>(entry1.getValue());
                            difference.removeAll(entry2.getValue());
                            return new AbstractMap.SimpleEntry<>(entry1.getKey(), difference);
                        })
                        .filter(e -> e.getValue().size() > 2) // больше 2 различий
                )
                .distinct()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .skip(groupViolations.size() > 5 ? (int) (0.4 * groupViolations.size()) : 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<DisposableIndex> keys = new HashSet<>(groupViolations.keySet());
        keys.removeAll(filteredAndLimited);
        for (DisposableIndex key : keys) {
            groupViolations.remove(key);
        }
    }

    private Optional<DisposableNodeInfo> findDisposable(long compareId) {
        return possibleDisposableOperands
                .stream()
                .filter((DisposableNodeInfo id) -> id.node.node().getId() == compareId)
                .findFirst();
    }

    private Optional<DisposableIndex> findDisposableIndex(long compareId) {
        return groupViolations.keySet()
                .stream()
                .filter((DisposableIndex disposable) -> disposable.id().node.node().getId() == compareId)
                .findFirst();
    }

    // Зависит ли один узел от другого транзитивно
    private boolean isTransitiveDependency(long nodeId1, long nodeId2) {
        long base = nodeId1;
        do {
            base = deps.getOrDefault(base, -1L);
        } while (base != -1L && base != nodeId2);

        return base != -1L;
    }

    // Подсчитать, какие токены и типы ошибок будут у выражения, если отключить все не вычисляемые группы
    private void calculateInitialViolations() {
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
                } else {
                    node.node().setAssignedValueTag(false);
                }
            }
        }
        log.debug("Question with disposed groups of tokens (initial): {}", languageTranslator.getCode(calculationTree));
        TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(calculationTree);
        initialDisposedViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
    }

    // Посчитать по отключаемым зонам возможные типы ошибок в них
    private void calculateViolations() {
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableNodeInfo grp = possibleDisposableOperands.get(i);
            calcDisposableGroupDiff(grp, initialDisposedViolations);
        }
    }

    // Посчитать разницу между возможными ошибками в изначальном варианте (где все отключаемые группы не вычисляются) и с включенной только этой группой
    private void calcDisposableGroupDiff(DisposableNodeInfo id, Set<String> initial) {
        if (id.partialEval && id.node.node() instanceof TernaryOperator ternary) {
            for (int k = 0; k < 2; k++) {
                Node target = k == 0 ? ternary.getThenExpr() : ternary.getElseExpr();
                TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(target);
                Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
                currentViolations.removeAll(initial);
                groupViolations.put(new DisposableIndex(id, k), currentViolations);
            }
        } else if (id.node.node() instanceof BinaryExpression binOp) {
            TokenList tokens = languageTranslator.getTokenizer().tokenizeExtended(binOp.getRight());
            Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(tokens);
            currentViolations.removeAll(initial);
            groupViolations.put(new DisposableIndex(id, 0), currentViolations);
        }
    }

    // Найти отключаемые группы, т.е. те, что полностью или частично не могут быть вычислены
    private void findDisposableGroups() {
        for (Node.Info nodeInfo : initialTree) {
            Node node = nodeInfo.node();
            if (isDisposableNode(node) && node instanceof BinaryExpression binOp) {
                possibleDisposableOperands.add(
                        new DisposableNodeInfo(nodeInfo, false, binOp instanceof ShortCircuitAndOp)
                );
            } else if (isDisposableNode(node) && node instanceof TernaryOperator){
                possibleDisposableOperands.add(
                        new DisposableNodeInfo(nodeInfo, true, false)
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

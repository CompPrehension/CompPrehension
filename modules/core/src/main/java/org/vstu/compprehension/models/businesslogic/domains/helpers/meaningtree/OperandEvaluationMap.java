package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.meaningtree.utils.tokens.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс предназначен для генерации уникальных значений операндов в вопросе, которые влияют на вычисление выражения.
 * Работает по принципу поиска в выражении групп токенов, которые могут не вычисляться в вопросе из-за определенных значений операндов
 * В зависимости от найденных групп генерируются значения операндов, создающие уникальные по возможным типам ошибок и концептов вопросы
 *
 * На данный момент реализация класса выполнена не совсем качественно.
 * Он слишком громоздкий и сложный. К сожалению, это единственный выход в текущей реализации MeaningTree.
 * Общее дерево MeaningTree сейчас плохо поддерживает модификацию, поэтому вместо него здесь
 * предпочитается использовать менее понятную реализацию на токенах
 */
public class OperandEvaluationMap {
    // Набор токенов, принадлежащих операнду или операндам операции (например, цепочка из одного оператора: a && b && c)
    // partialEval указывает, что из данной группы что-то будет вычислено в любом случае и её нельзя исключать
    // В данном случае под это условие подходит тернарный оператор
    // Группа состоит из основного оператора и оставшегося операнда. Например: a && b, где такая группа: && - оператор, b - оставшаяся группа
    // Если группа - цепочная. Например: a && b && c. То - первый && - оператор, а b && c - оставшаяся группа
    private record DisposableGroup(OperatorToken operator, TokenGroup rest, boolean partialEval) {};

    // Специальный класс, который указывает на индекс из списка List<DisposableGroup>.
    // Суть в том, что у тернарного оператора вычисляет обязательно какой-то из операндов.
    // Поэтому поле alt (от 0) указывает какой именно, если это тернарный оператор. Для других операторов alt = 0
    private record DisposableIndex(int index, int alt) {};

    private MeaningTreeOrderQuestionBuilder builder;

    /**
     * Группы, которые можно отключить (не вычислять при определенном значении операнда) частично или полностью
     */
    private final List<DisposableGroup> possibleDisposableOperands = new ArrayList<>();

    /**
     *  [индекс отключаемой группы, номер альтернативы] -> уникальные типы ошибок студента, если эта группа вычислится
     */
    private final Map<DisposableIndex, Set<String>> groupViolations = new HashMap<>();

    /**
     * Изначальные типы ошибок, когда все возможные отключаемые группы отключены,
     * а те, что не отключаются полностью переведены в стандартное положение (у тернарного - false)
     */
    private Set<String> initialDisposedViolations;
    private TokenList initialDisposedTokens;

    OperandEvaluationMap(MeaningTreeOrderQuestionBuilder builder) {
        this.builder = builder;
        findDisposableGroups();
        calculateInitialViolations();
        calculateViolations();
        filterViolations();
    }

    private boolean isOperatorCanBeOmitted(OperatorToken op) {
        return op.arity == OperatorArity.TERNARY && ((ComplexOperatorToken)op).positionOfToken == 0
                || op.arity == OperatorArity.BINARY && op.additionalOpType != OperatorType.OTHER;
    }

    /**
     * Сгенерировать уникальный набор значений, чтобы сделать разнообразные вопросы
     * @return словарь из ключа - позиции токена, которым нужно присвоить значение true/false
     */
    public Pair<Integer[], List<boolean[]>> generate() {
        List<Integer> indexes = new ArrayList<>();

        // Предпочтительные значения для операндов, которые увеличат число возможных типов ошибок
        Map<Integer, Boolean> preferred = new HashMap<>();

        // Зависимости между токенами. Пример: a && b && c. Здесь c не имеет значения, если b = false, следовательно, его вычислимость зависит от b
        // А вариативность вопроса, где этот операнд с разными значениями в любом случае не вычислится, не нужна
        Map<Integer, Integer> dependencies = new HashMap<>();

        // Заполнение начальных данных
        for (DisposableIndex index : groupViolations.keySet().stream()
                .sorted((DisposableIndex first, DisposableIndex second) -> Integer.compare(
                        possibleDisposableOperands.get(first.index).rest.length(),
                        possibleDisposableOperands.get(second.index).rest.length()
                )).toList().reversed()
        ) {
            DisposableGroup grp = possibleDisposableOperands.get(index.index);
            int tokenPosInList = builder.tokens.indexOf(grp.operator);
            if (grp.partialEval) {
                preferred.put(tokenPosInList, index.alt == 0);
            } else {
                preferred.put(tokenPosInList, true);
            }
            if (!indexes.contains(tokenPosInList)) {
                indexes.add(tokenPosInList);
            }
            for (Token tok : grp.rest) {
                if (tok instanceof OperatorToken op && isOperatorCanBeOmitted(op)) {
                    int foundTokenIndex = builder.tokens.indexOf(tok);
                    if (tokenPosInList != foundTokenIndex) {
                        dependencies.put(foundTokenIndex, tokenPosInList);
                    }
                }
            }
        }

        boolean[] preferredValues = new boolean[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            preferredValues[i] = preferred.get(indexes.get(i));
        }

        // Больше 128 комбинаций не экономно по памяти и производительности
        if (indexes.size() > 7) {
            return new ImmutablePair<>(indexes.toArray(new Integer[0]), List.of(preferredValues));
        }

        // Здесь будут отфильтрованные комбинации, без бесполезных комбинаций
        Set<boolean[]> combinations = new HashSet<>();

        for (boolean[] array : generateCombinations(indexes.size())) {
            boolean[] arrayCopy = new boolean[array.length];
            System.arraycopy(array, 0, arrayCopy, 0, array.length);

            for (int i = 0; i < array.length; i++) {
                // Приводим одинаковые по смыслу комбинации к одному виду, чтобы они не дублировались в результирующем множестве
                int tokenIndex = indexes.get(i);
                int depTokenIndex = dependencies.getOrDefault(tokenIndex, -1);
                int depArrayIndex = indexes.indexOf(depTokenIndex);
                if (depTokenIndex != -1 && depArrayIndex != -1) {
                    OperatorToken op = (OperatorToken) builder.tokens.get(depTokenIndex);
                    if (op.arity == OperatorArity.BINARY && !array[depArrayIndex]) {
                        // Зануляем эту часть, так как в ней нет смысла, она не выполнится
                        arrayCopy[i] = false;
                    } else if (op.arity == OperatorArity.TERNARY) {
                        OperandToken operand = (OperandToken) builder.tokens.get(tokenIndex);
                        Map<OperandPosition, TokenGroup> operandsOfTernary = builder.tokens.findOperands(depTokenIndex);
                        boolean containsOperand = false;
                        OperandPosition targetBranch = array[depArrayIndex] ? OperandPosition.CENTER : OperandPosition.RIGHT;
                        for (Token tok : operandsOfTernary.get(targetBranch)) {
                            if (tok.equals(operand)) {
                                containsOperand = true;
                                break;
                            }
                        }
                        if (!containsOperand) {
                            arrayCopy[i] = false;
                        }
                    }
                }
            }
            combinations.add(arrayCopy);
        }

        List<boolean[]> result = new ArrayList<>(combinations.stream().limit(7).toList());
        if (preferredValues.length > 0) result.add(preferredValues); // Обязательно добавляем наиболее богатую по количеству ошибок комбинацию


        return new ImmutablePair<>(indexes.toArray(new Integer[0]), result);
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

    // Функция чистого представления токенов - игнорируются отключенные токены. Нужна, например, для поиска типов ошибок в них
    private TokenList clearTokens(TokenList tokens) {
        return new TokenList(tokens.stream().filter(Objects::nonNull).toList());
    }

    // Подсчитать, какие токены и типы ошибок будут у выражения, если отключить все не вычисляемые группы
    private void calculateInitialViolations() {
        TokenList tokens = builder.tokens;
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableGroup grp = possibleDisposableOperands.get(i);
            if (grp.partialEval) {
                TokenList op = grp.rest.findOperands(grp.rest.source.indexOf(grp.operator)).get(OperandPosition.RIGHT).copyToList();
                tokens = tokens.replace(grp.rest, null); // чтобы индексы не сдвигались вырезаем группу токенов фактической заменой их на null
                tokens.setAll(grp.rest.start, op);
            } else {
                tokens = tokens.replace(grp.rest, null);
                tokens.set(builder.tokens.indexOf(grp.operator), null);
            }
        }
        initialDisposedViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(clearTokens(tokens));
        initialDisposedTokens = tokens;
    }

    // Посчитать по отключаемым зонам возможные типы ошибок в них
    private void calculateViolations() {
        for (int i = 0; i < possibleDisposableOperands.size(); i++) {
            DisposableGroup grp = possibleDisposableOperands.get(i);
            calcDisposableGroupDiff(i, grp, initialDisposedViolations, initialDisposedTokens);
        }
    }

    // Посчитать разницу между возможными ошибками в изначальном варианте (где все отключаемые группы не вычисляются) и с включенной только этой группой
    private void calcDisposableGroupDiff(int i, DisposableGroup grp,
                                         Set<String> initial, TokenList tokens) {
        if (grp.partialEval) {
            var operands = grp.rest.findOperands(grp.rest.source.indexOf(grp.operator));
            for (int k = 0; k < 2; k++) {
                TokenList variant = tokens.clone();
                variant.setAll(grp.rest.start, operands.get(k == 0 ? OperandPosition.CENTER : OperandPosition.RIGHT).copyToList());
                Set<String> currentViolations = MeaningTreeOrderQuestionBuilder.findPossibleViolations(clearTokens(variant));
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

    // Найти отключаемые группы, т.е. те, что полностью или частично не могут быть вычислены
    private void findDisposableGroups() {
        HashSet<OperatorToken> visited = new HashSet<>();
        for (int i = 0; i < builder.tokens.size(); i++) {
            Token t = builder.tokens.get(i);
            if (t instanceof OperatorToken op && !visited.contains(op) && isOperatorCanBeOmitted(op)) {
                Map<OperandPosition, TokenGroup> operands = builder.tokens.findOperands(i);
                // У тернарного оператора операнды имеет только токен ? (или if), последующий токен вспомогательный
                // То же касается скобок вызова функций и индекса
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
                        // Chaining binary operator - т.е. случай, если a && b && c && d и т.д.
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

package org.vstu.compprehension.strategies;

import org.vstu.compprehension.data.exercise.ExerciseStageData;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.service.BktService;
import org.vstu.compprehension.frontend.dto.ExerciseSkillDto;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.SkillMasteryState;
import org.vstu.compprehension.bkt.grpc.SkillState;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainBase;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.businesslogic.strategies.StrategyBase;
import org.vstu.compprehension.strategies.util.LeafEngagedSkillsExtractor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BktStrategy extends StrategyBase {

    private final BktService bktService;
    private final DomainFactory domainFactory;
    private final StrategyOptions options;

    @Autowired
    public BktStrategy(BktService bktService, DomainFactory domainFactory,
                       ExerciseAttemptDataService exerciseAttemptService) {
        super(exerciseAttemptService);
        this.bktService = bktService;
        this.domainFactory = domainFactory;
        this.options = StrategyOptions.builder()
                .multiStagesEnabled(true)
                .visibleToUser(true)
                .build();
    }

    @NotNull
    @Override
    public String getStrategyId() {
        return "BktStrategy";
    }

    @NotNull
    @Override
    public String getDisplayName(Language language) {
        if (language == Language.RUSSIAN) {
            return "Стратегия на основе BKT";
        }
        return "BKT based strategy";
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        if (language == Language.RUSSIAN) {
            return "Стратегия использует BKT для отслеживания знаний студента " +
                    "и работает на основе состояния усвоения target skills";
        }
        return "The strategy uses BKT to track student knowledge " +
                "and works based on the mastery state of the target skills";
    }

    @NotNull
    @Override
    public StrategyOptions getOptions() {
        return options;
    }

    @Override
    public QuestionRequest generateQuestionRequest(long exerciseAttemptId) {
        var attempt = getAttempt(exerciseAttemptId);
        val exercise = attempt.exercise();
        val domain = domainFactory.getDomain(exercise.domainName());

        val stageTargetSkills = getStageForNextQuestion(attempt).getSkills()
                .stream()
                .filter(skill -> skill.getKind().equals(RoleInExercise.TARGETED))
                .map(ExerciseSkillDto::getName)
                .distinct()
                .toList();

        val userId = attempt.userId();

        val questionTargetSkillsNames =
                bktService.chooseBestQuestion(domain.getDomainId(), userId, stageTargetSkills);
        val questionTargetSkills = questionTargetSkillsNames.stream()
                .map(domain::getSkill)
                .toList();

        ExerciseStageData exerciseStage = getStageForNextQuestion(attempt);
        QuestionRequest qr = initQuestionRequest(attempt, exerciseStage, domain);

        Concept badConcept = domain.getConcept("SystemIntegrationTest");
        if (badConcept != null) {
            qr.getDeniedConcepts().add(badConcept);
        }

        if (!questionTargetSkills.isEmpty()) {

            // Заменяем Target-skills на список от BKT
            qr.setTargetSkills(new ArrayList<>(questionTargetSkills));
            val denied = new HashSet<>(qr.getDeniedSkills());
            Set<Skill> allowed = new HashSet<>();
            allowed.addAll(qr.getAllowedSkills());
            allowed.addAll(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.TARGETED));
            questionTargetSkills.forEach(allowed::remove);
            denied.forEach(allowed::remove);

            qr.setAllowedSkills(List.copyOf(allowed));
        }

        return adjustQuestionRequest(qr, attempt);
    }

    @Override
    public float grade(long exerciseAttemptId, Domain.InterpretSentenceResult judgeResult) {
        var attempt = getAttempt(exerciseAttemptId);
        updateUserKnowledgeModel(attempt, judgeResult);

        val exercise = attempt.exercise();
        val domain = (DomainBase) domainFactory.getDomain(exercise.domainName());

        val targetSkills = getTargetSkills(attempt);

        val userId = attempt.userId();

        // Сколько вопросов планировалось в упражнении
        val nQuestionsExpected = attempt.exercise()
                .stages()
                .stream()
                .mapToInt(ExerciseStageData::getNumberOfQuestions)
                .reduce(Integer::sum)
                .orElse(1);

        // Собираем все навыки, встречавшиеся в уже отвеченных вопросах
        val targetSkillsInAnswers = attempt.questions().stream()
                .flatMap(q -> {
                    if (q.metadata() == null) return Stream.empty();
                    return domain.skillsFromBitmask(q.metadata().skillBits()).stream()
                            .flatMap(s -> s.getClosestVisibleParents().stream()); // BKT учитывает только главные навыки
                })
                .map(Skill::getName)
                .filter(targetSkills::contains)
                .collect(Collectors.toSet());

        // Получаем состояние всех навыков в рамках attempt
        val skillStates = bktService.getSkillStates(domain.getDomainId(), userId, new ArrayList<>(targetSkillsInAnswers));
        val pCorr = skillStates.stream()
                .collect(Collectors.toMap(SkillState::getSkill, SkillState::getCorrectPrediction));

        float cumulative = 0f;

        // Подсчитываем оценку за уже выполненные вопросы
        for (AttemptQuestionData q : attempt.questions()) {

            // Ожидаемая корректность по target-навыкам вопроса
            List<String> qTargets;
            if (q.metadata() == null) {
                qTargets = Collections.emptyList();
            } else {
                qTargets = domain.skillsFromBitmask(q.metadata().skillBits()).stream()
                        .flatMap(s -> s.getClosestVisibleParents().stream()) // BKT учитывает только главные навыки
                        .map(Skill::getName)
                        .filter(targetSkills::contains)
                        .distinct()
                        .toList();
            }
            val expected = qTargets.isEmpty()
                    ? 1.0 // вопрос без target-умений
                    : qTargets.stream()
                    .mapToDouble(skill -> pCorr.getOrDefault(skill, 0.0))
                    .min()
                    .orElse(1.0);

            // Доля корректных действий
            val interactions = q.interactions();
            val totalInteractions = interactions.size();
            val correctInteractions = interactions.stream()
                    .filter(i -> i != null &&
                            (i.violationLawNames() == null || i.violationLawNames().isEmpty()))
                    .count();
            val accuracy = totalInteractions == 0
                    ? 0f
                    : (float) correctInteractions / Math.max(4, totalInteractions); // Math.max оставил как в static стратегии

            // Оценка за вопрос
            val questionScore = (accuracy == 1f)
                    ? 1.0
                    : expected * accuracy;

            cumulative += (float) questionScore;
        }

        // Если стратегия уже решила FINISH, назначаем полный балл за оставшиеся вопросы
        if (decide(exerciseAttemptId) == Decision.FINISH) {
            val remaining = nQuestionsExpected - attempt.questions().size();
            cumulative += remaining; // Считаем, что все не выданные вопросы решены верно
        }

        // Итоговая относительная оценка (0..1)
        return cumulative / nQuestionsExpected;
    }

    @Override
    public Decision decide(long exerciseAttemptId) {
        var exerciseAttempt = getAttempt(exerciseAttemptId);
        val exercise = exerciseAttempt.exercise();
        val domain = domainFactory.getDomain(exercise.domainName());

        val targetSkills = getTargetSkills(exerciseAttempt);

        val skillStates = bktService.getSkillStates(
                domain.getDomainId(),
                exerciseAttempt.userId(),
                targetSkills
        );

        List<AttemptQuestionData> questions = exerciseAttempt.questions();

        // Последний вопрос должен быть завершен
        AttemptQuestionData lastQuestion = questions.getLast();
        if (!isQuestionCompleted(lastQuestion)) {
            return Decision.CONTINUE;
        }

        // Быстрый выход: все целевые навыки уже освоены
        if (skillStates != null && !skillStates.isEmpty()) {
            boolean allMastered = skillStates
                    .stream()
                    .allMatch(skill ->
                            SkillMasteryState.fromValue(skill.getState()).equals(SkillMasteryState.MASTERED)
                    );
            if (allMastered) {
                return Decision.FINISH;
            }
        }

        // Проверка на минимальное число завершённых вопросов (как в static strategy)
        int minimumQuestionsToAsk = getNumberOfQuestionsToAsk(exerciseAttempt.exercise());

        long completedQuestions = questions.stream()
                .filter(this::isQuestionCompleted)
                .count();

        if (completedQuestions < minimumQuestionsToAsk) {
            return Decision.CONTINUE;
        }

        return Decision.FINISH;
    }

    private void updateUserKnowledgeModel(ExerciseAttemptWithQuestionsData exerciseAttempt, Domain.InterpretSentenceResult judgeResult) {
        if (judgeResult.decisionTreeTrace == null) return;

        val domain = domainFactory.getDomain(exerciseAttempt.exercise().domainName());
        val observedSkills = LeafEngagedSkillsExtractor.extract(judgeResult.decisionTreeTrace);

        Set<String> leafEngagedSkills;
        if (judgeResult.isAnswerCorrect) {
            leafEngagedSkills = observedSkills.getCorrectlyApplied();
        } else {
            leafEngagedSkills = observedSkills.getViolated();
        }
        val engagedSkills = calculateEngagedSkills(domain, leafEngagedSkills, judgeResult.isAnswerCorrect)
                .stream()
                .map(Skill::getName)
                .toList();
        bktService.updateBktRoster(
                domain.getDomainId(),
                exerciseAttempt.userId(),
                judgeResult.isAnswerCorrect,
                engagedSkills
        );
    }

    private List<String> getTargetSkills(ExerciseAttemptWithQuestionsData attempt) {
        return attempt
                .exercise()
                .stages()
                .stream()
                .flatMap(stage -> stage.getSkills().stream())
                .filter(skill -> skill.getKind().equals(RoleInExercise.TARGETED))
                .map(ExerciseSkillDto::getName)
                .distinct()
                .toList();
    }

    private boolean isQuestionCompleted(AttemptQuestionData q) {
        // Вопрос считается завершенным, когда в последнем взаимодействии
        // feedback.interactionsLeft == 0.
        if (q.interactions().isEmpty()) {
            return false;
        }
        AttemptQuestionInteractionData last = q.interactions().getLast();
        return last.interactionsLeft() == 0;
    }

    /**
     * Вычисляет навыки, которые считаются задействованными (примененными/нарушенными)
     * студентом при выборе ответа.
     * Некоторые навыки считаются примененными/нарушенными,
     * даже если в узел дерева с этим навыком интерпретатор не заходил
     * @param domain Домен
     * @param observedSkills Список наблюдаемых, в ответе студента, навыков (например, из трассы интерпретатора)
     * @param isCorrect Корректен ли ответ студента
     * @return Список задействованных навыков
     */
    private List<Skill> calculateEngagedSkills(Domain domain, Set<String> observedSkills, boolean isCorrect) {
        // TODO: логика работы этого метода привязана к дереву и связи между навыками
        //  должны строиться на уровне редактора деревьев, потом просто импортироваться
        //  вместе с деревом, запрашиваться у домена и использоваться.
        //  Но редактор пока этого не поддерживает (и не понятно, когда его доработают),
        //  поэтому пока захардкодил связи между навыками тут для домена выражений
        if (!domain.getShortName().equals("expression_dt")) return Collections.emptyList();
        return observedSkills
                .stream()
                .distinct()
                .flatMap(skill -> switch (skill) {
                    // 1 (номер на схеме дерева)
                    case "central_operand_needed" -> Stream.of("central_operand_needed");

                    // 2
                    case "is_central_operand_evaluated" -> isCorrect ?
                            Stream.of("is_central_operand_evaluated") :
                            Stream.of("is_central_operand_evaluated", "central_operand_needed");

                    // 3
                    case "nearest_operand_needed",
                         "left_operand_needed",
                         "right_operand_needed" -> Stream.of("nearest_operand_needed");

                    // 4
                    case "competing_operator_present",
                         "left_competing_operator_present",
                         "right_competing_operator_present" -> Stream.of("competing_operator_present");

                    // 5
                    case "current_operator_enclosed",
                         "left_operator_enclosed",
                         "right_operator_enclosed" -> Stream.of("current_operator_enclosed");

                    // 6
                    case "order_determined_by_parentheses",
                         "is_current_parenthesized_left_not",
                         "is_current_parenthesized_right_not",
                         "is_left_parenthesized_current_not",
                         "is_right_parenthesized_current_not" -> isCorrect ?
                            Stream.of("order_determined_by_parentheses") :
                            Stream.of("order_determined_by_parentheses", "competing_operator_present", "nearest_operand_needed");

                    // 7
                    case "order_determined_by_precedence",
                         "left_competing_to_right_precedence",
                         "right_competing_to_left_precedence" ->  isCorrect ?
                            Stream.of("order_determined_by_precedence", "order_determined_by_parentheses") :
                            Stream.of("order_determined_by_precedence", "order_determined_by_parentheses", "current_operator_enclosed", "competing_operator_present", "nearest_operand_needed");

                    // 8
                    case "associativity_without_opposing_operand",
                         "associativity_without_left_opposing_operand",
                         "associativity_without_right_opposing_operand" -> isCorrect ?
                            Stream.of("associativity_without_opposing_operand") :
                            Stream.of("associativity_without_opposing_operand", "order_determined_by_precedence", "order_determined_by_parentheses", "current_operator_enclosed", "competing_operator_present", "nearest_operand_needed");

                    // 9, 10
                    case "order_determined_by_associativity",
                         "left_competing_to_right_associativity" -> isCorrect ?
                            Stream.of("left_competing_to_right_associativity", "right_competing_to_left_associativity", "associativity_without_opposing_operand", "order_determined_by_precedence", "order_determined_by_parentheses") :
                            Stream.of("left_competing_to_right_associativity", "right_competing_to_left_associativity", "order_determined_by_precedence", "order_determined_by_parentheses", "current_operator_enclosed", "competing_operator_present", "nearest_operand_needed");
                    // 9, 10
                    case "right_competing_to_left_associativity" -> isCorrect ?
                            Stream.of("right_competing_to_left_associativity", "associativity_without_opposing_operand", "order_determined_by_precedence", "order_determined_by_parentheses") :
                            Stream.of("right_competing_to_left_associativity", "order_determined_by_precedence", "order_determined_by_parentheses", "current_operator_enclosed", "competing_operator_present", "nearest_operand_needed");

                    // 11
                    case "strict_order_operators_present",
                         "expression_strict_order_operators_present",
                         "earlyfinish_strict_order_operators_present" -> Stream.of("strict_order_operators_present");

                    // 12
                    case "is_current_operator_strict_order",
                         "is_current_operator_strict_order_while_solving",
                         "is_current_operator_strict_order_while_earlyfinish" -> Stream.of("is_current_operator_strict_order", "strict_order_first_operand_to_be_evaluated", "should_strict_order_current_operand_be_omitted", "no_omitted_operands_despite_strict_order", "is_first_operand_of_strict_order_operator_fully_evaluated");

                    // 13
                    case "strict_order_first_operand_to_be_evaluated",
                         "strict_order_first_operand_to_be_evaluated_while_solving",
                         "strict_order_first_operand_to_be_evaluated_while_earlyfinish" -> Stream.of("strict_order_first_operand_to_be_evaluated", "should_strict_order_current_operand_be_omitted", "no_omitted_operands_despite_strict_order", "is_first_operand_of_strict_order_operator_fully_evaluated");

                    // 14
                    case "is_first_operand_of_strict_order_operator_fully_evaluated" -> isCorrect ?
                            Stream.of("is_first_operand_of_strict_order_operator_fully_evaluated", "strict_order_first_operand_to_be_evaluated", "should_strict_order_current_operand_be_omitted", "no_omitted_operands_despite_strict_order") :
                            Stream.of("is_first_operand_of_strict_order_operator_fully_evaluated", "strict_order_first_operand_to_be_evaluated", "is_current_operator_strict_order", "strict_order_operators_present");

                    // 15
                    case "no_omitted_operands_despite_strict_order",
                         "no_omitted_operands_despite_strict_order_while_solving",
                         "no_omitted_operands_despite_strict_order_while_earlyfinish" -> Stream.of("no_omitted_operands_despite_strict_order", "strict_order_first_operand_to_be_evaluated", "should_strict_order_current_operand_be_omitted");

                    // 16
                    case "should_strict_order_current_operand_be_omitted",
                         "should_strict_order_current_operand_be_omitted_while_solving",
                         "should_strict_order_current_operand_be_omitted_while_earlyfinish" -> isCorrect ?
                            Stream.of("should_strict_order_current_operand_be_omitted", "no_omitted_operands_despite_strict_order", "strict_order_first_operand_to_be_evaluated") :
                            Stream.of("should_strict_order_current_operand_be_omitted", "no_omitted_operands_despite_strict_order", "strict_order_first_operand_to_be_evaluated", "is_current_operator_strict_order", "strict_order_operators_present");

                    // 17
                    case "are_central_operands_strict_order" -> Stream.of("are_central_operands_strict_order");

                    // 18
                    case "no_current_in_many_central_operands" -> Stream.of("no_current_in_many_central_operands");

                    // 19
                    case "no_comma_in_central_operands" -> Stream.of("no_comma_in_central_operands");

                    // 20
                    case "previous_central_operands_are_unevaluated" -> isCorrect ?
                            Stream.of("previous_central_operands_are_unevaluated") :
                            Stream.of("previous_central_operands_are_unevaluated", "no_comma_in_central_operands", "no_current_in_many_central_operands", "are_central_operands_strict_order");

                    default -> Stream.of();
                })
                .distinct()
                .map(domain::getSkill)
                .filter(Objects::nonNull)
                .toList();
    }
}

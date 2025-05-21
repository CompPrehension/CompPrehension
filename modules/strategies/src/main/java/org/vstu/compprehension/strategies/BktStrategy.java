package org.vstu.compprehension.strategies;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.Service.BktService;
import org.vstu.compprehension.dto.ExerciseSkillDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.Skill;
import org.vstu.compprehension.models.businesslogic.SkillMasteryState;
import org.vstu.compprehension.bkt.grpc.SkillState;
import org.vstu.compprehension.models.businesslogic.domains.DomainBase;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BktStrategy implements AbstractStrategy {

    private final BktService bktService;
    private final DomainFactory domainFactory;
    private final StrategyOptions options;

    @Autowired
    public BktStrategy(BktService bktService, DomainFactory domainFactory) {
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
    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public FeedbackType determineFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity attempt) {
        val exercise = attempt.getExercise();
        val domain = domainFactory.getDomain(exercise.getDomain().getName());

        val exerciseTargetSkills = getTargetSkills(attempt);

        val userId = attempt.getUser().getId();

        val questionTargetSkillsNames =
                bktService.chooseBestQuestion(domain.getDomainId(), userId, exerciseTargetSkills);
        val questionTargetSkills = questionTargetSkillsNames.stream()
                .map(domain::getSkill)
                .toList();

        ExerciseStageEntity exerciseStage = getStageForNextQuestion(attempt);
        QuestionRequest qr = initQuestionRequest(attempt, exerciseStage, domain);

        Concept badConcept = domain.getConcept("SystemIntegrationTest");
        if (badConcept != null) {
            qr.getDeniedConcepts().add(badConcept);
        }

        if (!questionTargetSkills.isEmpty()) {

            // 1. Заменяем Target-skills на список от BKT
            qr.setTargetSkills(new ArrayList<>(questionTargetSkills));

            // 2. Denied-skills не трогаем
            val denied = new HashSet<>(qr.getDeniedSkills());

            // 3. Все остальные skills считаем Allowed,
            //    даже если раньше они были Target в initQuestionRequest.
            Set<Skill> allowed = new HashSet<>();

            // 3.1) добавим то, что уже лежало в Allowed
            allowed.addAll(qr.getAllowedSkills());

            // 3.2) добавим старые Target (initQuestionRequest), чтобы «перекрасить» их в Allowed
            allowed.addAll(getExerciseStageSkillsWithImplied(exerciseStage.getSkills(), domain, RoleInExercise.TARGETED));

            // 3.3) удаляем из Allowed всё, что теперь Target или Denied
            questionTargetSkills.forEach(allowed::remove);
            denied.forEach(allowed::remove);

            qr.setAllowedSkills(List.copyOf(allowed));
        }

        return adjustQuestionRequest(qr, attempt);
    }

    @Override
    public float grade(ExerciseAttemptEntity attempt) {
        val exercise = attempt.getExercise();
        val domain = (DomainBase) domainFactory.getDomain(exercise.getDomain().getName());

        val targetSkills = getTargetSkills(attempt);

        val userId = attempt.getUser().getId();

        // 1. Сколько вопросов планировалось в упражнении
        val nQuestionsExpected = attempt.getExercise()
                .getStages()
                .stream()
                .mapToInt(ExerciseStageEntity::getNumberOfQuestions)
                .reduce(Integer::sum)
                .orElse(1);

        // 2. Собираем все навыки, встречавшиеся в уже отвеченных вопросах
        val targetSkillsInAnswers = attempt.getQuestions().stream()
                .flatMap(q -> {
                    if (q.getMetadata() == null) return Stream.empty();
                    return domain.skillsFromBitmask(q.getMetadata().getSkillBits()).stream()
                            .flatMap(s -> s.getClosestVisibleParents(new HashSet<>()).stream()); // BKT учитывает только главные навыки
                })
                .map(Skill::getName)
                .filter(targetSkills::contains)
                .collect(Collectors.toSet());

        // 3. Получаем состояние всех навыков в рамках attempt
        val skillStates = bktService.getSkillStates(domain.getDomainId(), userId, new ArrayList<>(targetSkillsInAnswers));

        // 3a. Преобразуем в Map<skillId, pCorrect>
        val pCorr = skillStates.stream()
                .collect(Collectors.toMap(SkillState::getSkill, SkillState::getCorrectPrediction));

        float cumulative = 0f;

        // 4. Подсчитываем оценку за уже выполненные вопросы
        for (QuestionEntity q : attempt.getQuestions()) {

            // 4.1 Ожидаемая корректность по target-навыкам вопроса
            List<String> qTargets;
            if (q.getMetadata() == null) {
                qTargets = Collections.emptyList();
            } else {
                qTargets = domain.skillsFromBitmask(q.getMetadata().getSkillBits()).stream()
                        .flatMap(s -> s.getClosestVisibleParents(new HashSet<>()).stream()) // BKT учитывает только главные навыки
                        .map(Skill::getName)
                        .filter(targetSkills::contains)
                        .distinct()
                        .toList();
            }
            val expected = qTargets.isEmpty()
                    ? 1.0 // вопрос без target-умений
                    : qTargets.stream()
                    .mapToDouble(skill -> pCorr.getOrDefault(skill, 0.0))
                    .reduce(1, (f, s) -> f * s);

            // 4.2 Доля корректных действий (сглаженная)
            val interactions = q.getInteractions();
            val totalInteractions = interactions.size();
            val correctInteractions = interactions.stream()
                    .filter(i -> i != null &&
                            (i.getViolations() == null || i.getViolations().isEmpty()))
                    .count();
            val accuracy = totalInteractions == 0
                    ? 0f
                    : (float) correctInteractions / Math.max(4, totalInteractions);

            // 4.3 Оценка за вопрос
            val questionScore = (accuracy == 1f)
                    ? 1.0
                    : expected * accuracy;

            cumulative += (float) questionScore;
        }

        // 5. Если стратегия уже решила «FINISH», назначаем полный балл за оставшиеся вопросы
        if (decide(attempt) == Decision.FINISH) {
            val remaining = nQuestionsExpected - attempt.getQuestions().size();
            cumulative += remaining; // Считаем, что все не выданные вопросы решены верно
        }

        // 6. Итоговая относительная оценка (0..1)
        return cumulative / nQuestionsExpected;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        val exercise = exerciseAttempt.getExercise();
        val domain = domainFactory.getDomain(exercise.getDomain().getName());

        val targetSkills = getTargetSkills(exerciseAttempt);

        val skillStates = bktService.getSkillStates(
                domain.getDomainId(),
                exerciseAttempt.getUser().getId(),
                targetSkills
        );

        List<QuestionEntity> questions = exerciseAttempt.getQuestions();

        // 1. Ограничение: последний вопрос должен быть завершён
        QuestionEntity lastQuestion = questions.getLast();
        if (!isQuestionCompleted(lastQuestion)) {
            return Decision.CONTINUE;
        }

        // 2. Быстрый выход: все целевые навыки уже освоены
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

        // 3. Проверка на минимальное число завершённых вопросов (как в static strategy)
        int minimumQuestionsToAsk = getNumberOfQuestionsToAsk(exerciseAttempt.getExercise());

        long completedQuestions = questions.stream()
                .filter(this::isQuestionCompleted)
                .count();

        if (completedQuestions < minimumQuestionsToAsk) {
            return Decision.CONTINUE;
        }

        return Decision.FINISH;
    }

    private List<String> getTargetSkills(ExerciseAttemptEntity attempt) {
        return attempt
                .getExercise()
                .getStages()
                .stream()
                .flatMap(stage -> stage.getSkills().stream())
                .filter(skill -> skill.getKind().equals(RoleInExercise.TARGETED))
                .map(ExerciseSkillDto::getName)
                .distinct()
                .toList();
    }

    /**
     * Вопрос считается завершённым, когда в последнем взаимодействии
     * feedback.interactionsLeft == 0.
     */
    private boolean isQuestionCompleted(QuestionEntity q) {
        if (q.getInteractions().isEmpty()) {
            return false;
        }
        InteractionEntity last = q.getInteractions().getLast();
        return last.getFeedback().getInteractionsLeft() == 0;
    }
}

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
import org.vstu.compprehension.models.businesslogic.SkillState;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import java.util.List;

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
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = domainFactory.getDomain(exercise.getDomain().getName());

        ExerciseStageEntity exerciseStage = getStageForNextQuestion(exerciseAttempt);

        QuestionRequest qr = initQuestionRequest(exerciseAttempt, exerciseStage, domain);

        Concept badConcept = domain.getConcept("SystemIntegrationTest");
        if (badConcept != null)
            qr.getDeniedConcepts().add(badConcept);

//        Random random = domain.getRandomProvider().getRandom();
//
//        //  * (0.8 .. 1.2)
//        float changeCoeff = 0.8f + 0.4f * random.nextFloat();
//        float complexity = qr.getComplexity() * changeCoeff;
//        qr.setComplexity(complexity);

        return adjustQuestionRequest(qr, exerciseAttempt);
    }


    @Override
    public float grade(ExerciseAttemptEntity exerciseAttempt) {
        // all questions defined by exercise
        int nQuestionsExpected = exerciseAttempt.getExercise().getStages().stream().mapToInt(ExerciseStageEntity::getNumberOfQuestions).reduce(Integer::sum).orElse(1);
        // current progress over all questions
        float cumulativeGrade = 0;
        for(QuestionEntity q : exerciseAttempt.getQuestions()) {
            List<InteractionEntity> interactions = q.getInteractions();
            int knownInteractions = interactions.size();
            long correctInteractions = interactions.stream()
                    .filter(inter -> inter != null && (inter.getViolations() == null || inter.getViolations().isEmpty()))
                    .count();
            if (knownInteractions == 0)
                continue;  // nothing done yet.
            cumulativeGrade += correctInteractions / (float) Math.max(4, knownInteractions);  // don't give best score for just first 1..3 interactions
        }

        return cumulativeGrade / nQuestionsExpected;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        val exercise = exerciseAttempt.getExercise();
        val domain = domainFactory.getDomain(exercise.getDomain().getName());

        val targetSkills = exerciseAttempt
                .getExercise()
                .getStages()
                .stream()
                .flatMap( stage -> stage.getSkills().stream())
                .filter(skill -> skill.getKind().equals(RoleInExercise.TARGETED))
                .map(ExerciseSkillDto::getName)
                .toList();

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
                            SkillState.fromValue(skill.getState()).equals(SkillState.MASTERED)
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

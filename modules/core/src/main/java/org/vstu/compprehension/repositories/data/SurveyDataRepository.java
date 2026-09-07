package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.survey.SurveyData;
import org.vstu.compprehension.data.survey.SurveyOptionsData;
import org.vstu.compprehension.data.survey.SurveyQuestionData;
import org.vstu.compprehension.data.survey.SurveyVoteData;
import org.vstu.compprehension.entities.SurveyAnswerEntity;
import org.vstu.compprehension.entities.SurveyEntity;
import org.vstu.compprehension.entities.SurveyQuestionEntity;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.SurveyAnswerRepository;
import org.vstu.compprehension.repositories.entity.SurveyRepository;
import org.vstu.compprehension.repositories.entity.SurveyRepository.SurveyVoteView;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
@RequiredArgsConstructor
public class SurveyDataRepository {

    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public @NotNull SurveyData getById(@NotNull String surveyId) {
        return toData(surveyRepository.findOne(surveyId)
                .orElseThrow(() -> new NoSuchElementException("Survey " + surveyId + " not found")));
    }

    @Transactional(readOnly = true)
    public @NotNull List<SurveyVoteData> findUserAttemptVotes(long userId, long attemptId,
                                                              @NotNull String surveyId) {
        return surveyRepository.findUserAttemptVotes(userId, attemptId, surveyId).stream()
                .map(SurveyDataRepository::toData)
                .toList();
    }

    @Transactional
    public void saveVote(long userId, @NotNull SurveyVoteData vote) {
        var surveyQuestion = surveyRepository.findSurveyQuestion(vote.surveyQuestionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Survey question " + vote.surveyQuestionId() + " not found"));

        var id = new SurveyAnswerEntity.SurveyResultId(
                vote.surveyQuestionId(), vote.questionId(), userId);
        var answer = surveyAnswerRepository.findById(id).orElseGet(SurveyAnswerEntity::new);
        answer.setSurveyQuestion(surveyQuestion);
        // Существование вопроса и пользователя доказано вызывающим — он проверил, что
        // вопрос принадлежит попытке этого пользователя. Поэтому ссылки без запроса.
        answer.setQuestion(questionRepository.getReferenceById(vote.questionId()));
        answer.setUser(userRepository.getReferenceById(userId));
        answer.setResult(vote.answer());
        surveyAnswerRepository.save(answer);
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull SurveyData toData(@NotNull SurveyEntity entity) {
        String surveyId = Strict.required(entity.getSurveyId(), "surveyId", "survey");
        String owner = "survey " + surveyId;
        var options = Strict.required(entity.getOptions(), "options", owner);
        return new SurveyData(
                surveyId,
                new SurveyOptionsData(Strict.required(options.getSize(), "options.size", owner)),
                Strict.required(entity.getQuestions(), "questions", owner).stream()
                        .map(SurveyDataRepository::toData)
                        .toList());
    }

    private static @NotNull SurveyQuestionData toData(@NotNull SurveyQuestionEntity entity) {
        long id = Strict.required(entity.getId(), "id", "survey question");
        String owner = "survey question " + id;
        return new SurveyQuestionData(
                id,
                Strict.required(entity.getType(), "type", owner),
                Strict.required(entity.getText(), "text", owner),
                entity.isRequired(),
                Strict.required(entity.getPolicy(), "policy", owner),
                Strict.required(entity.getOptions(), "options", owner));
    }

    private static @NotNull SurveyVoteData toData(@NotNull SurveyVoteView view) {
        long surveyQuestionId = Strict.required(view.getSurveyQuestionId(), "surveyQuestionId", "survey vote");
        return new SurveyVoteData(
                surveyQuestionId,
                Strict.required(view.getQuestionId(), "questionId", "vote on survey question " + surveyQuestionId),
                view.getAnswer());
    }
}

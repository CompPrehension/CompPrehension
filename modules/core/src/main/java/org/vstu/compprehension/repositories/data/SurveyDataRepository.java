package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.survey.SurveyData;
import org.vstu.compprehension.data.survey.SurveyVoteData;
import org.vstu.compprehension.entities.SurveyAnswerEntity;
import org.vstu.compprehension.entities.SurveyEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.SurveyAnswerRepository;
import org.vstu.compprehension.repositories.entity.SurveyRepository.SurveyVoteView;
import org.vstu.compprehension.repositories.entity.SurveyRepository;
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
    private final Mapper<SurveyEntity, SurveyData> surveyMapper;
    private final Mapper<SurveyVoteView, SurveyVoteData> surveyVoteMapper;

    @Transactional(readOnly = true)
    public @NotNull SurveyData getById(@NotNull String surveyId) {
        return surveyMapper.map(surveyRepository.findOne(surveyId)
                .orElseThrow(() -> new NoSuchElementException("Survey " + surveyId + " not found")));
    }

    @Transactional(readOnly = true)
    public @NotNull List<SurveyVoteData> findUserAttemptVotes(long userId, long attemptId,
                                                              @NotNull String surveyId) {
        return surveyVoteMapper.mapAll(
                surveyRepository.findUserAttemptVotes(userId, attemptId, surveyId));
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
        answer.setQuestion(questionRepository.getReferenceById(vote.questionId()));
        answer.setUser(userRepository.getReferenceById(userId));
        answer.setResult(vote.answer());
        surveyAnswerRepository.save(answer);
    }
}

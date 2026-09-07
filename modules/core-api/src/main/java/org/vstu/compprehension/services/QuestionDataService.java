package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.data.question.*;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;

import java.util.List;
import java.util.Optional;

public interface QuestionDataService {
     Question generateQuestion(long exerciseAttemptId);

     Question generateQuestion(int questionMetadataId, Language lang) ;

     @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(long sourceQuestionId, @NotNull ViolationData violation, Language lang);

     SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseData> responses, Language language);

     List<ResponseData> resolveAnswers(long questionId, List<SubmittedAnswerData> answers);

     Optional<InteractionResponsesData> findLatestCorrectInteraction(long questionId);

     RecordedInteractionData recordInteraction(NewInteractionData interaction);

     void gradeInteraction(long interactionId, float grade);

     Question getQuestion(Long questionId);

     Question getSolvedQuestion(Long questionId);

     Optional<Long> findQuestionOwnerUserId(Long questionId);

     void saveQuestion(Question question, @Nullable QuestionRequestLogData questionRequestLog, @Nullable Long exerciseAttemptId);
}

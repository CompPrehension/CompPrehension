package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.*;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;

import java.util.List;
import java.util.Optional;

public interface QuestionDataService {
     QuestionData generateQuestion(long exerciseAttemptId);

     QuestionData generateQuestion(int questionMetadataId, Language lang);

     @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(long sourceQuestionId, @NotNull ViolationData violation, Language lang);

     SupplementaryFeedbackDto judgeSupplementaryQuestion(long supplementaryQuestionId, List<? extends AnswerData> responses, Language language);

     List<AnswerData> resolveAnswers(long questionId, List<SubmittedAnswerData> answers);

     QuestionInteractionData recordInteraction(NewInteractionData interaction);

     void gradeInteraction(long interactionId, float grade);

     QuestionData getQuestion(Long questionId);

     QuestionData getSolvedQuestion(Long questionId);

     Optional<Long> findQuestionOwnerUserId(Long questionId);

     @NotNull QuestionData saveQuestion(@NotNull QuestionData question, @Nullable QuestionRequestLogData questionRequestLog, @Nullable Long exerciseAttemptId);
}

package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.frontend.dto.survey.SurveyDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyResultDto;

import java.util.List;

public interface SurveyFrontendService {
    @NotNull SurveyDto getSurvey(@NotNull String surveyId);

    @NotNull List<SurveyResultDto> getUserAttemptVotes(long userId, long attemptId, @NotNull String surveyId);

    void saveAnswer(long userId, @NotNull SurveyResultDto result);
}

package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;

@Component
@RequiredArgsConstructor
class SupplementaryQuestionDtoMapperImpl implements SupplementaryQuestionDtoMapper {

    private final QuestionDtoMapper questionDtoMapper;

    @Override
    public @NotNull SupplementaryQuestionDto map(@NotNull QuestionData question,
                                                @NotNull Language language) {
        var questionDto = questionDtoMapper.map(question, language);
        if (questionDto.getAnswers().length > 0) {
            return SupplementaryQuestionDto.FromQuestion(questionDto);
        }
        // TODO вынести в сервисный слой
        return SupplementaryQuestionDto.FromMessage(new SupplementaryFeedbackDto(
                FeedbackDto.Message.Success(questionDto.getText().replaceAll("<[^>]*>", "")),
                SupplementaryFeedbackDto.Action.Finish));
    }
}

package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;

@Component
@RequiredArgsConstructor
class SupplementaryQuestionDtoMapperImpl implements SupplementaryQuestionDtoMapper {

    private final QuestionDtoMapper questionDtoMapper;

    @Override
    public @NotNull SupplementaryQuestionDto map(@NotNull SupplementaryResponse response, @NotNull Language language) {
        if (response.getQuestion() == null) {
            return SupplementaryQuestionDto.FromMessage(response.getFeedback());
        }
        var question = questionDtoMapper.map(response.getQuestion(), language);
        if (question.getAnswers().length > 0) {
            return SupplementaryQuestionDto.FromQuestion(question);
        }
        // TODO вынести в сервисный слой
        // Вопрос без вариантов ответа спрашивать не о чем: он превращается в сообщение.
        return SupplementaryQuestionDto.FromMessage(new SupplementaryFeedbackDto(
                FeedbackDto.Message.Success(question.getText().replaceAll("<[^>]*>", "")),
                SupplementaryFeedbackDto.Action.Finish));
    }
}

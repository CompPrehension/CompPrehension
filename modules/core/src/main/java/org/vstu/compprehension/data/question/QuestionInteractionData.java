package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.enums.InteractionType;
import org.vstu.compprehension.data.exerciseattempt.AttemptInteractionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Одно взаимодействие студента с вопросом в объёме, нужном доменам.
 * <p>
 * Отличается от {@link AttemptInteractionData}: тот — read-only срез для стратегий,
 * где от нарушений нужны только имена законов. Здесь нарушения нужны целиком, потому
 * что домены их создают и разбирают.
 * <p>
 * Полей {@code orderNumber}, {@code createdAt}, {@code correctLaw}, {@code newResponses}
 * и {@code lastSupplementaryQuestion} тут нет: домены их не читают, а сервис работает
 * с сущностью напрямую.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInteractionData {
    private Long id;
    private InteractionType interactionType;
    private @Nullable FeedbackData feedback;
    @Builder.Default
    private List<ViolationData> violations = new ArrayList<>();
    @Builder.Default
    private List<ResponseData> responses = new ArrayList<>();
    @Builder.Default
    private List<CorrectLawData> correctLaw = new ArrayList<>();

    /**
     * Вопрос, которому принадлежит взаимодействие.
     * <p>
     * Обратная ссылка: домены строят по взаимодействию модель вопроса. Ссылка
     * двусторонняя и замкнутая — эти объекты живут только в памяти и не сериализуются.
     */
    private QuestionData question;
}

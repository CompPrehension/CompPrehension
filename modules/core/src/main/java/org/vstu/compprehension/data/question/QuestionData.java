package org.vstu.compprehension.data.question;

import org.vstu.compprehension.data.questionoptions.QuestionOptionsData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.enums.QuestionStatus;
import org.vstu.compprehension.data.enums.QuestionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Вопрос в виде, с которым работают домены.
 * <p>
 * Изменяемый контейнер, а не неизменяемый снимок: домены дописывают в вопрос варианты
 * ответов и факты решения по ходу генерации и разбора, как делали это раньше прямо
 * в {@code QuestionEntity}. Разделение на «прочитать» и «вернуть изменения» — отдельная
 * задача на будущее; сейчас цель в том, чтобы убрать из контрактов JPA.
 * <p>
 * Чего здесь намеренно нет:
 * <ul>
 *   <li>{@code exerciseAttempt} — привязку к попытке делает сервис после генерации;</li>
 *   <li>{@code domainEntity} — домен проставляет {@code QuestionService} при сохранении;</li>
 *   <li>{@code questionRequestLog} — запись журнала запроса тоже дело сервиса.</li>
 * </ul>
 * {@code QuestionOptionsData} и {@code BackendFactData} вопреки именам не JPA-сущности,
 * а значения из json-колонок, поэтому используются как есть.
 */
@Data
@NoArgsConstructor
public class QuestionData {
    private Long id;
    private QuestionType questionType;
    private QuestionStatus questionStatus;
    private String questionText;
    private String questionName;
    private Date createdAt;
    private @Nullable QuestionMetadataData metadata;
    private String questionDomainType;
    private QuestionOptionsData options;
    private @NotNull List<String> tags = new ArrayList<>(0);
    private List<AnswerObjectData> answerObjects = new ArrayList<>();
    private @NotNull List<QuestionInteractionData> interactions = new ArrayList<>(0);
    private List<BackendFactData> statementFacts = new ArrayList<>();
    private List<BackendFactData> solutionFacts = new ArrayList<>();
}

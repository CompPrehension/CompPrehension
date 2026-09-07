package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Вариант ответа в вопросе.
 * <p>
 * Изменяемый контейнер: домены собирают варианты по ходу генерации вопроса, как раньше
 * собирали {@code AnswerObjectEntity}. Имена аксессоров совпадают с сущностью намеренно —
 * чтобы переход не потребовал править сотни мест.
 * <p>
 * Обратных ссылок на вопрос и ответы студента здесь нет: бизнес-логика ими не пользуется,
 * а для записи в БД их проставляет сервис.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerObjectData {
    private Long id;
    private Integer answerId;
    private String hyperText;
    private String domainInfo;
    private boolean isRightCol;
    private String concept;
}

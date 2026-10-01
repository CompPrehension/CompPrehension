package org.vstu.compprehension.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.vstu.compprehension.data.question.HypothesisClarificationData;

import java.util.Date;

/** Уточняющий вопрос о рассуждении студента, заданный после его ответа, и ответ на него. */
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "interaction_clarification")
public class InteractionClarificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @Type(JsonType.class)
    @Column(name = "content", nullable = false)
    private HypothesisClarificationData content;

    // null — студент ещё не ответил.
    @Column(name = "answered_at")
    private Date answeredAt;

    // null при ответе — студент назвал другую причину.
    @Column(name = "chosen_hypothesis")
    private String chosenHypothesis;
}

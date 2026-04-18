package org.vstu.compprehension.models.entities.lti;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

/**
 * LTI-специфичные данные попытки. Заполняются только для попыток, созданных через LTI launch;
 * при прямом доступе через Keycloak запись не создаётся. PK совпадает с id родительского
 * {@link ExerciseAttemptEntity}.
 */
@Entity
@Table(name = "attempt_lti_context")
@Getter
@Setter
@NoArgsConstructor
public class AttemptLtiContextEntity {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private ExerciseAttemptEntity attempt;

    /** URL lineitem'а LTI AGS для отправки оценки. */
    @Column(name = "lineitem_url", nullable = false, length = 512)
    private String lineitemUrl;

    /** LTI {@code context.id} - идентификатор курса в LMS. */
    @Column(name = "context_id", length = 255)
    private String contextId;
}

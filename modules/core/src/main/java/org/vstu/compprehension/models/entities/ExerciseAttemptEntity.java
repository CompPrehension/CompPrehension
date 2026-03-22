package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "exercise_attempt")
public class ExerciseAttemptEntity {
    //TODO: Нужен ли здесь язык студента
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private AttemptStatus attemptStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ToString.Exclude
    @OneToMany(mappedBy = "exerciseAttempt", fetch = FetchType.LAZY)
    private List<QuestionEntity> questions;

    /**
     * URL lineitem'а LTI AGS для отправки оценки обратно в Moodle.
     * Заполняется при создании попытки из LTI-запуска; {@code null} при прямом доступе через Keycloak.
     */
    @Column(name = "lti_lineitem_url")
    private String ltiLineitemUrl;

    /**
     * Идентификатор контекста курса Moodle из LTI JWT (claim {@code context.id}).
     * Используется для идентификации курса при grade passback; {@code null} при прямом доступе.
     */
    @Column(name = "lti_context_id")
    private String ltiContextId;

    /**
     * Внутренний идентификатор пользователя на платформе Moodle (claim {@code sub} из LTI JWT).
     * Необходим для grade passback через AGS - Moodle сопоставляет оценку с пользователем именно по этому значению.
     * Не совпадает с {@code externalId} пользователя в системе (тот хранит {@code issuer + "_" + sub}).
     * {@code null} при прямом доступе через Keycloak.
     */
    @Column(name = "lti_user_id")
    private String ltiUserId;
}

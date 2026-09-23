package org.vstu.compprehension.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.vstu.compprehension.enums.Language;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user", indexes = {
    @Index(columnList = "external_id", name = "external_id_hidx"),
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "external_id")
    private String externalId;

    /**
     * Идентификатор пользователя во внешней LMS (например Moodle {@code sub} из LTI JWT).
     * Используется для grade passback как получатель оценки.
     */
    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "preferred_language", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Language preferred_language;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;
}

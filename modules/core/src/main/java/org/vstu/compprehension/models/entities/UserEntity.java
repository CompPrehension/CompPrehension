package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "User", indexes = {
    @Index(columnList = "external_id", name = "external_id_hidx"),
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "birthdate")
    private Date birthdate;

    @Column(name = "login")
    private String login;

    @Column(name = "external_id")
    private String externalId;

    /**
     * Идентификатор пользователя во внешней LMS (например Moodle {@code sub} из LTI JWT).
     * Используется для grade passback как получатель оценки.
     */
    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "preferred_language")
    @Enumerated(EnumType.ORDINAL)
    private Language preferred_language;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;
}

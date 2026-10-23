package org.vstu.compprehension.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Language;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "idp_issuer")
    private @Nullable String idpIssuer;

    @Column(name = "idp_subject")
    private @Nullable String idpSubject;

    @Column(name = "preferred_language", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Language preferred_language;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;
}

package org.vstu.compprehension.models.entities;

import lombok.ToString;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
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

    @Column(name = "preferred_language")
    @Enumerated(EnumType.ORDINAL)
    private Language preferred_language;

    @ElementCollection(targetClass = Role.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @CollectionTable(name = "UserRole", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private List<Role> roles;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "UserExercise",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_id"))
    private List<ExerciseEntity> exercises;

}

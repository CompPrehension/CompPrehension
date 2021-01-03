package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.Role;
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
@Table(name = "User")
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

    @Column(name = "preferred_language")
    @Enumerated(EnumType.ORDINAL)
    private Language preferred_language;

    @ElementCollection(targetClass = Role.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @CollectionTable(name = "UserRole", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private List<Role> roles;


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserActionEntity> userActions;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserCourseRoleEntity> userCourseRoles;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ExerciseAttemptEntity> exerciseAttempts;


    @ManyToMany()
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinTable(
            name = "UserGroup",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"))
    private List<GroupEntity> groups;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "UserExercise",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_id"))
    private List<ExerciseEntity> exercises;

}

package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Course")
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<ExerciseEntity> exercises;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<UserCourseRoleEntity> userCourseRoles;
}

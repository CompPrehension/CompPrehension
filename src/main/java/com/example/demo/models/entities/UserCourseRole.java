package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.CourseRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "UserCourseRole")
public class UserCourseRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "role")
    @Enumerated(EnumType.ORDINAL)
    private CourseRole courseRole;
}

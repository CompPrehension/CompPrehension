package org.vstu.compprehension.models.entities.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "permission_scope",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_permission_scope_kind_course_eduRes",
        columnNames = {"kind", "course_id", "education_resource_id"}
    )
)
public class PermissionScopeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private PermissionScopeKind kind;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "education_resource_id")
    private EducationResourceEntity educationResource;

    public static PermissionScopeEntity ofGlobalScope() {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.GLOBAL;
        return scope;
    }

    public static PermissionScopeEntity ofCourseScope(CourseEntity course) {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.COURSE;
        scope.course = course;
        return scope;
    }

    public static PermissionScopeEntity ofEducationResourceScope(EducationResourceEntity educationResource) {
        var scope = new PermissionScopeEntity();
        scope.kind = PermissionScopeKind.EDUCATION_RESOURCE;
        scope.educationResource = educationResource;
        return scope;
    }
}

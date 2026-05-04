package org.vstu.compprehension.models.entities.course;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExerciseCourseLinkId implements Serializable {
    private Long exercise;
    private Long course;
}

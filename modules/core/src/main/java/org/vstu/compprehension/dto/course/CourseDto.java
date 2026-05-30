package org.vstu.compprehension.dto.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private long id;
    private String name;
    private long educationResourceId;
    private String educationResourceName;
}

package org.vstu.compprehension.frontend.dto.course;

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
    /** У образовательного ресурса нет имени — подписью служит его адрес. */
    private String educationResourceUrl;
}

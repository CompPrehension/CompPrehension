package org.vstu.compprehension.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Закон, корректно применённый студентом в рамках взаимодействия. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectLawData {
    private Long id;
    private String lawName;
}

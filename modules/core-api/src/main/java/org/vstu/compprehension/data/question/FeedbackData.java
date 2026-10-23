package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Оценка и остаток шагов по итогам взаимодействия. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackData {
    private Long id;
    private float grade;
    private int interactionsLeft;
}

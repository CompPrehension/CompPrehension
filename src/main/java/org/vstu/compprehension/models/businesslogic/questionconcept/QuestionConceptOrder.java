package org.vstu.compprehension.models.businesslogic.questionconcept;

import org.vstu.compprehension.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptOrder {

    private String startConcept;

    private String notInOrderConcept;

    private String followVerb;

    private String notInOrderVerb;

    private QuestionEntity question;
}

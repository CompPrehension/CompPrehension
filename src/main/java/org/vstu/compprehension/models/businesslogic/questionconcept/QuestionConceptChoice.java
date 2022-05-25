package org.vstu.compprehension.models.businesslogic.questionconcept;

import org.vstu.compprehension.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptChoice {
    private String selectedVerb;

    private String notSelectedVerb;

    private String selectedConcept;

    private String notSelectedConcept;

    private QuestionEntity question;
}

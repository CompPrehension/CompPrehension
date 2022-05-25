package org.vstu.compprehension.models.businesslogic.questionconcept;

import org.vstu.compprehension.models.entities.QuestionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionConceptMatch {
    private String matchVerb;

    private String noMatchLeftConcept;

    private String noMatchLeftVerb;

    private String noMatchRightConcept;

    private String noMatchRightVerb;

    private QuestionEntity question;
}

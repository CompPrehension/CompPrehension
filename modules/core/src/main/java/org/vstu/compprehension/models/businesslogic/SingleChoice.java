package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.QuestionEntity;

public class SingleChoice extends Choice {

    public SingleChoice(QuestionEntity questionData, Domain domain) {
        super(questionData, domain);
    }

}

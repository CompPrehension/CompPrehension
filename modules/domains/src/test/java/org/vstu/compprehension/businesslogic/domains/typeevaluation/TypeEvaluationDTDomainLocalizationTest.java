package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.vstu.compprehension.businesslogic.domains.DecisionTreeDomainLocalizationContract;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;

import java.util.List;

class TypeEvaluationDTDomainLocalizationTest extends DecisionTreeDomainLocalizationContract {

    @Override
    protected DecisionTreeReasoningDomain domain() {
        return TypeEvaluationDomainFixture.domain();
    }

    @Override
    protected List<String> messageBundles() {
        return TypeEvaluationDomainFixture.BUNDLES;
    }
}

package org.vstu.compprehension.models.businesslogic.domains.helpers;

import its.model.definition.Domain;
import its.model.definition.EnumValueRef;
import its.model.definition.ObjectDef;
import its.model.nodes.DecisionTree;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A helper class used to solve problems from the {@link ProgrammingLanguageExpressionDTDomain}
 */
public class ProgrammingLanguageExpressionsSolver {
    public static List<ObjectDef> getUnevaluated(Domain domain){
        return domain.getObjects().stream()
            .filter(obj ->
                obj.isInstanceOf("operator")
                && new EnumValueRef("state", "unevaluated").equals(obj.getPropertyValue("state"))
            )
            .sorted(Comparator.comparing(obj -> (Integer) obj.getPropertyValue("precedence")))
            .toList();
    }

    public boolean solveForX(ObjectDef xObject, Domain domain, DecisionTree decisionTree){
        LearningSituation situation = new LearningSituation(
            domain,
            new HashMap<>(Collections.singletonMap("X", xObject.getReference()))
        );
        return DecisionTreeReasoner.solve(decisionTree, situation).getLast().getNode().getValue();
    }

    private void solve(Domain domain, DecisionTree decisionTree, BiConsumer<ObjectDef, ObjectDef> retain) {
        Domain situationDomain = domain.copy();

        List<ObjectDef> unevaluated = getUnevaluated(situationDomain);
        while (!unevaluated.isEmpty()){
            for(ObjectDef obj : unevaluated){
                solveForX(obj, situationDomain, decisionTree);
            }
            unevaluated = getUnevaluated(situationDomain);
        }

        domain.getObjects().forEach(domainObj -> retain.accept(
            domainObj,
            situationDomain.getObjects().get(domainObj.getName())
        ));
    }

    public void solveTree(Domain domain, Map<String, DecisionTree> decisionTreeMap){
        solve(domain, decisionTreeMap.get("no_strict"), (domainObj, solvedObj) -> {
            domainObj.getRelationshipLinks().addAll(
                solvedObj.getRelationshipLinks().stream()
                    .filter(link -> link.getRelationshipName().contains("OperandOf"))
                    .toList()
            );
            Optional.ofNullable(solvedObj.getDefinedPropertyValues().get("evaluatesTo"))
                .ifPresent(propertyValue -> domainObj.getDefinedPropertyValues().addOrReplace(propertyValue));
        });
    }

    public void solveStrict(Domain domain, Map<String, DecisionTree> decisionTreeMap){
        solve(domain, decisionTreeMap.get(""), (domainObj, solvedObj) -> {
            Optional.ofNullable(solvedObj.getDefinedPropertyValues().get("state"))
                .filter(property ->
                    property.getValue() instanceof EnumValueRef enumValueRef
                        && "omitted".equals(enumValueRef.getValueName())
                )
                .ifPresent(propertyValue -> domainObj.getDefinedPropertyValues().addOrReplace(propertyValue));
        });
    }


}

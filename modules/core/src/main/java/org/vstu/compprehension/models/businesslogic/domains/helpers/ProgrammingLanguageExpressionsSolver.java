package org.vstu.compprehension.models.businesslogic.domains.helpers;

import its.model.definition.DomainModel;
import its.model.definition.EnumValueRef;
import its.model.definition.ObjectDef;
import its.model.nodes.DecisionTree;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A helper class used to solve problems from the {@link ProgrammingLanguageExpressionDTDomain}
 */
public class ProgrammingLanguageExpressionsSolver {
    public record SolveResult(boolean solved,
                              List<String> laws,
                              List<String> skills,
                              List<DecisionTreeReasoner.DecisionTreeEvaluationResult> dtNodes) {}

    public static List<ObjectDef> getUnevaluated(DomainModel domain) {
        return domain.getObjects().stream()
            .filter(obj ->
                obj.isInstanceOf("operator")
                && new EnumValueRef("state", "unevaluated").equals(obj.getPropertyValue("state"))
            )
            .sorted(Comparator.comparing(obj -> (Integer) obj.getPropertyValue("precedence")))
            .toList();
    }

    public SolveResult solveForX(ObjectDef xObject, DomainModel domain, DecisionTree decisionTree) {
        LearningSituation situation = new LearningSituation(
            domain,
            new HashMap<>(Collections.singletonMap("X", xObject.getReference()))
        );
        List<DecisionTreeReasoner.DecisionTreeEvaluationResult> lst = DecisionTreeReasoner.solve(decisionTree, situation);
        List<String> skills = new ArrayList<>();
        List<String> laws = new ArrayList<>();
        boolean solved = lst.getLast().getNode().getValue();
        for (DecisionTreeReasoner.DecisionTreeEvaluationResult res : lst) {
            String[] resSkill = res.getNode().getMetadata().containsAny("skill") && res.getNode().getMetadata().get("skill") != null ?
                    res.getNode().getMetadata().get("skill").toString().split(";") : new String[0];
            String[] resLaw = res.getNode().getMetadata().containsAny("law") && res.getNode().getMetadata().get("law") != null ?
                    res.getNode().getMetadata().get("law").toString().split(";") : new String[0];
            Collections.addAll(skills, resSkill);
            Collections.addAll(laws, resLaw);
        }
        return new SolveResult(solved, laws, skills, lst);
    }


    private void solve(DomainModel domain, DecisionTree decisionTree, BiConsumer<ObjectDef, ObjectDef> retain) {
        DomainModel situationDomain = domain.copy();

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

    public void solveTree(DomainModel domain, Map<String, DecisionTree> decisionTreeMap) {
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

    public void solveStrict(DomainModel domain, Map<String, DecisionTree> decisionTreeMap) {
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

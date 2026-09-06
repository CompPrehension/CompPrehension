package helpers;

import its.model.definition.DomainModel;
import its.model.definition.ObjectDef;
import its.model.definition.ObjectRef;
import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
import its.reasoner.nodes.DecisionTreeTrace;
import org.vstu.compprehension.models.businesslogic.Explanation;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.*;

public class GenerateErrorTextForScopeObjects {

    private static Map<String, String> enumToString = Map.of(
            "positionExpression:right", "text.right",
            "positionExpression:left", "text.left",
            "positionExpression:center", "text.center",
            "typeVariable:input", "text.inputR",
            "typeVariable:output", "text.outputR",
            "typeVariable:mutable", "text.mutableR"
    );

    private static Map<String, String> enumToStringI = Map.of(
            "typeVariable:input", "text.inputI",
            "typeVariable:output", "text.outputI",
            "typeVariable:mutable", "text.mutableI"
    );

    public static String generateMessage(String template, Map<String, ObjectRef> situation, DomainModel domain) {
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Pattern pattern1 = Pattern.compile("->|\\.|[\\w)(]+|\\$\\w+");
            Matcher matcher1 = pattern1.matcher(matcher.group(1));

            ArrayList<String> tokens = new ArrayList<>();

            while (matcher1.find()) {
                tokens.add(matcher1.group());
            }

            String[] array = tokens.toArray(new String[0]);
            ObjectDef obj;
            if(array[0].charAt(0) == '$') {
                obj = domain.getObjects().get(array[0].substring(1));
            } else {
                obj = situation.get(array[0]).findIn(domain);
            }

            String value = "";
            boolean isI = false;
            for (int i = 1; i < array.length; i += 2) {
                if(array[i].equals("->")) {
                    int nextIndex = i+1;
                    obj = domain.getObjects().get(
                            obj.getRelationshipLinks().stream().filter(
                                    relationshipLinkStatement -> relationshipLinkStatement.getRelationshipName().equals(array[nextIndex])
                            ).findFirst().orElse(null).getObjectNames().get(0)
                    );
                } else if (array[i].equals(".")) {
                    if(array[i+1].contains("(I)")) {
                        String property = array[i+1].replace("(I)", "");
                        isI = true;
                        value = obj.getPropertyValue(property, Map.of()).toString();
                    } else {
                        value = obj.getPropertyValue(array[i+1], Map.of()).toString();
                    }
                }
            }
            if(isI && enumToStringI.get(value) != null) {
                value = enumToStringI.get(value);
            } else if(!isI && enumToString.get(value) != null) {
                value = enumToString.get(value);
            }
            matcher.appendReplacement(result, value);
        }
        matcher.appendTail(result);
        if (!result.isEmpty()) {
            char firstChar = Character.toUpperCase(result.charAt(0));
            result.setCharAt(0, firstChar);
        }
        return result.toString();
    }

    public static Explanation generateErrorExplanation(DecisionTreeTrace trace, DomainModel situationDomain, Language language) {
        Explanation result = new Explanation(Explanation.Type.ERROR, "");;
        collectErrorExplanation(result, trace, situationDomain, language);

        if(!result.getChildren().isEmpty() && result.getChildren().getFirst().getChildren().size() > 4) {
            int countErrors = result.getChildren().getFirst().getChildren().size();

            SequencedSet<Explanation> firstExplanation = result.getChildren().getFirst().getChildren().stream()
                    .limit(4)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if(language.equals(Language.ENGLISH)) {
                firstExplanation.add(new Explanation(Explanation.Type.ERROR, "and " + (countErrors-4) + " other errors."));
            } else if(language.equals(Language.RUSSIAN) && countErrors-4 == 1) {
                firstExplanation.add(new Explanation(Explanation.Type.ERROR, "И еще " + (countErrors-4) + " ошибка."));
            } else if (language.equals(Language.RUSSIAN) && countErrors-4 >= 2 && countErrors-4 <= 4) {
                firstExplanation.add(new Explanation(Explanation.Type.ERROR, "И еще " + (countErrors-4) + " ошибки."));
            } else if (language.equals(Language.RUSSIAN) && countErrors-4 >= 5) {
                firstExplanation.add(new Explanation(Explanation.Type.ERROR, "И еще " + (countErrors-4) + " ошибок."));
            }
            result = new Explanation(Explanation.Type.ERROR, result.getChildren().getFirst().getRawMessage().toString(), firstExplanation.stream().toList());
        }
        return new Explanation(
                Explanation.Type.ERROR,
                result.toHyperTextWithoutCommonPrefix(language)
        );
    }

    public static void collectErrorExplanation(Explanation explanation, DecisionTreeTrace trace, DomainModel situationDomain, Language language) {
        if(trace.getResultingElement().isAggregated()) {
            DecisionTreeTrace lastDecisionTreeTrace = new DecisionTreeTrace(List.of(trace.get(trace.size() - 1)));
            String errorNodeText = "";
            if(!lastDecisionTreeTrace.getResultingElement().isAggregated() && lastDecisionTreeTrace.getBranchResult() == BranchResult.ERROR && lastDecisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                errorNodeText = generateMessage(lastDecisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), lastDecisionTreeTrace.getFinalVariableSnapshot(), situationDomain);
            }
            Explanation newExplanation;
                newExplanation =  new Explanation(Explanation.Type.ERROR, errorNodeText);
                explanation.getChildren().add(newExplanation);

            for (DecisionTreeTrace decisionTreeTrace : trace.getResultingElement().nestedTraces()) {
                collectErrorExplanation(newExplanation, decisionTreeTrace, situationDomain, language);
            }
        } else {
            if(!trace.getResultingElement().isAggregated() && trace.getBranchResult() == BranchResult.ERROR && trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                String errorNodeText = generateMessage(trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), trace.getFinalVariableSnapshot(), situationDomain);
                explanation.getChildren().add(new Explanation(Explanation.Type.ERROR, errorNodeText));
            }
        }
    }


    public static Explanation generateHintExplanation(DecisionTreeTrace trace, DomainModel situationDomain, Language language) {
        Explanation result = new Explanation(Explanation.Type.HINT, "");;
        boolean isHint = collectHintExplanation(result, trace, situationDomain, language);
        if(isHint) {
            Explanation explanation = new Explanation(
                    Explanation.Type.HINT,
                    ""
            );
            explanation.getChildren().add(new Explanation(Explanation.Type.HINT,
                    result.toHyperTextWithoutCommonPrefix(language)
            ));
            return explanation;
        } else {
            Explanation explanation = new Explanation(
                    Explanation.Type.HINT,
                    ""
            );
            explanation.getChildren().add(new Explanation(Explanation.Type.HINT, ""));
            return explanation;
        }
    }

    public static Explanation generateHintExplanationDataFlow(DecisionTreeTrace trace, DomainModel situationDomain, Language language) {
        Explanation result = new Explanation(Explanation.Type.HINT, "");
        boolean isHint = true;
        if(!trace.getResultingElement().isAggregated()) {

            String hintText = "";
            if (trace.getBranchResult() == BranchResult.ERROR && trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                isHint=false;
            } else if (trace.getBranchResult() == BranchResult.CORRECT && trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null && trace.getResultingNode() instanceof BranchResultNode) {
                hintText = generateMessage(trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), trace.getFinalVariableSnapshot(), situationDomain);
            }
            if(isHint) {
                result = new Explanation(Explanation.Type.HINT, hintText);
                for (DecisionTreeTrace decisionTreeTrace : trace.get(1).nestedTraces()) {
                    if (decisionTreeTrace.getBranchResult() == BranchResult.ERROR && decisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                        isHint=false;
                        break;
                    } else if (decisionTreeTrace.getBranchResult() == BranchResult.CORRECT && decisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null && decisionTreeTrace.getResultingNode() instanceof BranchResultNode) {
                        hintText = generateMessage(decisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), decisionTreeTrace.getFinalVariableSnapshot(), situationDomain);
                        result.getChildren().add(new Explanation(Explanation.Type.HINT, hintText));
                    }
                }
            }
        } else {
            isHint = collectHintExplanation(result, trace, situationDomain, language);
        }

        if(isHint) {
            Explanation explanation = new Explanation(
                    Explanation.Type.HINT,
                    ""
            );
            explanation.getChildren().add(new Explanation(Explanation.Type.HINT,
                    result.toHyperTextWithoutCommonPrefix(language)
            ));
            return explanation;
        } else {
            Explanation explanation = new Explanation(
                    Explanation.Type.HINT,
                    ""
            );
            explanation.getChildren().add(new Explanation(Explanation.Type.HINT, ""));
            return explanation;
        }
    }

    public static boolean collectHintExplanation(Explanation explanation, DecisionTreeTrace trace, DomainModel situationDomain, Language language) {
        boolean result = true;
        if(trace.getResultingElement().isAggregated()) {
            DecisionTreeTrace lastDecisionTreeTrace = new DecisionTreeTrace(List.of(trace.get(trace.size() - 1)));
            String hintText = "";
            if (lastDecisionTreeTrace.getBranchResult() == BranchResult.ERROR && lastDecisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                return false;
            } else if (lastDecisionTreeTrace.getBranchResult() == BranchResult.CORRECT && lastDecisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null && lastDecisionTreeTrace.getResultingNode() instanceof BranchResultNode) {
                hintText = generateMessage(lastDecisionTreeTrace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), lastDecisionTreeTrace.getFinalVariableSnapshot(), situationDomain);
            }
            Explanation newExplanation;
            newExplanation = new Explanation(Explanation.Type.HINT, hintText);
            explanation.getChildren().add(newExplanation);

            for (DecisionTreeTrace decisionTreeTrace : trace.getResultingElement().nestedTraces()) {
                result &= collectHintExplanation(newExplanation, decisionTreeTrace, situationDomain, language);
            }
        } else {
            if (trace.getBranchResult() == BranchResult.ERROR && trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null) {
                return false;
            } else if (trace.getBranchResult() == BranchResult.CORRECT && trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation") != null && trace.getResultingNode() instanceof BranchResultNode) {
                String hintText = generateMessage(trace.getResultingNode().getMetadata().get(language.toLocaleString(), "explanation").toString(), trace.getFinalVariableSnapshot(), situationDomain);
                explanation.getChildren().add(new Explanation(Explanation.Type.HINT, hintText));
            }
        }
        return result;
    }
}

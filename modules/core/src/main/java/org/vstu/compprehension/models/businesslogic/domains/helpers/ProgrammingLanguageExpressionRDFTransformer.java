package org.vstu.compprehension.models.businesslogic.domains.helpers;

import its.model.definition.ClassDef;
import its.model.definition.DomainModel;
import its.model.definition.ObjectDef;
import its.model.definition.loqi.DomainLoqiWriter;
import its.model.nodes.DecisionTree;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static its.model.definition.build.DomainBuilderUtils.*;

@Log4j2
public class ProgrammingLanguageExpressionRDFTransformer {

    private static final String DEBUG_DIR = "./modules/core/src/main/resources/" + ProgrammingLanguageExpressionDTDomain.DOMAIN_MODEL_LOCATION;
    private static final String BASE_TTL_PREF = "http://vstu.ru/poas/code#";

    public static void debugDumpLoqi(DomainModel model, String filename) {
        if(!ENABLE_DEBUG_SAVE) return;
        try {
            DomainLoqiWriter.saveDomain(
                model,
                new FileWriter(DEBUG_DIR + filename),
                new HashSet<>()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DomainModel questionToDomainModel(
        DomainModel domainModel,
        Map<String, DecisionTree> decisionTreeMap,
        Question question,
        List<ResponseEntity> responses
    ){
        return questionToDomainModel(domainModel, decisionTreeMap, question, responses, true).domainModel;
    }

    public record ParsedDomain(DomainModel domainModel, List<Integer> errorPos) {
    }

    public static ParsedDomain questionToDomainModel(
        DomainModel domainModel,
        Map<String, DecisionTree> decisionTreeMap,
        Question question,
        List<ResponseEntity> responses,
        boolean isLastSelectionVariable
    ) {
        Model base = ProgrammingLanguageExpressionDomain.factsToOntModel(
            question.getStatementFactsWithSchema().stream()
                .map(Fact::asBackendFact)
                .toList()
        );

        List<Resource> selected = responses.stream()
            .map((r) -> base.getResource(BASE_TTL_PREF + r.getLeftAnswerObject().getDomainInfo()))
            .toList();
        return questionToDomainModel(
            domainModel,
            decisionTreeMap,
            base,
            selected,
            isLastSelectionVariable
        );
    }

    private static ParsedDomain questionToDomainModel(
        DomainModel domainModel,
        Map<String, DecisionTree> decisionTreeMap,
        Model base,
        List<Resource> selectedTokens,
        boolean isLastSelectionVariable
    ){
        saveModel("base.ttl", base);

        DomainModel situationDomain = new DomainModel();
        ParsedDomain parseResult = new ParsedDomain(situationDomain, new ArrayList<>());

        Property indexProperty = base.getProperty(BASE_TTL_PREF + "index");
        List<Resource> baseTokens = base.listSubjectsWithProperty(indexProperty).toList().stream()
            .sorted(Comparator.comparingInt((a) -> a.getProperty(indexProperty).getInt()))
            .collect(Collectors.toList());

        Map<Resource, ObjectDef> baseTokensToTokens = new HashMap<>();
        Map<Resource, ObjectDef> baseTokensToElements = new HashMap<>();
        Map<Resource, Resource> complexPairsMap = getComplexPairsMap(baseTokens);

        for(Resource baseToken : baseTokens){
            createAndPutObjects(
                baseToken,
                domainModel,
                situationDomain,
                parseResult,
                baseTokensToTokens,
                baseTokensToElements,
                complexPairsMap
            );
        }

        for(Resource baseToken : baseTokens.subList(1, baseTokens.size())){
            int tokenIndex = baseTokens.indexOf(baseToken);
            ObjectDef resToken = baseTokensToTokens.get(baseToken);
            ObjectDef previousResToken = baseTokensToTokens.get(baseTokens.get(tokenIndex - 1));
            addRelationship(previousResToken, "directlyLeftOf", resToken.getName());
        }

        situationDomain.addMerge(domainModel);
        situationDomain.validateAndThrow();

        solveSituation(
            situationDomain,
            decisionTreeMap,
            selectedTokens,
            isLastSelectionVariable,
            baseTokensToElements,
            baseTokensToTokens
        );


        situationDomain.validateAndThrow();
        debugDumpLoqi(situationDomain, "out.loqi");
        return parseResult;
    }

    private static void createAndPutObjects(
        Resource baseToken,
        DomainModel domainModel,
        DomainModel situationDomain,
        ParsedDomain parseResult,
        Map<Resource, ObjectDef> baseTokensToTokens,
        Map<Resource, ObjectDef> baseTokensToElements,
        Map<Resource, Resource> complexPairsMap
    ) {
        if(baseTokensToTokens.containsKey(baseToken))
            return;

        ParsedClassName parsedClassName = className(baseToken, domainModel);
        String className = parsedClassName.className;
        if(!parsedClassName.isCorrect){
            parseResult.errorPos.add(getIndex(baseToken));
        }
        ObjectDef resElement = newObject(situationDomain, "element_" + baseToken.getLocalName(), className);
        setEnumProperty(
            resElement, "state",
            "state", className.equals("operand") ? "evaluated" : "unevaluated"
        );
        if(domainModel.getClasses().get(className).isSubclassOf("operand")) {
            boolean evaluationValue = getByProperty(baseToken, "has_value")
                .map(res -> res.asLiteral().getBoolean())
                .orElse(true);
            setBoolProperty(resElement, "evaluatesTo", evaluationValue);
        }
        ObjectDef resToken = resTokenFromBase(baseToken, resElement);

        Pair<String, String> loc = getLocalizedName(baseToken, className);
        addLocalizedName(resElement, loc);
        addLocalizedName(resToken, loc);

        baseTokensToTokens.put(baseToken, resToken);
        baseTokensToElements.put(baseToken, resElement);

        if(complexPairsMap.containsKey(baseToken)){
            Resource otherBaseToken = complexPairsMap.get(baseToken);
            ObjectDef otherResToken = resTokenFromBase(otherBaseToken, resElement);
            Pair<String, String> otherloc = getLocalizedName(otherBaseToken, className);
            addLocalizedName(otherResToken, otherloc);

            baseTokensToTokens.put(otherBaseToken, otherResToken);
        }
    }

    private static void solveSituation(
        DomainModel situationDomain,
        Map<String, DecisionTree> decisionTreeMap,
        List<Resource> selected,
        boolean isLastSelectionVariable,
        Map<Resource, ObjectDef> baseTokensToElements,
        Map<Resource, ObjectDef> baseTokensToTokens
    ) {
        ProgrammingLanguageExpressionsSolver solver = new ProgrammingLanguageExpressionsSolver();
        solver.solveTree(situationDomain, decisionTreeMap);
        solver.solveStrict(situationDomain, decisionTreeMap);


        for (Resource baseToken : selected) {
            //в зависимости от контекста, последний выбранный объект делаем переменной, и не записываем факт его вычисления
            if(isLastSelectionVariable && baseToken == selected.get(selected.size() - 1)){
                if(baseTokensToElements.containsKey(baseToken)){
                    newVariable(situationDomain, "X", baseTokensToElements.get(baseToken).getName());
                    newVariable(situationDomain, "X1", baseTokensToTokens.get(baseToken).getName());
                }
                continue;
            }

            ObjectDef operator = baseTokensToElements.get(baseToken);
            setEnumProperty(
                operator, "state",
                "state", "evaluated"
            );
            situationDomain.getObjects().stream()
                .filter(object -> object.isInstanceOf("operand") &&
                    !object.getRelationshipLinks().listByName("isOperandOf").isEmpty() &&
                    object.getRelationshipLinks().listByName("isOperandOf").stream()
                        .allMatch(link -> link.getObjects().get(0) == operator)
                )
                .forEach(operand -> {
                    setEnumProperty(
                        operand, "state",
                        "state", "used"
                    );
                });
        }
    }

    private static ObjectDef resTokenFromBase(Resource baseToken, ObjectDef resElement){
        DomainModel situation = resElement.getDomainModel();
        ObjectDef token = newObject(situation, "token_" + baseToken.getLocalName(), "token");
        addRelationship(resElement, "has", token.getName());
        addMeta(token, "index", getIndex(baseToken));
        return token;
    }

    private static Map<Resource, Resource> getComplexPairsMap(List<Resource> baseTokens){
        Map<Resource, Resource> map = new HashMap<>();
        for(int i = 0; i < baseTokens.size(); i++){
            Resource currentToken = baseTokens.get(i);
            Resource complexEnd = getOtherComplex(baseTokens, i);
            if(complexEnd != null){
                map.put(currentToken, complexEnd);
            }
        }
        return map;
    }

    private static Resource getOtherComplex(List<Resource> baseTokens, int index){
        Resource baseToken = baseTokens.get(index);
        int nesting = 0;
        for(int i = index; i < baseTokens.size(); i++){
            Resource currentToken = baseTokens.get(i);
            if(getText(baseToken).equals(getText(currentToken))){
                nesting+=1;
            }
            else if(isComplexEnd(baseToken, currentToken)){
                nesting-=1;
                if(nesting == 0){
                    return currentToken;
                }
            }
        }
        return null;
    }

    private static boolean isComplexEnd(Resource begin, Resource end){
        return getText(end).equals(Map.of(
            "[", "]",
            "(", ")",
            "?", ":",
            "if", "else"
        ).get(getText(begin)));
    }

    private static int getIndex(Resource baseToken){
        return baseToken.getProperty(baseToken.getModel().getProperty(BASE_TTL_PREF + "index")).getInt() - 1;
    }

    private static Optional<RDFNode> getByProperty(Resource baseRes, String propertyName){
        Property property = baseRes.getModel().getProperty(BASE_TTL_PREF + propertyName);
        if(!baseRes.hasProperty(property)){
            return Optional.empty();
        }
        return Optional.of(baseRes.getProperty(property).getObject());
    }

    private static void addLocalizedName(ObjectDef object, org.apache.commons.lang3.tuple.Pair<String, String> localization){
        addMeta(object, "RU", "localizedName", localization.getLeft());
        addMeta(object, "EN", "localizedName", localization.getRight());
    }

    private static org.apache.commons.lang3.tuple.Pair<String, String> getLocalizedName(Resource baseToken, String classname){
        String ru;
        String en;
        if(classname.equals("parenthesis")){
            ru = "скобки";
            en = "parenthesis";
        }
        else if(classname.equals("operand")){
            ru = "операнд " + getText(baseToken);
            en = "variable " + getText(baseToken);
        }
        else {
            ru = "оператор " + getText(baseToken);
            en = "operator " + getText(baseToken);
        }
        int pos = getIndex(baseToken) + 1;
        ru += " на позиции " + pos;
        en += " at position " + pos;
        return Pair.of(ru, en);
    }

    private static String getText(Resource baseToken){
        return baseToken.getProperty(baseToken.getModel().getProperty(BASE_TTL_PREF + "text")).getString();
    }

    private record ParsedClassName(String className, boolean isCorrect){
        ParsedClassName(String className){
            this(className, true);
        }
    }

    private static ParsedClassName className(Resource baseToken, DomainModel domainModel) {
        String text = getText(baseToken);

        List<ClassDef> possibleClasses = domainModel.getClasses().stream()
            .filter(classDef -> classDef.getMetadata().getEntries().stream()
                .anyMatch(metadata -> metadata.getPropertyName().contains("text") && text.equals(metadata.getValue()))
            )
            .toList();
        if(possibleClasses.isEmpty()){
            return new ParsedClassName("operand", text.matches("[a-zA-Z_$][a-zA-Z0-9_$]*"));
        }
        if(possibleClasses.size() == 1){
            return new ParsedClassName(possibleClasses.get(0).getName());
        }

        Property precedenceProperty = baseToken.getModel().getProperty(BASE_TTL_PREF + "precedence");
        int tokenPrecedence = baseToken.getProperty(precedenceProperty).getInt();
        return new ParsedClassName(
            possibleClasses.stream()
                .filter(classDef -> Integer.valueOf(tokenPrecedence).equals(classDef.getPropertyValue("precedence")))
                .findFirst()
                .map(ClassDef::getName)
                .orElseThrow()
        );
    }

    public static void saveModel(String filename, Model model){
        if(!ENABLE_DEBUG_SAVE) return;
        try {
            OutputStream out = new FileOutputStream(DEBUG_DIR + filename);
            model.write(out, "TTL");
            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final boolean ENABLE_DEBUG_SAVE = true;
}

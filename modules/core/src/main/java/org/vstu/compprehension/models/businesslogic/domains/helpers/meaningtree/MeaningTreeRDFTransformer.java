package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;


import its.model.DomainSolvingModel;
import its.model.definition.*;
import its.model.definition.loqi.DomainLoqiWriter;
import its.model.nodes.DecisionTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.ProgrammingLanguageExpressionsSolver;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.utils.tokens.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static its.model.definition.build.DomainBuilderUtils.*;


public class MeaningTreeRDFTransformer {
    private static final class EndToken extends Token {
        public EndToken() {
            super("student_end_evaluation", TokenType.SEPARATOR);
        }
    }

    private static final String DEBUG_DIR = "./modules/core/src/main/resources/" + ProgrammingLanguageExpressionDTDomain.DOMAIN_MODEL_LOCATION;
    private static final String BASE_TTL_PREF = "http://vstu.ru/poas/code#";

    public record ParsedDomain(DomainModel domainModel, List<Integer> errorPos) {
    }

    private record ParsedClassName(String className, boolean isCorrect){
        ParsedClassName(String className){
            this(className, true);
        }
    }


    public static TokenList tokenize(List<BackendFactEntity> facts, SupportedLanguage language) {
        Model m = MeaningTreeRDFHelper.backendFactsToModel(facts);
        MeaningTree mt = new RDFDeserializer().deserializeTree(m);
        try {
            return language.createTranslator(new MeaningTreeDefaultExpressionConfig()).getTokenizer().tokenizeExtended(mt.getRootNode());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("MeaningTree translator creation failed");
        }
    }

    public static SupportedLanguage detectLanguage(List<Tag> tags) {
        List<String> languages = SupportedLanguage.getMap().keySet().stream().map(SupportedLanguage::toString).toList();
        for (Tag tag : tags) {
            if (languages.contains(tag.getName().toLowerCase())) {
                return SupportedLanguage.fromString(tag.getName().toLowerCase());
            }
        }
        return SupportedLanguage.CPP;
    }

    public static DomainModel questionToDomainModel(DomainSolvingModel model, List<BackendFactEntity> facts,
                                                    List<ResponseEntity> responses, List<Tag> tags) {
        return questionToDomainModel(model, facts, responses, tags, true);
    }

    public static DomainModel questionToDomainModel(DomainSolvingModel model, List<BackendFactEntity> facts,
                                                    List<ResponseEntity> responses, List<Tag> tags, boolean setX) {
        Model base = MeaningTreeRDFHelper.backendFactsToModel(facts);
        Map<String, DecisionTree> decisionTreeMap = model.getDecisionTrees();
        SupportedLanguage language = detectLanguage(tags);
        TokenList tokens = tokenize(facts, language);
        TokenList selected = new TokenList(responses.stream()
                .map((r) -> {
                    String fullTokenName = base.getResource(BASE_TTL_PREF + r.getLeftAnswerObject().getDomainInfo()).getLocalName();
                    String[] tokenName = fullTokenName.split("_");
                    if (fullTokenName.equals("end_token") || fullTokenName.equals("student_end_evaluation")) {
                        return new EndToken();
                    }
                    return tokens.get(Integer.valueOf(tokenName[tokenName.length - 1]));
                })
                .toList());

        DomainModel situationDomain = new DomainModel();
        DomainModel domainModel = model.getMergedTagDomain(language.toString());
        ParsedDomain parseResult = new ParsedDomain(situationDomain, new ArrayList<>());

        Map<Token, ObjectDef> baseTokensToTokens = new HashMap<>();
        Map<Token, ObjectDef> baseTokensToElements = new HashMap<>();
        Map<Token, Token> complexPairsMap = getComplexPairsMap(tokens);

        for(int i = 0; i < tokens.size(); i++){
            createAndPutObjects(
                    tokens.get(i),
                    tokens,
                    domainModel,
                    situationDomain,
                    parseResult,
                    baseTokensToTokens,
                    baseTokensToElements,
                    complexPairsMap
            );
        }

        for(Token baseToken : tokens.subList(1, tokens.size())){
            int tokenIndex = tokens.indexOf(baseToken);
            ObjectDef resToken = baseTokensToTokens.get(baseToken);
            ObjectDef previousResToken = baseTokensToTokens.get(tokens.get(tokenIndex - 1));
            addRelationship(previousResToken, "directlyLeftOf", resToken.getName());
        }

        situationDomain.addMerge(domainModel);
        situationDomain.validateAndThrow();

        solveSituation(
                situationDomain,
                decisionTreeMap,
                tokens,
                selected,
                setX,
                baseTokensToElements,
                baseTokensToTokens
        );
        situationDomain.validateAndThrow();
        debugDumpLoqi(situationDomain, "out.loqi", domainModel);
        return parseResult.domainModel;
    }

    private static void createAndPutObjects(
            Token baseToken,
            TokenList tokens,
            DomainModel domainModel,
            DomainModel situationDomain,
            ParsedDomain parseResult,
            Map<Token, ObjectDef> baseTokensToTokens,
            Map<Token, ObjectDef> baseTokensToElements,
            Map<Token, Token> complexPairsMap
    ) {
        if(baseTokensToTokens.containsKey(baseToken))
            return;
        int tokenIndex = tokens.indexOf(baseToken);
        ParsedClassName parsedClassName = findClassName(domainModel, baseToken);
        String className = parsedClassName.className;
        if(!parsedClassName.isCorrect){
            parseResult.errorPos.add(tokenIndex);
        }
        ClassDef classDef = domainModel.getClasses().get(className);
        ObjectDef resElement = newObject(situationDomain, String.format("element_op_%d", tokenIndex), className);
        setEnumProperty(
                resElement, "state",
                "state", className.equals("operand") ? "evaluated" : "unevaluated"
        );
        if(domainModel.getClasses().get(className).isSubclassOf("operand")) {
            boolean evaluationValue = baseToken.getAssignedValue() != null && (boolean) baseToken.getAssignedValue();
            setBoolProperty(resElement, "evaluatesTo", evaluationValue);
        }
        ObjectDef resToken = resTokenFromBase(tokenIndex, resElement);

        Pair<String, String> loc = getLocalizedName(baseToken, complexPairsMap, tokenIndex, classDef);
        addLocalizedName(resElement, loc);
        addLocalizedName(resToken, loc);

        baseTokensToTokens.put(baseToken, resToken);
        baseTokensToElements.put(baseToken, resElement);

        if(complexPairsMap.containsKey(baseToken)){
            Token otherBaseToken = complexPairsMap.get(baseToken);
            int otherTokenIndex = tokens.indexOf(otherBaseToken);
            ObjectDef otherResToken = resTokenFromBase(otherTokenIndex, resElement);
            Pair<String, String> otherloc = getLocalizedName(otherBaseToken, complexPairsMap, otherTokenIndex, classDef);
            addLocalizedName(otherResToken, otherloc);

            baseTokensToTokens.put(otherBaseToken, otherResToken);
        }
    }

    private static ObjectDef resTokenFromBase(int tokenIndex, ObjectDef resElement){
        DomainModel situation = resElement.getDomainModel();
        ObjectDef token = newObject(situation, String.format("token_%d", tokenIndex), "token");
        addRelationship(resElement, "has", token.getName());
        addMeta(token, "index", tokenIndex);
        return token;
    }

    private static ParsedClassName findClassName(DomainModel domainModel, Token token) {
        if ((token.type == TokenType.COMMA && !(token instanceof OperatorToken) || token.type == TokenType.SEPARATOR)) {
            return new ParsedClassName("separator");
        } else if (token.type == TokenType.CAST) {
            return new ParsedClassName("operator_cast");
        } else if (token.type == TokenType.INITIALIZER_LIST_OPENING_BRACE ||
                token.type == TokenType.INITIALIZER_LIST_CLOSING_BRACE) {
            return new ParsedClassName("operator_list");
        } else if (token.type == TokenType.KEYWORD) {
            return new ParsedClassName("operand", true);
        } else if (token instanceof OperatorToken op && op.additionalOpType == OperatorType.NEW_ARRAY) {
            return new ParsedClassName("operator_newarray");
        }
        List<ClassDef> possibleClasses = domainModel.getClasses().stream()
                .filter(classDef -> classDef.getMetadata().getEntries().stream()
                        .anyMatch(metadata -> metadata.getPropertyName().contains("text") &&
                                token.value.equals(metadata.getValue()))
                )
                .toList();
        if(possibleClasses.isEmpty()){
            if (token.type == TokenType.CALL_OPENING_BRACE) {
                var method = domainModel.getClasses().stream().filter(classDef -> classDef.getName().contains("method_call")).findFirst();
                if (method.isPresent()) {
                    return new ParsedClassName(method.get().getName());
                }
            }
            return new ParsedClassName("operand", token.value.matches("[a-zA-Z_$][a-zA-Z0-9_$]*"));
        }
        if(possibleClasses.size() == 1 || !(token instanceof OperatorToken)){
            return new ParsedClassName(possibleClasses.getFirst().getName());
        }
        int tokenPrecedence = ((OperatorToken)token).precedence;
        return new ParsedClassName(
                possibleClasses.stream()
                        .filter(classDef -> classDef.isSubclassOf("operator") && Integer.valueOf(tokenPrecedence).equals(classDef.getPropertyValue("precedence", Map.of())))
                        .filter(classDef -> {
                            if (((OperatorToken)token).additionalOpType == OperatorType.METHOD_CALL) {
                                return classDef.getName().contains("method_call");
                            }
                            return true;
                        })
                        .findFirst()
                        .map(ClassDef::getName)
                        .orElseThrow()
        );
    }

    private static Map<Token, Token> getComplexPairsMap(TokenList baseTokens){
        Map<Token, Token> map = new HashMap<>();
        for(int i = 0; i < baseTokens.size(); i++){
            Token currentToken = baseTokens.get(i);
            int complexEnd = -1;
            if ((currentToken instanceof ComplexOperatorToken complexOperatorToken
                            && complexOperatorToken.isOpening())) {
                complexEnd = baseTokens.findClosingComplex(i);
            } else if (currentToken.type.isOpeningBrace() && currentToken.type.isOnlyGroupingBrace()) {
                // обычные скобки (не индексы или вызовы функций)
                int nesting = 1;
                for (int j = i + 1; j < baseTokens.size(); j++) {
                    if (baseTokens.get(j).type.isClosingBrace() && baseTokens.get(j).type.isOnlyGroupingBrace()) {
                        nesting--;
                        if (nesting == 0) {
                            complexEnd = j;
                            break;
                        }
                    } else if (baseTokens.get(j).type.isOpeningBrace() && baseTokens.get(j).type.isOnlyGroupingBrace()) {
                        nesting++;
                    }
                }
            }
            if (complexEnd != -1){
                map.put(currentToken, baseTokens.get(complexEnd));
            }
        }
        return map;
    }

    private static void addLocalizedName(ObjectDef object, org.apache.commons.lang3.tuple.Pair<String, String> localization){
        addMeta(object, "RU", "localizedName", localization.getLeft());
        addMeta(object, "EN", "localizedName", localization.getRight());
    }

    private static org.apache.commons.lang3.tuple.Pair<String, String> getLocalizedName(Token baseToken,
                                                                                        Map<Token, Token> complexPairs,
                                                                                        int tokenIndex,
                                                                                        ClassDef classdef) {
        String classname = classdef.getName();
        String baseTokens = baseToken.value;
        if (complexPairs.containsKey(baseToken)) {
            baseTokens = baseTokens.concat(" ");
            baseTokens = baseTokens.concat(complexPairs.get(baseToken).value);
        }
        String ru;
        String en;
        if (classname.equals("parenthesis")){
            ru = "скобки";
            en = "parenthesis";
        } else if (classname.equals("operand")) {
            ru = "операнд " + String.format("<code>%s</code>", baseTokens);
            en = "operand " + String.format("<code>%s</code>", baseTokens);
        }
        else if (classname.startsWith("operator")){
            if (classdef.getMetadata().get("EN", "localizedName")
                    .toString().endsWith(baseTokens)) {
                ru = "оператор " + String.format("<code>%s</code>", baseTokens);
                en = "operator " + String.format("<code>%s</code>", baseTokens);
            } else {
                ru = classdef.getMetadata().get("RU", "localizedName")
                        .toString();
                en = classdef.getMetadata().get("EN", "localizedName")
                        .toString();
            }
        } else {
            ru = String.format("<code>%s</code>", baseTokens);
            en = String.format("<code>%s</code>", baseTokens);
        }
        int pos = tokenIndex + 1;
        ru += " на позиции " + pos;
        en += " at position " + pos;
        return Pair.of(ru, en);
    }

    private static void solveSituation(
            DomainModel situationDomain,
            Map<String, DecisionTree> decisionTreeMap,
            TokenList allTokens,
            TokenList selected,
            boolean isLastSelectionVariable,
            Map<Token, ObjectDef> baseTokensToElements,
            Map<Token, ObjectDef> baseTokensToTokens
    ) {
        ProgrammingLanguageExpressionsSolver solver = new ProgrammingLanguageExpressionsSolver();
        appendTreeInfo(allTokens, baseTokensToElements, situationDomain);
        solver.solveStrict(situationDomain, decisionTreeMap);


        for (Token baseToken : selected) {
            //в зависимости от контекста, последний выбранный объект делаем переменной, и не записываем факт его вычисления
            if(isLastSelectionVariable && baseToken.equals(selected.getLast())){
                if(baseTokensToElements.containsKey(baseToken)) {
                    newVariable(situationDomain, "X", baseTokensToElements.get(baseToken).getName());
                    newVariable(situationDomain, "X1", baseTokensToTokens.get(baseToken).getName());
                }
                continue;
            }

            if (baseToken instanceof EndToken) {
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
                                    .allMatch(link -> link.getObjects().getFirst() == operator)
                    )
                    .forEach(operand -> {
                        setEnumProperty(
                                operand, "state",
                                "state", "used"
                        );
                    });
        }
    }

    private static void appendTreeInfo(TokenList tokens, Map<Token, ObjectDef> baseTokensToElements, DomainModel model) {
        for (Map.Entry<Token, ObjectDef> entry : baseTokensToElements.entrySet()) {
            if (entry.getKey() instanceof OperandToken op && op.operandOf() != null
                    && op.type != TokenType.SEPARATOR
                    && op.type != TokenType.CALLABLE_IDENTIFIER // чтобы не объединять имя функции и открывающую скобку
                    && op.type != TokenType.COMMA
                    && op.type != TokenType.OPENING_BRACE && op.type != TokenType.CLOSING_BRACE
                    && op.type != TokenType.COMPOUND_OPENING_BRACE && op.type != TokenType.COMPOUND_CLOSING_BRACE
                    && op.type != TokenType.UNKNOWN
                    && !(op.type.isBrace() && !(op instanceof OperatorToken))
                    ) {
                EnumDef positionEnum = model.getEnums().get("OperandPlacement");
                EnumValueRef pos = switch (op.operandPosition()) {
                    case LEFT -> positionEnum.getValues().get("left").getReference();
                    case CENTER -> positionEnum.getValues().get("center").getReference();
                    case RIGHT -> positionEnum.getValues().get("right").getReference();
                };
                entry.getValue().getRelationshipLinks().add(new RelationshipLinkStatement(
                        entry.getValue(),
                        "isOperandOf",
                        List.of(String.format("element_op_%d", tokens.indexOf(op.operandOf()))),
                        new NamedParamsValues(Map.of("placement", pos))
                ));
            }
        }
    }

    private static void debugDumpLoqi(DomainModel model, String filename, DomainModel toExclude) {
        if(!ENABLE_DEBUG_SAVE) return;
        if (toExclude != null) {
            model = model.copy();
            model.subtract(toExclude);
        }
        String filePath = new File(DEBUG_DIR).exists() ? DEBUG_DIR : "./";
        dumpModelLoqi(model, new File(filePath, filename));
    }

    public static void dumpModelLoqi(DomainModel model, File filePath) {
        try {
            DomainLoqiWriter.saveDomain(
                    model,
                    new FileWriter(filePath),
                    new HashSet<>()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Вернуть тег домена, который использует DomainModel и дерево решений (DecisionTree)
     * @return поддерживаемый тег домена DecisionTree
     */
    public static String getDecisionTreeDomainTag(SupportedLanguage language) {
        return language.toString().toLowerCase();
    }

    private static final boolean ENABLE_DEBUG_SAVE = true;
}

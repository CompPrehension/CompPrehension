package org.vstu.compprehension.models.businesslogic.domains.helpers;

import its.model.definition.Domain;
import its.model.definition.loqi.DomainLoqiWriter;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.definition.rdf.RDFUtils;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.data.util.Pair;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ProgrammingLanguageExpressionRDFTransformer {

    private static final String DEBUG_DIR = "C:\\Uni\\CompPrehension_mainDir\\inputs\\input_examples\\";
    private static void debugDumpLoqi(its.model.definition.Domain model, String filename){
        try {
            DomainLoqiWriter.saveDomain(
                model,
                new FileWriter(DEBUG_DIR + filename),
                new HashSet<>()
            );
        } catch (IOException e) {
            log.debug("Could not save " + filename);
        }
    }

    public static its.model.definition.Domain questionToDomainModel(
        its.model.definition.Domain commonDomainModel,
        QuestionEntity question,
        InteractionEntity lastQuestionInteraction
    ){
        return questionToDomainModel(
            commonDomainModel,
            question.getStatementFacts(),
            lastQuestionInteraction.getResponses()
        );
    }

    public static its.model.definition.Domain questionToDomainModel(
        its.model.definition.Domain commonDomainModel,
        List<BackendFactEntity> questionFacts,
        List<ResponseEntity> responses
    ){
        Model m = questionToModel(questionFacts, responses);
        its.model.definition.Domain situationModel = commonDomainModel.getDomain().copy();
        DomainRDFFiller.fillDomain(
            situationModel,
            m,
            Collections.singleton(DomainRDFFiller.Option.NARY_RELATIONSHIPS_OLD_COMPAT),
            null
        );
        situationModel.validateAndThrowInvalid();

//        val dumpModel = situationModel.copy();
//        dumpModel.subtract(commonDomainModel);
//        debugDumpLoqi(dumpModel, "out.loqi");
        return situationModel;
    }

    public static Model questionToModel(
        List<BackendFactEntity> questionFacts,
        List<ResponseEntity> responses
    ){
        Model base = ProgrammingLanguageExpressionDomain.factsToOntModel(questionFacts);
        List<Resource> selected = responses.stream()
            .map((r) -> base.getResource("http://vstu.ru/poas/code#" + r.getLeftAnswerObject().getDomainInfo()))
            .collect(Collectors.toList());
    
        saveModel("base.ttl", base);
        Model res = ModelFactory.createDefaultModel();
        res.setNsPrefix("", RDFUtils.POAS_PREF);
        Property indexProperty = base.getProperty("http://vstu.ru/poas/code#index");
        Property typeProperty = res.getProperty(RDFUtils.RDF_PREF + "type");
        Property leftOfProperty = res.getProperty(RDFUtils.POAS_PREF + "directlyLeftOf");
        Property stateProperty = res.getProperty(RDFUtils.POAS_PREF + "state");
        Property varProperty = res.getProperty(RDFUtils.POAS_PREF + "var...");
        Property ruProperty = res.getProperty(RDFUtils.POAS_PREF + "RU_localizedName");
        Property enProperty = res.getProperty(RDFUtils.POAS_PREF + "EN_localizedName");
        List<Resource> baseTokens = base.listSubjectsWithProperty(indexProperty).toList().stream()
                .sorted(Comparator.comparingInt((a) -> a.getProperty(indexProperty).getInt()))
                .collect(Collectors.toList());
        Map<Resource, Resource> baseTokensToTokens = new HashMap<>();
        Map<Resource, Resource> baseTokensToElements = new HashMap<>();
        for(Resource baseToken : baseTokens){
            if(baseTokensToTokens.containsKey(baseToken))
                continue;
            Resource resElement = getResource(res,"element_" + baseToken.getLocalName());
            resElement.addProperty(typeProperty, getClassResource(baseToken, res));
            resElement.addProperty(stateProperty, getResource(res, className(baseToken).equals("operand")? "evaluated" : "unevaluated"));
            Resource resToken = resTokenFromBase(baseToken, resElement);
    
            Pair<String, String> loc = getLocalizedName(baseToken, baseTokens.indexOf(baseToken)+1);
            resElement.addProperty(ruProperty, loc.getFirst());
            resElement.addProperty(enProperty, loc.getSecond());
            resToken.addProperty(ruProperty, loc.getFirst());
            resToken.addProperty(enProperty, loc.getSecond());
    
            baseTokensToTokens.put(baseToken, resToken);
            baseTokensToElements.put(baseToken, resElement);
            
            if(getOtherComplex(baseToken) != null){
                Resource otherBaseToken = getOtherComplex(baseToken);
                Resource otherResToken = resTokenFromBase(otherBaseToken, resElement);
                Pair<String, String> otherloc = getLocalizedName(otherBaseToken, baseTokens.indexOf(otherBaseToken)+1);
                otherResToken.addProperty(ruProperty, otherloc.getFirst());
                otherResToken.addProperty(enProperty, otherloc.getSecond());
                
                baseTokensToTokens.put(otherBaseToken, otherResToken);
            }
            
        }
        
        for(Resource baseToken : baseTokens.subList(1, baseTokens.size())){
            int tokenIndex = baseTokens.indexOf(baseToken);
            Resource resToken = baseTokensToTokens.get(baseToken);
            Resource previousResToken = baseTokensToTokens.get(baseTokens.get(tokenIndex - 1));
            previousResToken.addProperty(leftOfProperty, resToken);
        }

        if(!selected.isEmpty()) {
            for (Resource baseToken : selected.subList(0, selected.size() - 1)) {
                baseTokensToElements.get(baseToken).removeAll(stateProperty);
                baseTokensToElements.get(baseToken).addProperty(stateProperty, getResource(res, "evaluated"));
                baseToken.listProperties(base.getProperty("http://vstu.ru/poas/code#ast_edge")).toList().stream()
                    .map(s -> s.getObject().asResource())
                    .filter(resource -> !resource.equals(getOtherComplex(baseToken)))
                    .map(baseTokensToElements::get)
                    .forEach(operand -> {
                        operand.removeAll(stateProperty);
                        operand.addProperty(stateProperty, getResource(res, "used"));
                    });
            }
            Resource currentlyChosenRes = selected.get(selected.size() - 1);
            if(baseTokensToElements.containsKey(currentlyChosenRes)){
                baseTokensToElements.get(currentlyChosenRes).addProperty(varProperty, "X");
                baseTokensToTokens.get(currentlyChosenRes).addProperty(varProperty, "X1");
            }
        }
        saveModel("res.ttl", res);
        return res;
    }
    
    private static Resource resTokenFromBase(Resource baseToken, Resource resElement){
        Model res = resElement.getModel();
        Property typeProperty = res.getProperty(RDFUtils.RDF_PREF + "type");
//        Property belongsProperty = res.getProperty(RDFUtils.POAS_PREF + "belongsTo");
        Property hasProperty = res.getProperty(RDFUtils.POAS_PREF + "has");
        
        Resource resToken = getResource(res, "token_" + baseToken.getLocalName());
        resToken.addProperty(typeProperty, getResource(res,"token"));
//        resToken.addProperty(belongsProperty, resElement);
        resElement.addProperty(hasProperty, resToken);
        return resToken;
    }
    
    private static Resource getOtherComplex(Resource baseToken){
        Property p = baseToken.getModel().getProperty("http://vstu.ru/poas/code#has_complex_operator_part");
        return baseToken.hasProperty(p) ? baseToken.getProperty(p).getObject().asResource() : null;
    }
    
    private static Pair<String, String> getLocalizedName(Resource baseToken, int index){
        String classname = className(baseToken);
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
        ru += " на позиции " + index;
        en += " at position " + index;
        return Pair.of(ru, en);
    }

    private static String getText(Resource baseToken){
        return baseToken.getProperty(baseToken.getModel().getProperty("http://vstu.ru/poas/code#text")).getString();
    }

    private static String className(Resource baseToken){
        String classname;
        String text = getText(baseToken);
        Property arityProperty = baseToken.getModel().getProperty("http://vstu.ru/poas/code#arity");
        Property placeProperty = baseToken.getModel().getProperty("http://vstu.ru/poas/code#prefix_postfix");
        Property isFunctionCallProperty = baseToken.getModel().getProperty("http://vstu.ru/poas/code#is_function_call");

        if(text.equals("(") || text.equals(")"))
            classname = /*Objects.equals(baseToken.getProperty(isFunctionCallProperty).getString(), "true") ? "function_call" :*/ "parenthesis";
        else if(text.equals("[") || text.equals("]")) classname = "brackets";
        else if(text.equals("&&") ) classname = "and";
        else if(text.equals("||") ) classname = "or";
        else if(text.equals("!") ) classname = "not";
        else if(text.equals("==") ) classname = "equal";
        else if(text.equals("!=") ) classname = "notequal";
        else if(text.equals("<") ) classname = "less";
        else if(text.equals("<=") ) classname = "lesseq";
        else if(text.equals(">") ) classname = "greater";
        else if(text.equals(">=") ) classname = "greatereq";
        else if(text.equals("+") ) classname = baseToken.getProperty(arityProperty).getString().equals("binary") ? "plus" : "unaryPlus";
        else if(text.equals("-") ) classname = baseToken.getProperty(arityProperty).getString().equals("binary") ? "minus" : "unaryMinus";
        else if(text.equals("++") ) classname = baseToken.getProperty(placeProperty).getString().equals("prefix") ? "prefixInc" : "postfixInc";
        else if(text.equals("--") ) classname = baseToken.getProperty(placeProperty).getString().equals("prefix") ? "prefixDec" : "postfixDec";
        else if(text.equals("*") ) classname = baseToken.getProperty(arityProperty).getString().equals("binary") ? "multiplication" : "operator_unary_*";
        else if(text.equals("/") ) classname = "division";
        else if(text.equals("?") || text.equals(":")) classname = "ternaryConditional";
        else if(text.equals(",") ) classname = "comma";
        else if(text.equals("^") ) classname = "power";

        // новое от 30.06.2024 //
        else if(text.equals("+=") ) classname = "operator_+=";
        else if(text.equals("%") ) classname = "operator_%";
        else if(text.equals("%=") ) classname = "operator_%=";
        else if(text.equals("&") ) classname = baseToken.getProperty(arityProperty).getString().equals("binary") ? "operator_&" : "operator_unary_&";
        else if(text.equals("&=") ) classname = "operator_&=";
        /*else if(text.equals("(") ) classname = "__parentheses";*/
        else if(text.equals("*=") ) classname = "operator_*=";
        else if(text.equals("->") ) classname = "operator_pointer_field";
        else if(text.equals(".") ) classname = "operator_.";
        else if(text.equals("::") ) classname = "operator_::";
        else if(text.equals("<<") ) classname = "operator_left_shift";
        else if(text.equals("<<=") ) classname = "operator_left_shift_assign";
        else if(text.equals("=") ) classname = "operator_=";
        else if(text.equals(">>") ) classname = "operator_right_shift";
        else if(text.equals(">>=") ) classname = "operator_right_shift_assign";
        else if(text.equals("^=") ) classname = "operator_xor_assign";
        else if(text.equals("|") ) classname = "operator_bitwise_or";
        else if(text.equals("|=") ) classname = "operator_bitwise_or_assign";
        else if(text.equals("~") ) classname = "operator_~";
        else if(text.equals("sizeof") ) classname = "operator_sizeof";
        else if(text.equals("co_await") ) classname = "operator_co_await";
        else if(text.equals("new") ) classname = "operator_new";
        else if(text.equals("delete") ) classname = "operator_delete";
        else if(text.equals("<=>") ) classname = "operator_spaceship";
        else if(text.equals("throw") ) classname = "operator_throw";
        else if(text.equals("co_yield") ) classname = "operator_co_yield";
        // The default.
        else classname = "operand";

        return classname;
    }
    
    private static Resource getClassResource(Resource baseToken, Model res){
        return getResource(res, className(baseToken));
    }

    private static Resource getResource(Model model, String localName){
        return model.getResource(RDFUtils.POAS_PREF + localName);
    }
    
    private static void saveModel(String filename, Model model){
        if(true) return;
        log.info("saving {}", filename);
        OutputStream out = null;
        try {
            out = new FileOutputStream(DEBUG_DIR + filename);
            model.write(out, "TTL");
            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package org.vstu.compprehension.models.businesslogic.domains.helpers;

import its.reasoner.util.JenaUtil;
import its.reasoner.util.RDFUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.data.util.Pair;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class ProgrammingLanguageExpressionRDFTransformer {
    public static Model questionToModel(QuestionEntity question, InteractionEntity lastQuestionInteraction){
        Model base = ProgrammingLanguageExpressionDomain.factsToOntModel(question.getStatementFacts());
        List<Resource> selected = lastQuestionInteraction.getResponses().stream().map((r) -> base.getResource("http://vstu.ru/poas/code#" + r.getLeftAnswerObject().getDomainInfo())).collect(Collectors.toList());
    
        //saveModel("base.ttl", base);
        Model res = ModelFactory.createDefaultModel();
        res.setNsPrefix("", JenaUtil.POAS_PREF);
        Property indexProperty = base.getProperty("http://vstu.ru/poas/code#index");
        Property typeProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.RDF_PREF, "type"));
        Property leftOfProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "directlyLeftOf"));
        Property stateProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "state"));
        Property varProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "var..."));
        Property ruProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "RU_localizedName"));
        Property enProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "EN_localizedName"));
        List<Resource> baseTokens = base.listSubjectsWithProperty(indexProperty).toList().stream()
                .sorted(Comparator.comparingInt((a) -> a.getProperty(indexProperty).getInt()))
                .collect(Collectors.toList());
        Map<Resource, Resource> baseTokensToTokens = new HashMap<>();
        Map<Resource, Resource> baseTokensToElements = new HashMap<>();
        for(Resource baseToken : baseTokens){
            if(baseTokensToTokens.containsKey(baseToken))
                continue;
            Resource resElement = RDFUtil.resource(res,"element_" + baseToken.getLocalName());
            resElement.addProperty(typeProperty, getClassResource(baseToken, res));
            resElement.addProperty(stateProperty, RDFUtil.resource(res, className(baseToken).equals("operand")? "evaluated" : "unevaluated"));
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
        
        for(Resource baseToken : selected.subList(0, selected.size()-1)){
            baseTokensToElements.get(baseToken).removeAll(stateProperty);
            baseTokensToElements.get(baseToken).addProperty(stateProperty, RDFUtil.resource(res, "evaluated"));
            baseToken.listProperties(base.getProperty("http://vstu.ru/poas/code#ast_edge")).toList()
                    .forEach((s) -> {
                        Resource el = baseTokensToElements.get(s.getObject().asResource());
                        el.removeAll(stateProperty);
                        el.addProperty(stateProperty, RDFUtil.resource(res, "used"));
                    });
        }
        baseTokensToElements.get(selected.get(selected.size() -1)).addProperty(varProperty, "X");
        baseTokensToTokens.get(selected.get(selected.size() -1)).addProperty(varProperty, "X1");
        //saveModel("res.ttl", res);
        return res;
    }
    
    private static Resource resTokenFromBase(Resource baseToken, Resource resElement){
        Model res = resElement.getModel();
        Property typeProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.RDF_PREF, "type"));
        Property belongsProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "belongsTo"));
        Property hasProperty = res.getProperty(JenaUtil.INSTANCE.genLink(JenaUtil.POAS_PREF, "has"));
        
        Resource resToken = RDFUtil.resource(res, "token_" + baseToken.getLocalName());
        resToken.addProperty(typeProperty, RDFUtil.resource(res,"token"));
        resToken.addProperty(belongsProperty, resElement);
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
        if(text.equals("(") || text.equals(")")) classname = "parenthesis";
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
        else if(text.equals("*") ) classname = "multiplication";
        else if(text.equals("/") ) classname = "division";
        else if(text.equals("?") || text.equals(":")) classname = "ternaryConditional";
        else if(text.equals(",") ) classname = "comma";
        else if(text.equals("^") ) classname = "power";
        else classname = "operand";
        
        return classname;
    }
    
    private static Resource getClassResource(Resource baseToken, Model res){
        return RDFUtil.resource(res, className(baseToken));
    }
    
    private static void saveModel(String filename, Model model){
        log.info("saving {}", filename);
        OutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            model.write(out, "TTL");
            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

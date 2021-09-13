package org.vstu.compprehension.models.businesslogic.backend;


import lombok.extern.log4j.Log4j2;
import org.apache.jena.vocabulary.*;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.domains.helpers.FactsGraph;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.businesslogic.LawFormulation;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.PrintUtil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Primary
@Component @RequestScope
@Log4j2
public class JenaBackend extends Backend {

    static String BACKEND_TYPE = "Jena";

    String baseIRIPrefix;
    OntModel model;
    ArrayList<Rule> domainRules = new ArrayList<>();

/*
    String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
*/
    // use also    model.getNsPrefixURI("owl")   to get a standard prefix


    public void createOntology() {
        createOntology("http://vstu.ru/poas/code");
        //// createOntology("http://www.test/test.owl");
    }
    public void createOntology(String base) {

        baseIRIPrefix = base + "#";
        PrintUtil.registerPrefix("my", baseIRIPrefix); //?
        PrintUtil.registerPrefix("skos", "http://www.w3.org/2004/02/skos/core"); //?

        model = ModelFactory.createOntologyModel(OWL_MEM);  // createDefaultModel();
        model.setNsPrefix("my", base);
        model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        //                                  http://www.w3.org/2004/02/skos/core#Concept

        // polyfill some RDF/OWL entries
        model.createObjectProperty(RDF.type.getURI());  // already here by default?
        model.createObjectProperty(RDFS.subClassOf.getURI());  // already here by default?
        model.createObjectProperty(RDFS.subPropertyOf.getURI());  // already here by default?
//        model.createProperty(RDF.Property.getURI());
        model.createProperty(OWL.DatatypeProperty.getURI());
        model.createProperty(OWL.ObjectProperty.getURI());
        model.createProperty(OWL.FunctionalProperty.getURI());
        model.createProperty(OWL.InverseFunctionalProperty.getURI());
        model.createProperty(OWL.TransitiveProperty.getURI());
        model.createClass(OWL.Class.getURI());  // already here by default?

        domainRules.clear();
    }

    public OntModel getModel() {
        return model;
    }


//    static void runReasoning(String in_rdf_url, String rules_path, String out_rdf_path) {
//        // Register a namespace for use in the rules
////        String baseURI = "http://vstu.ru/poas/ctrl_structs_2020-05_v1#";
////        String baseURI = "http://penskoy.n/expressions1#
//        String baseURI = findIriForPrefix(rules_path, "my");
//        PrintUtil.registerPrefix("my", baseURI);
//
//        List<Rule> rules = Rule.rulesFromURL(rules_path);
//        Model data = RDFDataMgr.loadModel(in_rdf_url);
//
//        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
////        reasoner.setOWLTranslation(true);               // not needed in RDFS case
////        reasoner.setTransitiveClosureCaching(true);     // not required when there is no use of transitivity
//
//        long startTime = System.nanoTime();
//
//        InfModel inf = ModelFactory.createInfModel(reasoner, data);
//        inf.prepare();
//
//        long estimatedTime = System.nanoTime() - startTime;
//        System.out.println("Time spent on reasoning: " + String.valueOf((float) (estimatedTime / 1000 / 1000) / 1000) + " seconds.");
//
//        FileOutputStream out = null;
//        try {
//            out = new FileOutputStream(out_rdf_path);
//            RDFDataMgr.write(out, inf, Lang.NTRIPLES);  // Lang.NTRIPLES  or  Lang.RDFXML
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            System.out.println("Cannot write to file: " + out_rdf_path);
//        }
//    }

    public OntClass getThingClass() {
        return model.createClass(OWL.Thing.getURI());
    }

    Resource addOWLLawFormulation(String name, String type) {
//        // debug
//        System.out.println("addOWLLawFormulation( name: " + name + ", type: " + type + " )");

        switch (type) {
            case "owl:NamedIndividual":
                return model.createIndividual(termToUri(name), getThingClass());
            case "owl:ObjectProperty":
                return model.createObjectProperty(termToUri(name));
            case "owl:TransitiveProperty":
                return model.createTransitiveProperty(termToUri(name));
            case "owl:FunctionalProperty":
                return model.createProperty(termToUri(name));
            case "owl:InverseFunctionalProperty":
                return model.createInverseFunctionalProperty(termToUri(name));
            case "owl:DatatypeProperty":
                return model.createDatatypeProperty(termToUri(name));
            case "owl:Class":
                return model.createClass(termToUri(name));
            case "":
                // empty type - create ordinal resource
                return model.createResource(termToUri(name));
            default:
                throw new UnsupportedOperationException("JenaBackend.addOWLLawFormulation: unknown type: " + type);
        }
    }

    void addLaw(Law law) {
        assert law != null;

        for (LawFormulation lawFormulation : law.getFormulations()) {
//        /// debug
//            System.out.println("addLaw() : " + lawFormulation.getName());

            if (lawFormulation.getBackend().equals("OWL")) {
                addOWLLawFormulation(lawFormulation.getName(), lawFormulation.getFormulation());
            } else if (lawFormulation.getBackend().equals(BACKEND_TYPE)) {
                try {
                    domainRules.add(Rule.parseRule(lawFormulation.getFormulation()));
                } catch (Rule.ParserException e) {
                    log.error("Following error in rule: " + lawFormulation.getFormulation(), e);
                }
            } else if ("can_covert" != null) {

                // TODO: convert rules if possible ?
            }
        }
    }

    void addStatementFact(BackendFactEntity fact) {
        assert fact != null;

        String subj = fact.getSubject();
        String prop = fact.getVerb();
        String obj = fact.getObject();
        String objType = fact.getObjectType();
        assert prop != null;
        assert objType != null;

        /// debug
//        System.out.println("addStatementFact( subj: " + subj + ", prop: " + prop + ", obj: " + obj + " )");

        // TODO: save this info somehow?
        if (prop.startsWith("not-for-reasoner:") || objType.equals("List<boolean>")) {
            // ignore this fact (as it's not for the reasoner)
            return;
        }

        OntResource ind = addOWLLawFormulation(subj, fact.getSubjectType()).as(OntResource.class);
//        if (fact.getSubjectType().equals("owl:NamedIndividual")) {
//            ind = model.createIndividual(termToUri(subj), getThingClass());
//        } else if (fact.getSubjectType().equals("owl:Class")) {
//            ind = model.createClass(termToUri(subj));
//        } else {
//            ind = model.createOntResource(termToUri(subj));
//        }
        assert ind != null;

        if (objType.startsWith("xsd:")) {
            // datatype relation
            switch (objType) {
                case "xsd:int":
                case "xsd:integer": {
                    DatatypeProperty p = model.createDatatypeProperty(termToUri(prop));
                    model.addLiteral(ind, p, Integer.parseInt(obj));
                    break;
                }
                case "xsd:string": {
                    DatatypeProperty p = model.createDatatypeProperty(termToUri(prop));
                    model.add(ind, p, obj);
                    break;
                }
                case "xsd:boolean": {
                    DatatypeProperty p = model.createDatatypeProperty(termToUri(prop));
                    model.addLiteral(ind, p, Boolean.parseBoolean(obj));
                    break;
                }
                case "xsd:double": {
                    DatatypeProperty p = model.createDatatypeProperty(termToUri(prop));
                    model.addLiteral(ind, p, Double.parseDouble(obj));
                    break;
                }
                case "xsd:float": {
                    DatatypeProperty p = model.createDatatypeProperty(termToUri(prop));
                    model.addLiteral(ind, p, Float.parseFloat(obj));
                    break;
                }
                default:
                    throw new UnsupportedOperationException("JenaBackend.addStatementFact(): unknown datatype in objectType: " + objType);
            }
        } else {
            // object relation
            if (prop.equals("rdf:type")) {
                try {
                    addOWLLawFormulation(fact.getSubject(), fact.getObject());
                    return;
                } catch (UnsupportedOperationException exception) {
                    // continue if not a basic OWL assertion
                }
            }
            if (objType.equals("owl:NamedIndividual")) {
                ObjectProperty p = model.createObjectProperty(termToUri(prop));
                model.add(ind, p, model.createIndividual(termToUri(obj), OWL.Thing));

            } else if (objType.equals("owl:Class") && prop.equals("rdf:type")) {
                    model.createIndividual(ind.getURI(), model.createClass(termToUri(obj)));
            } else {
                // common case
                Property p = model.createProperty((termToUri(prop)));
                model.add(ind, p, model.createOntResource(termToUri(obj)));
            }
        }
    }


    private void callReasoner() {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(domainRules);

        long startTime = System.nanoTime();

        InfModel inf = ModelFactory.createInfModel(reasoner, model);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
        // print time report. TODO: remove the print
        log.info("Time Jena spent on reasoning: " + String.format("%.5f", (float)estimatedTime / 1000 / 1000 / 1000) + " seconds.");

        // use the inferred results (inf) ...
        model.add( inf );
    }

    private String convertDatatype(String jenaType) {
        String[] typeParts = jenaType.split("[.\\]]");
        return "xsd:" + typeParts[typeParts.length-1].toLowerCase();
    }

    /** Expand simple name as local, prefixed name as special */
    private String termToUri(String s) {
        String uri;
        assertNotNull(s, "termToUri(null) !");
        if (s.startsWith("http://")) {
            uri = s;
        }
        else {
            uri = model.expandPrefix(s);
            if (uri.equals(s)) {
                // not a standard name
                uri = baseIRIPrefix + s;
            }
        }

        ///
        // if (s.contains(":"))
        //     System.out.println("termToUri: " + s + " -> " + uri);
        ///
        return uri;
    }

    /** Returns local name or prefixed special name */
    private String uriToTerm(String uri) {
        if (uri == null) {
            log.debug("uriToTerm(): Encountered NULL uri! Defaulting to '[]'");
            return "[]";
        }

        String s = uri.replace(baseIRIPrefix, "");
        if (s.equals(uri)) {
            // not a local name
            s = model.qnameFor(uri);
            if (s == null) {
                // no qName available
                s = uri;
            }
        }

        ///
        // System.out.println("uriToTerm: " + uri + "\n -> " + s);
        ///
        return s;
    }

    private List<BackendFactEntity> getPropertyRelations(Property property) {
        List<BackendFactEntity> facts = new ArrayList<>();

        boolean isObjectProp = property instanceof ObjectProperty;

        String objType = isObjectProp ? "owl:NamedIndividual" : null;
        String subjType = null;
        String propName = uriToTerm(property.getURI());

        StmtIterator it = model.listStatements(null, property, (RDFNode) null);
        while (it.hasNext()) {
            Statement stmt = it.next();

            Resource subjResource = stmt.getSubject().asResource();
            if (subjResource.hasProperty(RDF.type, OWL.Class))
                subjType = "owl:Class";
            else if (subjResource.hasProperty(RDF.type, RDF.Property))
                subjType = "rdf:Property";
            else
                subjType = "owl:NamedIndividual";


            RDFNode objNode = stmt.getObject();
            String obj;

            if (isObjectProp) {
                Resource resource = objNode.asResource();
                String URI = resource.getURI();
                if (URI != null) {
                    obj = uriToTerm(URI);
                } else { /* resource is bnode*/
                    obj = resource.toString();
                }

                if (resource.hasProperty(RDF.type, OWL.Class))
                    objType = "owl:Class";
                else if (resource.hasProperty(RDF.type, RDF.Property))
                    objType = "rdf:Property";
                else
                    objType = "owl:NamedIndividual";

            } else {
                if (objType == null) {
                    RDFDatatype dt = objNode.asLiteral().getDatatype();
                    objType = convertDatatype(dt.toString());
                }
                obj = objNode.asLiteral().getLexicalForm();
            }

            String subjURI = subjResource.getURI();
            String subj;
            if (subjURI != null) {
                subj = uriToTerm(subjURI);
            } else { /* subj is bnode*/
                subj = subjResource.toString();
            }

            facts.add(new BackendFactEntity(
                    subjType,
                    subj,
                    propName,
                    objType,
                    obj
            ));
        }
        return facts;
    }

    public List<BackendFactEntity> getFacts(List<String> verbs) {

        List<BackendFactEntity> result = new ArrayList<>();

        for (String verb : verbs) {

            // find property by verb name
            OntProperty p;

            String uri = termToUri(verb);
            OntResource resource = model.getOntResource(uri);
            if (resource == null) {
                log.warn("JenaBack.getFacts() WARNING: Cannot find resource for verb: " + verb);
                continue;
            }
            try {
                p = resource.asProperty();
            } catch (ConversionException exception) {
                log.warn("JenaBack.getFacts() WARNING: Cannot find property for verb: " + verb);
                continue;
            }

            ///
//            System.out.println("get relations of prop: " + p.getURI());
            ///
            List<BackendFactEntity> verbFacts = getPropertyRelations(p);
            result.addAll(verbFacts);
        }
        return result;
    }

    private void debug_dump_model(String name) {
        if (false) {
            String out_rdf_path = "c:/temp/" + name + ".n3";
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(out_rdf_path);
                RDFDataMgr.write(out, model, Lang.NTRIPLES);  // Lang.NTRIPLES  or  Lang.RDFXML
                log.debug("Debug written: " + out_rdf_path + ". N of of triples: " + model.size());
            } catch (FileNotFoundException e) {
                log.error("Cannot write to file: " + out_rdf_path, e);
            }
        }
    }

    @Override
    public List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs) {
        createOntology();
        for (Law law : laws) {
            addLaw(law);
        }

        addFacts(statement);

        debug_dump_model("solve");

        callReasoner();

        debug_dump_model("solved");

        return new FactsGraph(getFacts(solutionVerbs)).removeDuplicates().getFacts();
    }

    public void addFacts(List<BackendFactEntity> facts) {
        for (BackendFactEntity fact : facts) {
            addStatementFact(fact);
        }
    }

    @Override
    public List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs) {
        createOntology();

        for (Law law : laws) {
            addLaw(law);
        }

        addFacts(statement);
        addFacts(response);
        addFacts(correctAnswer);

        debug_dump_model("judge");

        callReasoner();

        debug_dump_model("judged");

        return new FactsGraph(getFacts(violationVerbs)).removeDuplicates().getFacts();
    }


    static void jenaBasicExample() {
        // some definitions
        String personURI    = "http://somewhere/JohnSmith";
        String fullName     = "John Smith";

        // create an empty Model
        Model model = ModelFactory.createDefaultModel();

        // create the resource
        Resource johnSmith = model.createResource(personURI);

        // add the property
        johnSmith.addProperty(VCARD.FN, fullName);
    }

    static void JenaBackendSmokeTest() {
        // Make sure that environment setup is OK
        JenaBackend b = new JenaBackend();
        b.createOntology();
        b.getFacts(new ArrayList<>());
        String uri = b.model.expandPrefix("type");
    }

    public static void main(String ... args) {
        // Make sure that environment setup is OK
//        jenaBasicExample();
//        JenaBackendSmokeTest();
    }

}

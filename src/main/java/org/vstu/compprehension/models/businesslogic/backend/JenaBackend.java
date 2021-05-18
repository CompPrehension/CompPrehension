package org.vstu.compprehension.models.businesslogic.backend;


import org.apache.jena.vocabulary.RDF;
import org.vstu.compprehension.models.businesslogic.Law;
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
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.VCARD;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;


@Primary
@Component
public class JenaBackend extends Backend {

    static String BACKEND_TYPE = "Jena";

    String baseIRIPrefix;
    OntModel model;
    ArrayList<Rule> domainRules = new ArrayList<>();

    String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    // use also    model.getNsPrefixURI("owl")   and so to get a standard prefix


    void createOntology() {
        createOntology("http://www.test/test.owl");
    }
    void createOntology(String base) {

        baseIRIPrefix = base + "#";
        PrintUtil.registerPrefix("my", baseIRIPrefix); //?

        model = ModelFactory.createOntologyModel(OWL_MEM);  // createDefaultModel();

        domainRules.clear();
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
        return model.createClass(model.getNsPrefixURI("owl") + "Thing");
    }

    void addOWLLawFormulation(String name, String type) {
//        // debug
//        System.out.println("addOWLLawFormulation( name: " + name + ", type: " + type + " )");

        if (type.equals("owl:NamedIndividual")) {
            model.createIndividual(baseIRIPrefix + name, getThingClass());
        } else if (type.equals("owl:ObjectProperty")) {
            model.createObjectProperty(baseIRIPrefix + name);
        } else if (type.equals("owl:DatatypeProperty")) {
            model.createDatatypeProperty(baseIRIPrefix + name);
        } else if (type.equals("owl:Class")) {
            model.createClass(baseIRIPrefix + name);
        } else {
            throw new UnsupportedOperationException("JenaBackend.addOWLLawFormulations: unknown type: " + type);
        }
    }

    void addLaw(Law law) {
        assert law != null;
        for (LawFormulation lawFormulation : law.getFormulations()) {
            if (lawFormulation.getBackend().equals("OWL")) {
                addOWLLawFormulation(lawFormulation.getName(), lawFormulation.getFormulation());
            } else if (lawFormulation.getBackend().equals(BACKEND_TYPE)) {
                try {
                    domainRules.add(Rule.parseRule(lawFormulation.getFormulation()));
                } catch (Rule.ParserException e) {
                    System.out.println("Following error in rule: " + lawFormulation.getFormulation());
                    e.printStackTrace();
                }
            } else if ("can_covert" != null) {

                // TODO: convert rules if possible ?
            }
        }
    }

    void addStatementFact(BackendFactEntity fact) {
        assert fact != null;
        assert fact.getVerb() != null;

        if (fact.getVerb().equals("rdf:type")) {
            try {
                addOWLLawFormulation(fact.getSubject(), fact.getObject());
                return;
            } catch (UnsupportedOperationException exception) {
                // continue
            }
        }

        String subj = fact.getSubject();
        String prop = fact.getVerb();
        String obj = fact.getObject();
//        // debug
//        System.out.println("addStatementFact( subj: " + subj + ", prop: " + prop + ", obj: " + obj + " )");
        String objType = fact.getObjectType();

//        Individual ind = model.getIndividual(baseIRIPrefix + subj);
        // retrieve or create
        Individual ind = model.createIndividual(baseIRIPrefix + subj, getThingClass());
        assert ind != null;

        if (objType.equals("owl:NamedIndividual")) {
            ObjectProperty p = model.createObjectProperty(baseIRIPrefix + prop);
            model.add(ind, p, model.createOntResource(baseIRIPrefix + obj));
        } else if (objType.equals("owl:Class")) {
            if (prop.equals("rdf:type")) {
            model.createIndividual(ind.getURI(), model.createClass(baseIRIPrefix + obj));
            } else {
                ObjectProperty p = model.createObjectProperty((baseIRIPrefix + prop));
                model.add(ind, p, model.createClass(baseIRIPrefix + obj));
            }
        } else if (objType.equals("xsd:int") || objType.equals("xsd:integer")) {
            DatatypeProperty p = model.createDatatypeProperty(baseIRIPrefix + prop);
            model.addLiteral(ind, p, Integer.parseInt(obj));
        } else if (objType.equals("xsd:string")) {
            DatatypeProperty p = model.createDatatypeProperty(baseIRIPrefix + prop);
            model.add(ind, p, obj);
        } else if (objType.equals("xsd:boolean")) {
            DatatypeProperty p = model.createDatatypeProperty(baseIRIPrefix + prop);
            model.addLiteral(ind, p, Boolean.parseBoolean(obj));
        } else if (objType.equals("xsd:double")) {
            DatatypeProperty p = model.createDatatypeProperty(baseIRIPrefix + prop);
            model.addLiteral(ind, p, Double.parseDouble(obj));
        } else if (objType.equals("xsd:float")) {
            DatatypeProperty p = model.createDatatypeProperty(baseIRIPrefix + prop);
            model.addLiteral(ind, p, Float.parseFloat(obj));
        } else if (prop.startsWith("not-for-reasoner:") || objType.equals("List<boolean>")) {
            // ignore this fact (as it's not for the reasoner)
        } else {
            throw new UnsupportedOperationException("JenaBackend.addStatementFact: unknown objectType: " + objType);
        }
    }


    private void callReasoner() {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(domainRules);

        long startTime = System.nanoTime();

        InfModel inf = ModelFactory.createInfModel(reasoner, model);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
        // print time report. TODO: remove the print
        System.out.println("Time Jena spent on reasoning: " + String.valueOf((float) (estimatedTime / 1000 / 1000) / 1000) + " seconds.");

        // use the inferred results (inf) ...
        model.add( inf );
    }

    String convertDatatype(String jenaType) {
        String[] typeParts = jenaType.split("\\.|]");
        return "xsd:" + typeParts[typeParts.length-1].toLowerCase();
    }

    public List<BackendFactEntity> getPropertyRelations(Property property) {
        List<BackendFactEntity> facts = new ArrayList<>();

        boolean isObjectProp = property instanceof ObjectProperty;
        String objType = isObjectProp ? "owl:NamedIndividual" : null;
        String propName = property.getLocalName();

        StmtIterator it = model.listStatements(null, property, (RDFNode) null);
        while (it.hasNext()) {
            Statement stmt = it.next();

            RDFNode objNode = stmt.getObject();
            String obj;

            if (isObjectProp) {
                obj = objNode.asResource().getLocalName();
            } else {
                if (objType == null) {
                    RDFDatatype dt = objNode.asLiteral().getDatatype();
                    objType = convertDatatype(dt.toString());
                }
                obj = objNode.asLiteral().getLexicalForm();
            }

            facts.add(new BackendFactEntity(
                    "owl:NamedIndividual",
                    stmt.getSubject().getLocalName(),
                    propName,
                    objType,
                    obj
            ));
        }
        return facts;
    }

    List<BackendFactEntity> getFacts(List<String> verbs) {

        List<BackendFactEntity> result = new ArrayList<>();

        ExtendedIterator<OntProperty> props = model.listAllOntProperties();  // try listAllOntProperties if incomplete
        while (props.hasNext()) {
            Property p = props.next();
            String propName = p.getLocalName();
            if (verbs.contains(propName)) {
                List<BackendFactEntity> verbFacts = getPropertyRelations(p);
                result.addAll(verbFacts);
            }
        }
        return result;
    }

    private void debug_dump_model(String name) {
        String out_rdf_path = "c:/temp/" + name + ".n3";
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(out_rdf_path);
            RDFDataMgr.write(out, model, Lang.NTRIPLES);  // Lang.NTRIPLES  or  Lang.RDFXML
            System.out.println("Debug written: " + out_rdf_path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Cannot write to file: " + out_rdf_path);
        }
    }

    @Override
    public List<BackendFactEntity> solve(List<Law> laws, List<BackendFactEntity> statement, List<String> solutionVerbs) {
        createOntology();
        for (Law law : laws) {
            addLaw(law);
        }

        for (BackendFactEntity fact : statement) {
            addStatementFact(fact);
        }

        callReasoner();

        return getFacts(solutionVerbs);
    }

    @Override
    public List<BackendFactEntity> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, List<String> violationVerbs) {
        createOntology();

        for (Law law : laws) {
            addLaw(law);
        }

        for (BackendFactEntity fact : statement) {
            addStatementFact(fact);
        }
        for (BackendFactEntity fact : response) {
            addStatementFact(fact);
        }
        for (BackendFactEntity fact : correctAnswer) {
            addStatementFact(fact);
        }

        callReasoner();

        return getFacts(violationVerbs);
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
    }

    public static void main(String ... args) {
        // Make sure that environment setup is OK
//        jenaBasicExample();
//        JenaBackendSmokeTest();
    }

}

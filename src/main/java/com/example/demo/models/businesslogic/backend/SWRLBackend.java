package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.entities.LawFormulation;
import com.example.demo.models.entities.Mistake;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.core.SWRLAPIRule;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SWRLBackend extends Backend {

    IRI OntologyIRI;
    OWLOntology Ontology;
    OWLOntologyManager Manager;
    OWLDataFactory Factory;

    public SWRLBackend() {
        String base = "http://www.test/test.owl";
        OntologyIRI = IRI.create(base);
        Manager = OWLManager.createOWLOntologyManager();
        Ontology = null;

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classloader.getResourceAsStream("ast_by_marks.owl");
        try {
            Ontology = Manager.createOntology(OntologyIRI);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        Factory = Manager.getOWLDataFactory();
        Manager.getOntologyFormat(Ontology).asPrefixOWLDocumentFormat().setDefaultPrefix(base + "#");
    }


    public OWLClass getThingClass() {
        final String OWL_THING_IRI = "http://www.w3.org/2002/07/owl#Thing";
        return Manager.getOWLDataFactory().getOWLClass(IRI.create(OWL_THING_IRI));
    }

    public OWLObjectProperty getObjectProperty(String propertyName) {
        return Manager.getOWLDataFactory().getOWLObjectProperty(getFullIRI(propertyName));
    }

    public OWLDataProperty getDataProperty(String propertyName) {
        return Manager.getOWLDataFactory().getOWLDataProperty(getFullIRI(propertyName));
    }

    void setDataProperty(OWLDataProperty dataProperty, OWLNamedIndividual ind, OWLLiteral val) {
        Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dataProperty, ind, val));
    }

    public OWLNamedIndividual addInstance(IRI name) {
        OWLNamedIndividual ind = Factory.getOWLNamedIndividual(name);
        Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(ind));
        return ind;
    }

    public OWLObjectProperty addObjectProperty(IRI name) {
        OWLObjectProperty op = Factory.getOWLObjectProperty(name);
        Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(op));
        return op;
    }

    public OWLDataProperty addDataProperty(IRI name) {
        OWLDataProperty dp = Factory.getOWLDataProperty(name);
        Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(dp));
        return dp;
    }

    abstract OWLNamedIndividual findIndividual(String object);

    IRI getFullIRI(String name) {
        return IRI.create(OntologyIRI + "#" + name);
    }

    public List<BackendFact> solve(List<Law> laws, List<BackendFact> statement) {
        for (BackendFact fact : statement) {
            if (fact.getVerb().equals("rdf:type")) {
                if (fact.getSubject().equals("owl:NamedIndividual")) {
                    addInstance(getFullIRI(fact.getObject()));
                } else if (fact.getSubject().equals("owl:ObjectProperty")) {
                    addObjectProperty(getFullIRI(fact.getObject()));
                } else if (fact.getSubject().equals("owl:DatatypeProperty")) {
                    addDataProperty(getFullIRI(fact.getObject()));
                }
            } else {
                OWLNamedIndividual ind = findIndividual(fact.getObject());
                if (fact.getSubjectType().equals("owl:NamedIndividual")) {
                    OWLObjectProperty op = getObjectProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLObjectPropertyAssertionAxiom(op, ind, findIndividual(fact.getSubject())));
                } else if (fact.getSubjectType().equals("xsd:int")) {
                    OWLDataProperty dp = getDataProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Integer.parseInt(fact.getSubject())));
                } else if (fact.getSubjectType().equals("xsd:string")) {
                    OWLDataProperty dp = getDataProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, fact.getSubject()));
                } else if (fact.getSubjectType().equals("xsd:boolean")) {
                    OWLDataProperty dp = getDataProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Boolean.parseBoolean(fact.getSubject())));
                } else if (fact.getSubjectType().equals("xsd:double")) {
                    OWLDataProperty dp = getDataProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Double.parseDouble(fact.getSubject())));
                } else if (fact.getSubjectType().equals("xsd:float")) {
                    OWLDataProperty dp = getDataProperty(fact.getVerb());
                    Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Float.parseFloat(fact.getSubject())));
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Mistake> judge(List<Law> laws, List<BackendFact> statement, List<BackendFact> correctAnswer, List<BackendFact> response) {
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(Ontology);

        for (Law law : laws) {
            for (LawFormulation lawFormulation : law.getLawFormulations()) {
                if (lawFormulation.getBackend().getName().equals("SWRL")) {
                    try {
                        ruleEngine.createSWRLRule(lawFormulation.getLaw().getName(), lawFormulation.getFormulation());
                    } catch (SWRLParseException | SWRLBuiltInException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (BackendFact fact : statement) {
            setDataProperty(getDataProperty(fact.getVerb()), findIndividual(fact.getObject()), Factory.getOWLLiteral(fact.getSubject()));
        }
        for (BackendFact fact : response) {
            setDataProperty(getDataProperty(fact.getVerb()), findIndividual(fact.getObject()), Factory.getOWLLiteral(fact.getSubject()));
        }

        return findErrors();
    }

    abstract List<Mistake> findErrors();
}

package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.entities.BackendFact;
import com.example.demo.models.entities.LawFormulation;
import com.example.demo.models.entities.Mistake;
import gnu.trove.map.hash.THashMap;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.*;

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

    abstract OWLNamedIndividual findIndividual(String object);

    IRI getFullIRI(String name) {
        return IRI.create(OntologyIRI + "#" + name);
    }

    boolean addStatementFact(BackendFact fact) {
        if (fact.getVerb().equals("rdf:type")) {
            if (fact.getObject().equals("owl:NamedIndividual")) {
                OWLNamedIndividual ind = Factory.getOWLNamedIndividual(getFullIRI(fact.getSubject()));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(ind));
            } else if (fact.getObject().equals("owl:ObjectProperty")) {
                OWLObjectProperty op = Factory.getOWLObjectProperty(getFullIRI(fact.getSubject()));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(op));
            } else if (fact.getObject().equals("owl:DatatypeProperty")) {
                OWLDataProperty dp = Factory.getOWLDataProperty(getFullIRI(fact.getSubject()));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(dp));
            } else if (fact.getObject().equals("owl:Class")) {
                OWLClass cl = Factory.getOWLClass(fact.getSubject());
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(cl));
            } else {
                return false;
            }
        } else if (fact.getVerb().equals("impl:ErrorDescription")) {
            // pass
            return false;
        } else {
            OWLNamedIndividual ind = findIndividual(fact.getSubject());
            if (fact.getObjectType().equals("owl:NamedIndividual")) {
                OWLObjectProperty op = getObjectProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLObjectPropertyAssertionAxiom(op, ind, findIndividual(fact.getObject())));
            } else if (fact.getObjectType().equals("xsd:int")) {
                OWLDataProperty dp = getDataProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Integer.parseInt(fact.getObject())));
            } else if (fact.getObjectType().equals("xsd:string")) {
                OWLDataProperty dp = getDataProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, fact.getObject()));
            } else if (fact.getObjectType().equals("xsd:boolean")) {
                OWLDataProperty dp = getDataProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Boolean.parseBoolean(fact.getObject())));
            } else if (fact.getObjectType().equals("xsd:double")) {
                OWLDataProperty dp = getDataProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Double.parseDouble(fact.getObject())));
            } else if (fact.getObjectType().equals("xsd:float")) {
                OWLDataProperty dp = getDataProperty(fact.getVerb());
                Manager.addAxiom(Ontology, Factory.getOWLDataPropertyAssertionAxiom(dp, ind, Float.parseFloat(fact.getObject())));
            } else {
                return false;
            }
        }
        return true;
    }

    public List<BackendFact> solve(List<Law> laws, List<BackendFact> statement) {
        for (BackendFact fact : statement) {
            addStatementFact(fact);
        }
        return new ArrayList<>();
    }

    THashMap<String, String> getErrorTypeToLawName(List<BackendFact> statement) {
        THashMap<String, String> errorTypeToLawName = new THashMap<>();
        for (BackendFact fact : statement) {
            if (fact.getVerb().equals("impl:ErrorDescription")) {
                if (fact.getObjectType().equals("owl:ObjectProperty")) {
                    errorTypeToLawName.put(fact.getSubject(), fact.getObject());
                }
            }
        }
        return errorTypeToLawName;
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
            addStatementFact(fact);
        }
        for (BackendFact fact : response) {
            addStatementFact(fact);
        }

        return findErrors(getErrorTypeToLawName(statement));
    }

    abstract List<Mistake> findErrors(THashMap<String, String> errorTypeToLawName);
}

package org.vstu.compprehension.models.businesslogic.backend;

import org.apache.commons.lang3.NotImplementedException;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.businesslogic.LawFormulation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.*;

public abstract class SWRLBackend implements Backend {

    IRI OntologyIRI;
    OWLOntology Ontology;
    OWLOntologyManager Manager;
    OWLDataFactory Factory;

    public SWRLBackend() {
        createOntology();
    }

    void createOntology() {
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
        Manager.getOntologyFormat(Ontology).asPrefixOWLOntologyFormat().setDefaultPrefix(base + "#");
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

    void addStatementFact(BackendFactEntity fact) {
        assert fact != null;
        assert fact.getVerb() != null;
        if (fact.getVerb().equals("rdf:type")) {
            addOWLLawFormulation(fact.getSubject(), fact.getObject());
            return;
        }

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
            throw new UnsupportedOperationException("SWRLBackend.addStatementFact unknown objectType");
        }
    }

    abstract List<BackendFactEntity> getObjectProperties(String objectProperty);
    abstract List<BackendFactEntity> getDataProperties(String dataProperty);
    abstract void callReasoner();

    void addOWLLawFormulation(String name, String type) {
            if (type.equals("owl:NamedIndividual")) {
                OWLNamedIndividual ind = Factory.getOWLNamedIndividual(getFullIRI(name));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(ind));
            } else if (type.equals("owl:ObjectProperty")) {
                OWLObjectProperty op = Factory.getOWLObjectProperty(getFullIRI(name));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(op));
            } else if (type.equals("owl:DatatypeProperty")) {
                OWLDataProperty dp = Factory.getOWLDataProperty(getFullIRI(name));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(dp));
            } else if (type.equals("owl:Class")) {
                OWLClass cl = Factory.getOWLClass(getFullIRI(name));
                Manager.addAxiom(Ontology, Factory.getOWLDeclarationAxiom(cl));
            } else {
                throw new UnsupportedOperationException("SWRLBackend.addOWLLawFormulations unknown type:" + type);
            }
    }

    void addLaw(Law law) {
        assert law != null;
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(Ontology);
        for (LawFormulation lawFormulation : law.getFormulations()) {
            if (lawFormulation.getBackend().equals("SWRL")) {
                try {
                    ruleEngine.createSWRLRule(lawFormulation.getName(), lawFormulation.getFormulation());
                } catch (SWRLParseException | SWRLBuiltInException e) {
                    e.printStackTrace();
                }
            } else if (lawFormulation.getBackend().equals("OWL")) {
                addOWLLawFormulation(lawFormulation.getName(), lawFormulation.getFormulation());
            }
        }
    }

    @Override
    public Collection<Fact> solve(List<Law> laws, List<BackendFactEntity> statement, ReasoningOptions reasoningOptions) {
        createOntology();
        for (Law law : laws) {
            addLaw(law);
        }

        for (BackendFactEntity fact : statement) {
            addStatementFact(fact);
        }

        callReasoner();

        return getFacts(reasoningOptions.getVerbs());
    }

    @Override
    public Collection<Fact> judge(List<Law> laws, List<BackendFactEntity> statement, List<BackendFactEntity> correctAnswer, List<BackendFactEntity> response, ReasoningOptions reasoningOptions) {
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

        return getFacts(reasoningOptions.getVerbs());
    }

    List<Fact> getFacts(Set<String> verbs) {
        List<BackendFactEntity> result = new ArrayList<>();
        if (verbs == null)
            throw new NotImplementedException("Old implementation of SWRLBackend does not support null or empty `verbs` on either `solve` or `judge`. 2023.02");
        for (String verb : verbs) {
            List<BackendFactEntity> verbFacts = getObjectProperties(verb);
            result.addAll(verbFacts);
            List<BackendFactEntity> verbFactsData = getDataProperties(verb);
            result.addAll(verbFactsData);
        }
        return Fact.entitiesToFacts(result);
    }
}

package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.LawFormulation;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.List;

public abstract class SWRLBackend extends Backend {

    IRI OntologyIRI;
    OWLOntology Ontology;
    OWLOntologyManager Manager;
    OWLDataFactory Factory;
    OWLClass Person;

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

        OWLClass adult = Factory.getOWLClass(IRI.create(OntologyIRI + "#Adult"));
        Person = Factory.getOWLClass(IRI.create(OntologyIRI + "#Person"));
        OWLDataProperty hasAge = Factory.getOWLDataProperty(IRI.create(OntologyIRI + "#hasAge"));

        OWLNamedIndividual john = Factory.getOWLNamedIndividual(IRI.create(OntologyIRI + "#John"));
        OWLNamedIndividual andrea = Factory.getOWLNamedIndividual(IRI.create(OntologyIRI + "#Andrea"));

        OWLClassAssertionAxiom classAssertion = Factory.getOWLClassAssertionAxiom(Person, john);
        Manager.addAxiom(Ontology, classAssertion);
        classAssertion = Factory.getOWLClassAssertionAxiom(Person, andrea);
        Manager.addAxiom(Ontology, classAssertion);

        OWLDatatype integerDatatype = Factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        OWLLiteral literal = Factory.getOWLLiteral("41", integerDatatype);
        OWLAxiom ax = Factory.getOWLDataPropertyAssertionAxiom(hasAge, andrea, literal);
        Manager.addAxiom(Ontology, ax);

        literal = Factory.getOWLLiteral("15", integerDatatype);
        ax = Factory.getOWLDataPropertyAssertionAxiom(hasAge, john, literal);
        Manager.addAxiom(Ontology, ax);
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

    abstract OWLNamedIndividual findIndividual(String object);

    IRI getFullIRI(String name) {
        return IRI.create(OntologyIRI + "#" + name);
    }

    @Override
    public List<Mistake> judge(List<Law> laws, HyperText problem, List<BackendFact> statement, List<BackendFact> response) {
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(Ontology);

        for (Law law : laws) {
            for (LawFormulation lawFormulation : law.getLawFormulations()) {
                if (lawFormulation.getBackend().getName() == "SWRL") {
                    try {
                        ruleEngine.createSWRLRule(lawFormulation.getId().toString(), lawFormulation.getFormulation());
                    } catch (SWRLParseException e) {
                        e.printStackTrace();
                    } catch (SWRLBuiltInException e) {
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

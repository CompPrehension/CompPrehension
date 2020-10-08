package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.ArrayList;
import java.util.List;

public class PelletBackend extends SWRLBackend {
    @Override
    public List<Mistake> judge(List<Law> laws, HyperText problem, List<BackendFact> statement, List<BackendFact> response) {
        String base = "http://www.test/test.owl";
        IRI ontologyIRI = IRI.create(base);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        try {
            ontology = manager.createOntology(ontologyIRI);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        OWLDataFactory factory = manager.getOWLDataFactory();
        manager.getOntologyFormat(ontology).asPrefixOWLDocumentFormat().setDefaultPrefix(base + "#");

        OWLClass adult = factory.getOWLClass(IRI.create(ontologyIRI + "#Adult"));
        OWLClass person = factory.getOWLClass(IRI.create(ontologyIRI + "#Person"));
        OWLDataProperty hasAge = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#hasAge"));

        OWLNamedIndividual john = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + "#John"));
        OWLNamedIndividual andrea = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + "#Andrea"));

        OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(person, john);
        manager.addAxiom(ontology, classAssertion);
        classAssertion = factory.getOWLClassAssertionAxiom(person, andrea);
        manager.addAxiom(ontology, classAssertion);

        OWLDatatype integerDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
        OWLLiteral literal = factory.getOWLLiteral("41", integerDatatype);
        OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(hasAge, andrea, literal);
        manager.addAxiom(ontology, ax);

        literal = factory.getOWLLiteral("15", integerDatatype);
        ax = factory.getOWLDataPropertyAssertionAxiom(hasAge, john, literal);
        manager.addAxiom(ontology, ax);

        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology);
        try {
            ruleEngine.createSWRLRule("r1", "Person(?p)^hasAge(?p,?age)^swrlb:greaterThan(?age,17) -> Adult(?p)");
        } catch (SWRLParseException e) {
            e.printStackTrace();
        } catch (SWRLBuiltInException e) {
            e.printStackTrace();
        }

        List<Mistake> result = new ArrayList<>();
        return result;
    }
}

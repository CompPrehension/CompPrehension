package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainLawViolation;
import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;/*
import com.sun.xml.bind.v2.runtime.reflect.ListTransducedAccessorImpl;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.swing.plaf.basic.BasicLabelUI;
import java.io.File;
import java.util.*;*/

import java.util.List;

public class OntologyBackend extends SWRLBackend {
    @Override
    public List<Mistake> judge(List<Law> laws, HyperText problem, List<BackendFact> statement, List<BackendFact> response) {
        return null;
    }
    /*
    static final String DEFAULT_FILENAME = "ontologies/test.owl";
    static final String DEFAULT_ONTOLOGY_IRI = "http://www.semanticweb.org/poas/ontologies/2020/5/test";

    public OntologyBackend() {
        this(DEFAULT_FILENAME, DEFAULT_ONTOLOGY_IRI);
    }

    public OntologyBackend(String ontologyFilename, String ontologyIRI) {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File file = new File(classloader.getResource(ontologyFilename).getFile());
        OntologyIRI = ontologyIRI;

        try {
            OntologyManager = OWLManager.createOWLOntologyManager();
            Ontology = OntologyManager.loadOntologyFromOntologyDocument(file);
            DataFactory = OntologyManager.getOWLDataFactory();

        } catch (OWLOntologyCreationException e) {
            System.err.println("Error creating OWL ontology: " + e.getMessage());
            System.exit(-1);
        }
    }



    //@Override
    //public List<DomainLawViolation> judge(List<DomainLaw> laws, List<BackendFact> problem, List<BackendFact> response) {
//
    //    // fillInstances();
//
    //    Reasoner = OpenlletReasonerFactory.getInstance().createReasoner(Ontology);
    //    NodeSet<OWLNamedIndividual> mistakes = Reasoner.getInstances(getClass("IncorrectAnswer"));
//
    //    if (! mistakes.isEmpty()) {
    //        return new ArrayList<DomainLawViolation>(1){{
    //            add(null);
    //        }};
    //    }
//
    //    return null;
    //}

    public OWLNamedIndividual addInstance(String classname, int index, int step) {
//        IRI name = getFullIRI("op-" + String.valueOf(step) + "-" + String.valueOf(index));
//        OWLNamedIndividual ind = addInstance(name);
//
//        setDataProperty(getDataProperty("index"), ind, DataFactory.getOWLLiteral(index));
//        setDataProperty(getDataProperty("step"), ind, DataFactory.getOWLLiteral(step));
//        return ind;
        return null;
    }

    public OWLClass getThingClass() {
        final String OWL_THING_IRI = "http://www.w3.org/2002/07/owl#Thing";
        return OntologyManager.getOWLDataFactory().getOWLClass(IRI.create(OWL_THING_IRI));
    }

    public OWLClass getClass(String name) {
//        String iri = OntologyManager.getOntologyDocumentIRI(Ontology) + "#" + name;
        IRI iri = getFullIRI(name);
//        return OntologyManager.getOWLDataFactory().getOWLClass(IRI.create(iri));
        return OntologyManager.getOWLDataFactory().getOWLClass(getFullIRI(name));
    }

    IRI getFullIRI(String name) {
        return IRI.create(OntologyIRI + "#" + name);
    }

    String OntologyIRI;
    OWLOntologyManager OntologyManager;
    OWLOntology Ontology;
    OWLDataFactory DataFactory;
    OpenlletReasoner Reasoner;
*/
}

package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.BackendFact;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PelletBackend extends SWRLBackend {
    PelletReasoner Reasoner;

    public PelletBackend () {
        super();
        Reasoner = PelletReasonerFactory.getInstance().createReasoner(Ontology);
    }

    public NodeSet<OWLNamedIndividual> getAllIndividuals() {
        return Reasoner.getInstances(getThingClass(), true);
    }

    public String getDataValue(OWLNamedIndividual ind, OWLDataProperty dataProperty) {
        Set<OWLLiteral> data = Reasoner.getDataPropertyValues(ind, dataProperty);

        if (data.size() > 1) {
            throw new RuntimeException(ind.toStringID() + " has multiple " + dataProperty.toString() + " :" + data.toString());
        }
        if (data.isEmpty()) {
            return "";
        }

        return data.iterator().next().getLiteral();
    }

    OWLNamedIndividual findIndividual(String object) {
        return Factory.getOWLNamedIndividual(getFullIRI(object));
    }

    HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> getObjectPropertyRelations(String objectProperty) {
        HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> relations = new HashMap<>();

        for (Node<OWLNamedIndividual> nodeInd : Reasoner.getInstances(getThingClass(), true)) {
            OWLNamedIndividual ind = nodeInd.getRepresentativeElement();

            OWLObjectProperty opProperty = getObjectProperty(objectProperty);

            Set<OWLNamedIndividual> inds = new HashSet<>();

            for (Node<OWLNamedIndividual> sameOpInd : Reasoner.getObjectPropertyValues(ind, opProperty)) {
                OWLNamedIndividual opInd = sameOpInd.getRepresentativeElement();
                inds.add(opInd);
            }

            relations.put(ind, inds);
        }

        return relations;
    }

    @Override
    public List<BackendFact> getObjectProperties(String objectProperty) {
        List<BackendFact> facts = new ArrayList<>();
        HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> relations = getObjectPropertyRelations(objectProperty);

        for (Map.Entry<OWLNamedIndividual, Set<OWLNamedIndividual>> relationsEntry : relations.entrySet()) {
            for (OWLNamedIndividual to : relationsEntry.getValue()) {
                facts.add(new BackendFact(relationsEntry.getKey().getIRI().getShortForm(), objectProperty, to.getIRI().getShortForm()));
            }
        }
        return facts;
    }

    @Override
    void callReasoner() {
        Reasoner.refresh();
    }
}

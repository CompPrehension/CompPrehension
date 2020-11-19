package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Mistake;
import gnu.trove.map.hash.THashMap;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.util.*;

public class PelletBackend extends SWRLBackend {
    OpenlletReasoner Reasoner;

    public PelletBackend () {
        super();
        Reasoner = OpenlletReasonerFactory.getInstance().createReasoner(Ontology);
    }

    public NodeSet<OWLNamedIndividual> getAllIndividuals() {
        return Reasoner.getInstances(getThingClass());
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

        for (Node<OWLNamedIndividual> nodeInd : Reasoner.getInstances(Factory.getOWLClass(getThingClass()))) {
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
    List<Mistake> findErrors(THashMap<String, String> errorTypeToLawName) {
        List<Mistake> result = new ArrayList<>();

        for (Map.Entry<String, String> errorTypeToLawNameEntry : errorTypeToLawName.entrySet()) {
            HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> rels = getObjectPropertyRelations(errorTypeToLawNameEntry.getKey());
            for (Map.Entry<OWLNamedIndividual, Set<OWLNamedIndividual>> relsEntry : rels.entrySet()) {
                for (OWLNamedIndividual ind : relsEntry.getValue()) {
                    Mistake mistake = new Mistake();
                    mistake.setLawName(errorTypeToLawNameEntry.getValue());
                    result.add(mistake);
                }
            }
        }

        return result;
    }
}

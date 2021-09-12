package org.vstu.compprehension.models.businesslogic.backend;

import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component @RequestScope
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

    HashMap<OWLNamedIndividual, Set<OWLLiteral>> getDataPropertyRelations(String dataProperty) {
        HashMap<OWLNamedIndividual, Set<OWLLiteral>> relations = new HashMap<>();

        for (Node<OWLNamedIndividual> nodeInd : Reasoner.getInstances(getThingClass(), true)) {
            OWLNamedIndividual ind = nodeInd.getRepresentativeElement();

            OWLDataProperty dpProperty = getDataProperty(dataProperty);

            relations.put(ind, Reasoner.getDataPropertyValues(ind, dpProperty));
        }

        return relations;
    }

    @Override
    public List<BackendFactEntity> getObjectProperties(String objectProperty) {
        List<BackendFactEntity> facts = new ArrayList<>();
        HashMap<OWLNamedIndividual, Set<OWLNamedIndividual>> relations = getObjectPropertyRelations(objectProperty);

        for (Map.Entry<OWLNamedIndividual, Set<OWLNamedIndividual>> relationsEntry : relations.entrySet()) {
            for (OWLNamedIndividual to : relationsEntry.getValue()) {
                facts.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        relationsEntry.getKey().getIRI().getShortForm(),
                        objectProperty,
                        "owl:NamedIndividual",
                        to.getIRI().getShortForm()
                ));
            }
        }
        return facts;
    }

    String convertDatatype(String pelletType) {
        String[] typeParts = pelletType.split("#");
        assert typeParts.length == 2;
        return "xsd:" + typeParts[1];
    }

    @Override
    public List<BackendFactEntity> getDataProperties(String dataProperty) {
        List<BackendFactEntity> facts = new ArrayList<>();
        HashMap<OWLNamedIndividual, Set<OWLLiteral>> relations = getDataPropertyRelations(dataProperty);

        for (Map.Entry<OWLNamedIndividual, Set<OWLLiteral>> relationsEntry : relations.entrySet()) {
            for (OWLLiteral to : relationsEntry.getValue()) {
                facts.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        relationsEntry.getKey().getIRI().getShortForm(),
                        dataProperty,
                        convertDatatype(to.getDatatype().toString()),
                        to.getLiteral()
                ));
            }
        }
        return facts;
    }


    @Override
    void callReasoner() {
        Reasoner = PelletReasonerFactory.getInstance().createReasoner(Ontology);
        Reasoner.refresh();
    }
}

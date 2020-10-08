package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.LawFormulation;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    List<Mistake> findErrors() {
        List<Mistake> result = new ArrayList<>();

        for (Node<OWLNamedIndividual> sameInd : Reasoner.getInstances(Person)) {
            OWLNamedIndividual ind = sameInd.getRepresentativeElement();

            OWLDataProperty dp = getDataProperty("hasAge");

            String age = getDataValue(ind, dp);
            Mistake m = new Mistake();
            result.add(m);
        }
        return result;
    }
}

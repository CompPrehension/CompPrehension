package org.vstu.compprehension.models.businesslogic.domains.helpers;


import its.model.DomainSolvingModel;
import its.model.Utils;
import its.model.definition.Domain;
import its.model.definition.compat.DomainDictionariesRDFBuilder;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.model.definition.rdf.DomainRDFFiller;
import its.model.nodes.xml.DecisionTreeXMLBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.Callable;

public abstract class DecisionTreeHelper {
    public static DomainSolvingModel buildDomainModelFromLOQI(URL domainURL){
        return withHandledExceptions(() -> buildDomainModel(
            domainURL,
            DomainLoqiBuilder.buildDomain(
                new BufferedReader(new InputStreamReader(Utils.plus(domainURL, "domain.loqi").openStream()))
            )
        ));
    }

    public static DomainSolvingModel buildDomainModelFromDict(URL domainURL){
        return withHandledExceptions(() -> buildDomainModel(
            domainURL,
            DomainDictionariesRDFBuilder.buildDomain(
                domainURL,
                Collections.singleton(DomainRDFFiller.Option.NARY_RELATIONSHIPS_OLD_COMPAT)
            )
        ));
    }

    private static DomainSolvingModel buildDomainModel(URL domainURL, Domain domain) throws URISyntaxException {
        return new DomainSolvingModel(
            domain,
            Collections.singletonMap(
                "",
                DecisionTreeXMLBuilder.fromXMLFile(Utils.plus(domainURL, "tree.xml").toURI().toString())
            )
        );
    }

    private static <T> T withHandledExceptions(Callable<T> callable){
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

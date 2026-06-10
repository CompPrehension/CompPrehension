package org.vstu.compprehension.models.businesslogic.domains.helpers;


import its.model.DomainSolvingModel;

import java.net.URL;
import java.util.concurrent.Callable;

public abstract class DecisionTreeHelper {
    public static DomainSolvingModel buildDomainModelFromLOQI(URL domainURL){
        return withHandledExceptions(() -> new DomainSolvingModel(domainURL, DomainSolvingModel.BuildMethod.LOQI, false));
    }

    public static DomainSolvingModel buildDomainModelFromDict(URL domainURL){
        return withHandledExceptions(() -> new DomainSolvingModel(domainURL, DomainSolvingModel.BuildMethod.DICT_RDF, false));
    }

    private static <T> T withHandledExceptions(Callable<T> callable){
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

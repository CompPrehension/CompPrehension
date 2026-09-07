package org.vstu.compprehension.businesslogic.backend.facts;


import org.vstu.compprehension.data.question.BackendFactData;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Fact {

    private BackendFactData entity;


    public Fact() {
        entity = new BackendFactData();
    }

    public Fact(BackendFactData entity) {
        this.entity = entity;
    }

    /** copying constructor */
    public Fact(Fact fact) {
        BackendFactData e = fact.asBackendFact();
        this.entity = new BackendFactData(
                e.getSubjectType(), e.getSubject(),
                e.getVerb(),
                e.getObjectType(), e.getObject()
        );
    }

    public Fact(String subjectType, String subject, String verb, String objectType, String object) {
        entity = new BackendFactData(subjectType, subject, verb, objectType, object);
    }
    public Fact(String subject, String verb, String object) {
        entity = new BackendFactData(subject, verb, object);
    }

    public BackendFactData asBackendFact() {
        return entity;
    }

    public String toString() {
        return "[" + getSubject()
                // + " ("+ getSubjectType() +")"
                + " . " + getVerb()
                + " >> " + getObject()
                // + " ("+ getObjectType() +")"
                + "]";
    }


    public static List<Fact> entitiesToFacts(Collection<BackendFactData> factEntities) {
        return factEntities.stream().map(Fact::new).collect(Collectors.toList());
    }
    public static List<BackendFactData> factsToEntities(Collection<Fact> facts) {
        return facts.stream().map(Fact::asBackendFact).collect(Collectors.toList());
    }


    /* Proxying get/set method calls to inner entity */

    public String getObject() {
        return entity.getObject();
    }

    public void setObject(String object) {
        this.entity.setObject(object);
    }

    public String getObjectType() {
        return entity.getObjectType();
    }

    public void setObjectType(String objectType) {
        this.entity.setObjectType(objectType);
    }

    public String getSubject() {
        return entity.getSubject();
    }

    public void setSubject(String subject) {
        this.entity.setSubject(subject);
    }

    public String getSubjectType() {
        return entity.getSubjectType();
    }

    public void setSubjectType(String subjectType) {
        this.entity.setSubjectType(subjectType);
    }

    public String getVerb() {
        return entity.getVerb();
    }

    public void setVerb(String verb) {
        this.entity.setVerb(verb);
    }
}

package com.example.demo.models.businesslogic.backend;

public class BackendFact {
    
    private String object;
    private String objectType;

    private String subject;
    private String subjectType;

    private String verb;

    public BackendFact(String object, String objectType, String subject, String subjectType, String verb) {
        this.object = object;
        this.objectType = objectType;
        this.subject = subject;
        this.subjectType = subjectType;
        this.verb = verb;
    }

    public BackendFact(String object, String subject, String verb) {
        this.object = object;
        this.objectType = "";
        this.subject = subject;
        this.subjectType = "";
        this.verb = verb;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }
}

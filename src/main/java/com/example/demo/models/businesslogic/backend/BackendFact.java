package com.example.demo.models.businesslogic.backend;

public class BackendFact {
    
    private String object;

    private String subject;

    private String verb;

    public BackendFact(String object, String subject, String verb) {
        this.object = object;
        this.subject = subject;
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
}

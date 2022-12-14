package org.vstu.compprehension.models.entities;

import lombok.*;

import javax.persistence.*;

@Getter @Setter
@EqualsAndHashCode
@NoArgsConstructor
@Table(name = "BackendFacts")
public class BackendFactEntity {
    private String object;
    private String objectType;
    private String subject;
    private String subjectType;
    private String verb;

    public String toString() {
        return "[subject=" + subject
                + "  verb=" + verb
                + "  object=" + object
                + "]";
    }

    public BackendFactEntity(String subjectType, String subject, String verb, String objectType, String object) {
        this.object = object;
        this.objectType = objectType;
        this.subject = subject;
        this.subjectType = subjectType;
        this.verb = verb;
    }

    public BackendFactEntity(String subject, String verb, String object) {
        this.object = object;
        this.objectType = "";
        this.subject = subject;
        this.subjectType = "";
        this.verb = verb;
    }
}

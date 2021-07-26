package org.vstu.compprehension.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "BackendFacts")
public class BackendFactEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bf_generator")
    @SequenceGenerator(name = "bf_generator", sequenceName = "BACKEND_FACTS_SEQUENCE")
    private Long id;

    @Column(name = "object")
    private String object;
    @Column(name = "object_type")
    private String objectType;

    @Column(name = "subject")
    private String subject;
    @Column(name = "subject_type")
    private String subjectType;

    @Column(name = "verb")
    private String verb;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "BackendFact_id")
    private QuestionEntity question;

    public String toString() {
        return "[id=" + id
                + ";  subject=" + subject
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

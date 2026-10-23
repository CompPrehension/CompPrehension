package org.vstu.compprehension.data.question;

import java.io.Serializable;
import lombok.*;

@Getter @Setter
@EqualsAndHashCode
@NoArgsConstructor
public class BackendFactData implements Serializable {
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

    public BackendFactData(String subjectType, String subject, String verb, String objectType, String object) {
        this.object = object;
        this.objectType = objectType;
        this.subject = subject;
        this.subjectType = subjectType;
        this.verb = verb;
    }

    public BackendFactData(String subject, String verb, String object) {
        this.object = object;
        this.objectType = "";
        this.subject = subject;
        this.subjectType = "";
        this.verb = verb;
    }
}

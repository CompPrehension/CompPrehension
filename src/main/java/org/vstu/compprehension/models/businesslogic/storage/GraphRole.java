package org.vstu.compprehension.models.businesslogic.storage;

enum GraphRole {
    SCHEMA("schema#"), // all static assertions important for reasoning
    SCHEMA_SOLVED("schema_s#"), // inferences from schema itself
//    QUESTIONS("questions#"),  // all question metadata


    QUESTION_TEMPLATE("qt#"), // template - a backbone for a question
    QUESTION_TEMPLATE_SOLVED("qt_s#"), // inferences from template itself

    QUESTION("q#"),  // data complementing template to complete question
    QUESTION_SOLVED("q_s#"), // inferences from whole question

    QUESTION_DATA("q_data#"), // data required to create a question


//    QUESTION_TEMPLATE_FULL("qt_f#", List.of(QUESTION_TEMPLATE, QUESTION_TEMPLATE_SOLVED)), // template + its
//    inferences
//    QUESTION_FULL("q_f#", List.of(QUESTION, QUESTION_SOLVED, QUESTION_TEMPLATE, QUESTION_TEMPLATE_SOLVED)), //
//    question + its inferences
    ;

    public final String prefix;

    private GraphRole(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Convert to NamespaceUtil instance
     */
    public NamespaceUtil ns() {
        return new NamespaceUtil(prefix);
    }

    /**
     * Convert to NamespaceUtil instance based on provided prefix
     */
    public NamespaceUtil ns(String basePrefix) {
        return new NamespaceUtil(basePrefix + prefix);
    }

    static GraphRole getNext(GraphRole role) {
        int ordIndex = role.ordinal() + 1;
        for (GraphRole other : GraphRole.values()) {
            if (other.ordinal() == ordIndex)
                return other;
        }
        return null;
    }

    static GraphRole getPrevious(GraphRole role) {
        int ordIndex = role.ordinal() - 1;
        for (GraphRole other : GraphRole.values()) {
            if (other.ordinal() == ordIndex)
                return other;
        }
        return null;
    }
}

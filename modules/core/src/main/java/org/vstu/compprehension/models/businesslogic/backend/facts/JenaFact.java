package org.vstu.compprehension.models.businesslogic.backend.facts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.vstu.compprehension.models.businesslogic.backend.util.TermMapping;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.Optional;

//@JsonIgnoreProperties({"statement", "subjTypeStatement", "factList"})
//@Table(name = "BackendFacts")
public class JenaFact extends Fact {

    public static final String EMPTY = "";

    @Getter
    @Setter
    @JsonIgnore
    private transient Statement statement = null;

    @Getter
    @JsonIgnore
    private transient Statement subjTypeStatement = null;

    /** Parent list of facts */
    @Getter
    @Setter
    @ToString.Exclude
    @JsonIgnore
    private transient JenaFactList factList = null;

    public JenaFact(Statement statement) {
        super(null, null, null, null, null);
        this.statement = statement;
    }

    public JenaFact(String subjectType, String subject, String verb, String objectType, String object, Statement statement) {
        super(subjectType, subject, verb, objectType, object);
        this.statement = statement;
    }
    public JenaFact(Fact fact, Statement statement) {
        super(fact.getSubjectType(), fact.getSubject(), fact.getVerb(), fact.getObjectType(), fact.getObject());
        this.statement = statement;
    }
    public JenaFact(BackendFactEntity fact, Statement statement) {
        super(fact.getSubjectType(), fact.getSubject(), fact.getVerb(), fact.getObjectType(), fact.getObject());
        this.statement = statement;
    }
    public JenaFact(BackendFactEntity fact) {
        super(fact.getSubjectType(), fact.getSubject(), fact.getVerb(), fact.getObjectType(), fact.getObject());
    }

    public JenaFact(Fact fact) {
        super(fact.asBackendFact());
    }

    public JenaFact(String subject, String verb, String object, Statement statement) {
        super(null, subject, verb, null, object);
        this.statement = statement;
    }

    public JenaFact(String subject, String verb, String object) {
        super(subject, verb, object);
    }

    public static JenaFact fromFact(Fact fact) {
        if (fact instanceof JenaFact) {
            return ((JenaFact) fact);
        } else {
            return new JenaFact(fact);
        }
    }

    public String toString__() {
        return "[" + getSubject()
                // + " ("+ getSubjectType() +")"
                + " . " + getVerb()
                + " >> " + getObject()
                // + " ("+ getObjectType() +")"
                + "]";
    }

    public BackendFactEntity asBackendFact() {
        return new BackendFactEntity(
                getSubjectType(), getSubject(), // getters ensure filling all fields with values
                getVerb(),
                getObjectType(), getObject()
        );
    }

    /** (Force) update statement from fact's SPO terms
     * */
    public void updateStatement() {
        if (factList == null)
            return;

        Model dataModel = factList.getModel();
        if (statement == null) {
            // create fresh statement
            statement = makeStatement(this, dataModel, factList.getTermMapping());
            dataModel.add(statement);

        } else /* statement is bound */ {
            TermMapping tm = factList.getTermMapping();
            Resource sR = tm.termToResource(getSubject());
            Property vR = tm.termToProperty(getVerb());
            RDFNode oR = tm.objectToLiteralOrResource(getObject(), getObjectType());
            if (statement.getSubject().equals(sR) && statement.getPredicate().equals(vR)) {
                if (!statement.getObject().equals(oR)) {
                    // change object only
                    statement.changeObject(oR);
                }
                // else: don't change anything
            } else {
                // replace with new (updated) statement
                dataModel.remove(statement);
                this.statement = dataModel.createStatement(sR, vR, oR);
                dataModel.add(statement);
            }
        }
    }

    /** Creates statement for model / finds it in the model. Does not add it to the model explicitly!
     * @param fact source fact
     * @param dataModel data model
     * @param tm term mapping from FactList
     * @return new statement
     */
    public static Statement makeStatement(Fact fact, Model dataModel, TermMapping tm) {
        Resource sR = tm.termToResource(fact.getSubject());
        Property vR = tm.termToProperty(fact.getVerb());
        RDFNode oR = tm.objectToLiteralOrResource(fact.getObject(), fact.getObjectType());
            // create fresh statement
            Statement statement = dataModel.createStatement(sR, vR, oR);
            /*dataModel.add(statement);*/
            return statement;
    }

    /** (Force) update fact's SPO terms from statement
     * */
    public void updateFactFromStatement() {
        if (factList == null || statement == null)
            return;

        TermMapping tm = factList.getTermMapping();
        super.setSubject(tm.resourceToTerm(statement.getSubject()));

        super.setVerb(tm.resourceToTerm(statement.getPredicate()));

        Pair<String, String> obj_objType = tm.literalOrResourceToStringAndType(statement.getObject());
        super.setObject(obj_objType.getLeft());
        super.setObjectType(obj_objType.getRight());
    }

    /** save data about subjectType from Fact. Note that this is saved not as rdf:type relation but as rdf:subject relation (to be later able to retrieve this data from model and not to confuse with other potential rdf:type relations).
     * @param subjectType arbitrary string denoting subject type
     */
    @Override
    public void setSubjectType(String subjectType) {
        super.setSubjectType(subjectType);
        if (statement == null)
            return;

        if (!EMPTY.equals(subjectType)) {
            if (subjTypeStatement == null) {
                Model dataModel = getModel();
                if (dataModel != null) {
                    subjTypeStatement = dataModel.createStatement(statement.getSubject(), RDF.subject /* not rdf:type !*/, (subjectType));
                    dataModel.add(subjTypeStatement);
                }
            } else {
                subjTypeStatement.changeObject(subjectType);
            }
        } else {
            // remove any notion from dataModel
            Model dataModel = getModel();
            if (dataModel != null) {
                dataModel.removeAll(statement.getSubject(), RDF.subject /* not rdf:type !*/, null);
            }

            subjTypeStatement = null;
        }
    }

    @Override
    public String getSubjectType() {
        if (super.getSubjectType() == null) {
            if (subjTypeStatement == null && statement != null) {
                // try find it in data model
                Model dataModel = getModel();
                if (dataModel != null) {
                    Optional<Statement> st = dataModel.listStatements(statement.getSubject(), RDF.subject /* not rdf:type !*/, (String) null).toSet().stream().findAny();
                    st.ifPresent(value -> subjTypeStatement = value);
                }
            }
            if (subjTypeStatement != null) {
                super.setSubjectType(subjTypeStatement.getLiteral().getString());
            } else {
                super.setSubjectType(EMPTY);
            }
        }
        return super.getSubjectType();
    }

    @Override
    public void setSubject(String subject) {
        boolean changed = !super.getSubject().equals(subject);
        super.setSubject(subject);

        if (changed && !subject.equals(EMPTY))
            updateStatement();
    }

    @Override
    public String getSubject() {
        String s = super.getSubject();
        if (s == null || s.equals(EMPTY)) {
            // fetch from model
            updateFactFromStatement();
        }
        return super.getSubject();
    }

    @Override
    public void setVerb(String verb) {
        boolean changed = !super.getVerb().equals(verb);
        super.setVerb(verb);

        if (changed && !verb.equals(EMPTY))
            updateStatement();
    }

    @Override
    public String getVerb() {
        String s = super.getVerb();
        if (s == null || s.equals(EMPTY)) {
            // fetch from model
            updateFactFromStatement();
        }
        return super.getVerb();
    }

    @Override
    public void setObject(String object) {
        boolean changed = !super.getObject().equals(object);
        super.setObject(object);

        if (changed /*&& !object.equals(EMPTY)*/)
            updateStatement();
    }

    @Override
    public String getObject() {
        String s = super.getObject();
        if (s == null /*|| s.equals(EMPTY)*/) {
            // fetch from model
            updateFactFromStatement();
        }
        return super.getObject();
    }


    /**
     * @return data model
     */
    @JsonIgnore
    public Model getModel() {
        if (statement != null)
            return statement.getModel();
        if (factList != null)
            return factList.getModel();
        return null;
    }
}


/*

* Caused by: java.lang.IllegalArgumentException: The given Json object value: FactList{model of size: 1296} cannot be transformed to a String

* Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class org.apache.jena.graph.impl.AllCapabilities and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: org.vstu.compprehension.models.businesslogic.backend.facts.FactList[0]->org.vstu.compprehension.models.businesslogic.backend.facts.FactTriple["statement"]->org.apache.jena.rdf.model.impl.StatementImpl["model"]->org.apache.jena.rdf.model.impl.ModelCom["graph"]->org.apache.jena.mem.GraphMem["capabilities"])

* */
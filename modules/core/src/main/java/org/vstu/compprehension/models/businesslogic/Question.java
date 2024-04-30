package org.vstu.compprehension.models.businesslogic;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.facts.JenaFactList;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;

public abstract class Question {
    @Getter
    @NotNull
    protected QuestionEntity questionData;
    
    @Getter @Setter
    protected List<String> concepts;
    @Setter
    protected List<String> negativeLaws;

    public @Nullable QuestionMetadataEntity getMetadata() {
        return questionData.getMetadata();
    }
    @Getter
    @Setter
    protected Set<String> tags;
    @Getter
    transient protected Domain domain;  // "transient" makes json reader ignore this field
    
    public Question(@NotNull QuestionEntity questionData, Domain domain) {
        this.questionData = questionData;
        this.domain = domain;
        concepts = new ArrayList<>();
        negativeLaws = new ArrayList<>();
        tags = new HashSet<>();
    }

    public int answerObjectsCount() {
        
        return questionData.getAnswerObjects().size();
    }

    public void addAnswerObject(AnswerObjectEntity newObject) {
        
        questionData.getAnswerObjects().add(newObject);
    }

    public void setAnswerObjects(List<AnswerObjectEntity> objects) {

        questionData.setAnswerObjects(objects);
    }

    public List<AnswerObjectEntity> getAnswerObjects() {
        
        return questionData.getAnswerObjects();
    }

    public HyperText getQuestionText() {
        
        return new HyperText(questionData.getQuestionText());
    }

    public String getQuestionName() {

        return questionData.getQuestionName();
    }

    /** Make an identifier of the question template that is unique in system scope. Intended to be used as a solution key in reasoner's cache for this question and questions having the same solution (i.e. generated from the same template).
     * @return name of the question template or question itself prefixed with domain short name
     */
    public String getQuestionUniqueTemplateName() {
        String domainPrefix = domain.getShortName();

        return domainPrefix + Optional.ofNullable(getMetadata())
                .map(QuestionMetadataEntity::getTemplateId)
                .filter(i -> i != 0)
                .map(tId -> ":template-id:" + tId)
                .orElse(":question:"+getQuestionName());
    }

    public AnswerObjectEntity getAnswerObject(int answerId) {
        return questionData.getAnswerObjects().stream()
                .filter(a -> a.getAnswerId() == answerId)
                .findFirst()
                .orElse(null);
    }

    public QuestionType getQuestionType() {
        
        return questionData.getQuestionType();
    }

    /**
     * Don't use it for normal questions, only for templates
     * @return
     */
    public List<String> getNegativeLaws() {
        if (negativeLaws == null) {
            negativeLaws = new ArrayList<>();
        }
        return negativeLaws;
    }

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в универсальной форме
     *
     * @return - факты в универсальной форме
     */
    public abstract Collection<Fact> responseToFacts(List<ResponseEntity> responses);

    public List<BackendFactEntity> getStatementFacts() {
        return questionData.getStatementFacts();
    }

    /** Get statement facts with common domain definitions for reasoning (schema) added */
    public Collection<Fact> getStatementFactsWithSchema() {
        JenaFactList fl = JenaFactList.fromBackendFacts(questionData.getStatementFacts());
        fl.addFromModel(domain.getSchemaForSolving());
        return fl;
    }

    public List<BackendFactEntity> getSolutionFacts() {
        return questionData.getSolutionFacts();
    }

    public String getQuestionDomainType() {
        return questionData.getQuestionDomainType();
    }

    public boolean isSupplementary() {
        return this.questionData.getQuestionDomainType().contains("Supplementary");
    }
}

package org.vstu.compprehension.businesslogic;

import org.vstu.compprehension.data.question.BackendFactData;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.backend.Fact;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.QuestionType;

import java.util.*;

public class Question {
    @Getter
    @NotNull
    protected QuestionData questionData;
    
    @Getter @Setter
    protected List<String> concepts;

    @Setter
    protected List<String> negativeLaws;

    public @Nullable QuestionMetadataData getMetadata() {
        return questionData.getMetadata();
    }

    @Getter
    @NotNull 
    final protected Domain domain;
    
    public Question(@NotNull QuestionData questionData, @NotNull Domain domain) {
        this.questionData = questionData;
        this.domain = domain;
        concepts = new ArrayList<>();
        negativeLaws = new ArrayList<>();
    }

    public @NotNull List<String> getTagNames() {
        return questionData.getTags();
    }

    public List<Tag> getTags() {
        return getTagNames().stream()
                .map(domain::getTag)
                .filter(Objects::nonNull)
                .toList();
    }

    public void setAnswerObjects(List<AnswerObjectData> objects) {

        questionData.setAnswerObjects(objects);
    }

    public List<AnswerObjectData> getAnswerObjects() {
        
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
                .map(QuestionMetadataData::getTemplateId)
                .filter(Objects::nonNull)
                .map(tId -> ":template-id:" + tId)
                .orElse(":question:"+getQuestionName());
    }

    public AnswerObjectData getAnswerObject(int answerId) {
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
    public Collection<Fact> responseToFacts(List<ResponseData> responses) {
        return domain.responseToFacts(this, responses);
    }

    public List<BackendFactData> getStatementFacts() {
        return questionData.getStatementFacts();
    }

    /** Get statement facts with common domain definitions for reasoning (schema) added */
    public Collection<Fact> getStatementFactsWithSchema() {
        return domain.getQuestionStatementFactsWithSchema(this);        
    }

    public List<BackendFactData> getSolutionFacts() {
        return questionData.getSolutionFacts();
    }

    public String getQuestionDomainType() {
        return questionData.getQuestionDomainType();
    }

    public boolean isSupplementary() {
        return this.questionData.getQuestionDomainType().contains("Supplementary");
    }
}

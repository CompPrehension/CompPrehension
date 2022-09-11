package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.utils.HyperText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class Question {
    
    protected QuestionEntity questionData;
    protected List<String> concepts;
    protected List<String> negativeLaws;
    protected HashSet<String> tags;
    transient protected Domain domain;  // "transient" makes json reader ignore this field
    
    public Question(QuestionEntity questionData, Domain domain) {
        this.questionData = questionData;
        this.domain = domain;
        concepts = new ArrayList<>();
        negativeLaws = new ArrayList<>();
        tags = new HashSet<>();
    }

    public Domain getDomain() {
        return this.domain;
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

    public AnswerObjectEntity getAnswerObject(int answerId) {
        return questionData.getAnswerObjects().stream()
                .filter(a -> a.getAnswerId() == answerId)
                .findFirst()
                .orElse(null);
    }

    public QuestionType getQuestionType() {
        
        return questionData.getQuestionType();
    }
    
    public QuestionEntity getQuestionData() {
        
        return questionData;
    }

    /**
     * Don't use it for normal questions, only for templates
     * @return
     */
    public List<String> getConcepts() {
        return concepts;
    }
    /**
     * Don't use it for normal questions, only for templates
     * @return
     */
    public HashSet<String> getTags() {
        return tags;
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
     * @return - факты в универсальной форме
     */
    public abstract List<BackendFactEntity> responseToFacts(List<ResponseEntity> responses);

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в удобном для указанного backend-а виде
     * @param backendId - id backend-а для которого будут сформированы
     *                  факты
     * @return - факты в том формате, в котором их поймет backend
     */
    public abstract List<BackendFactEntity> responseToFacts(long backendId);

    public List<BackendFactEntity> getStatementFacts() {
        return questionData.getStatementFacts();
    }

    public List<BackendFactEntity> getSolutionFacts() {
        return questionData.getSolutionFacts();
    }

    public String getQuestionDomainType() {
        return questionData.getQuestionDomainType();
    }

    public boolean isSupplementary() {
        return this.questionData != null && this.questionData.getQuestionDomainType().contains("Supplementary");
    }
}

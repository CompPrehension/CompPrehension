package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.QuestionBack;
import org.vstu.compprehension.models.businesslogic.frontend.QuestionFront;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.utils.HyperText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class Question implements QuestionFront, QuestionBack {
    
    protected QuestionEntity questionData;
    protected List<String> concepts;
    protected List<String> negativeLaws;
    protected HashSet<String> tags;
    
    public Question(QuestionEntity questionData) {
        this.questionData = questionData;
    }

    @Override
    public int answerObjectsCount() {
        
        return questionData.getAnswerObjects().size();
    }

    @Override
    public void addAnswerObject(AnswerObjectEntity newObject) {
        
        questionData.getAnswerObjects().add(newObject);
    }

    @Override
    public void setAnswerObjects(List<AnswerObjectEntity> objects) {

        questionData.setAnswerObjects(objects);
    }

    @Override
    public List<AnswerObjectEntity> getAnswerObjects() {
        
        return questionData.getAnswerObjects();
    }

    @Override
    public HyperText getQuestionText() {
        
        return new HyperText(questionData.getQuestionText());
    }

    @Override
    public String getQuestionName() {

        return questionData.getQuestionName();
    }

    @Override
    public AnswerObjectEntity getAnswerObject(int answerId) {
        return questionData.getAnswerObjects().stream()
                .filter(a -> a.getAnswerId() == answerId)
                .findFirst()
                .orElse(null);
    }

    @Override
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

    @Override
    public List<BackendFactEntity> getStatementFacts() {
        return questionData.getStatementFacts();
    }

    @Override
    public List<BackendFactEntity> getSolutionFacts() {
        return questionData.getSolutionFacts();
    }

    public String getQuestionDomainType() {
        return questionData.getQuestionDomainType();
    }

    @Override
    public boolean isSupplementary() {
        return this.questionData != null && this.questionData.getQuestionDomainType().contains("Supplementary");
    }
}

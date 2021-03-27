package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.QuestionBack;
import org.vstu.compprehension.models.businesslogic.frontend.QuestionFront;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.utils.HyperText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class Question implements QuestionFront, QuestionBack {
    
    protected QuestionEntity questionData;

    protected List<ResponseEntity> studentResponses = new ArrayList<>();

    protected List<String> concepts;
    protected HashSet<String> tags;
    
    protected boolean isFinalResponse = false;
    
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
    public void addResponse(ResponseEntity r) {
        
        studentResponses.add(r);
    }

    @Override
    public void addFullResponse(List<ResponseEntity> responses) {

        studentResponses = responses;
        isFinalResponse = true;
    }

    public void FinalResponse(boolean isFinalResponse) {

        this.isFinalResponse = isFinalResponse;
        
    }
    
    public boolean isFinalResponse() {
        
        return isFinalResponse;
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
    public AnswerObjectEntity getAnswerObject(int index) {
        
        return questionData.getAnswerObjects().get(index);
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
}

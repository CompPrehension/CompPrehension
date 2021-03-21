package com.example.demo.models.businesslogic;

import com.example.demo.models.businesslogic.backend.QuestionBack;
import com.example.demo.models.businesslogic.frontend.QuestionFront;
import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.BackendFactEntity;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.QuestionEntity;
import com.example.demo.models.entities.ResponseEntity;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;

public abstract class Question implements QuestionFront, QuestionBack {
    
    protected QuestionEntity questionData;

    protected List<ResponseEntity> studentResponses = new ArrayList<>();

    protected List<String> concepts;
    
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

    public List<String> getConcepts() {
        return concepts;
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

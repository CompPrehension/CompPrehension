package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.AnswerObject;
import com.example.demo.models.entities.BackendFact;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.entities.Response;
import com.example.demo.utils.HyperText;

import java.util.List;

public interface QuestionBack {
    
    public AnswerObject getAnswerObject(int index);
    
    public List<AnswerObject> getAnswerObjects();
    
    public int answerObjectsCount();
    
    public void addAnswerObject(AnswerObject newObject);
    
    public void setAnswerObjects(List<AnswerObject> objects);
    
    public void addResponse(Response r);
    
    public void addFullResponse(List<Response> responses);

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в универсальной форме
     * @return - факты в универсальной форме
     */
    public List<BackendFact> responseToFacts();

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в удобном для указанного backend-а виде 
     * @param backendId - id backend-а для которого будут сформированы 
     *                  факты
     * @return - факты в том формате, в котором их поймет backend
     */
    public abstract List<BackendFact> responseToFacts(long backendId);
    
    public QuestionType getQuestionType();

    public List<BackendFact> getStatementFacts();

    public String getQuestionDomainType();
}

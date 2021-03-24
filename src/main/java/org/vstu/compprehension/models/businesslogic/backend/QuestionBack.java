package org.vstu.compprehension.models.businesslogic.backend;

import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.List;

public interface QuestionBack {
    
    public AnswerObjectEntity getAnswerObject(int index);
    
    public List<AnswerObjectEntity> getAnswerObjects();
    
    public int answerObjectsCount();
    
    public void addAnswerObject(AnswerObjectEntity newObject);
    
    public void setAnswerObjects(List<AnswerObjectEntity> objects);
    
    public void addResponse(ResponseEntity r);
    
    public void addFullResponse(List<ResponseEntity> responses);

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в универсальной форме
     * @return - факты в универсальной форме
     */
    public List<BackendFactEntity> responseToFacts();

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в удобном для указанного backend-а виде 
     * @param backendId - id backend-а для которого будут сформированы 
     *                  факты
     * @return - факты в том формате, в котором их поймет backend
     */
    public abstract List<BackendFactEntity> responseToFacts(long backendId);
    
    public QuestionType getQuestionType();

    public List<BackendFactEntity> getStatementFacts();

    public List<BackendFactEntity> getSolutionFacts();

    public String getQuestionDomainType();

}

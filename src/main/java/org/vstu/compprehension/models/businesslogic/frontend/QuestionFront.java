package org.vstu.compprehension.models.businesslogic.frontend;

import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.utils.HyperText;

import java.util.List;

public interface QuestionFront {
    
    public List<AnswerObjectEntity> getAnswerObjects();
    
    public HyperText getQuestionText();

    public String getQuestionName();

    public AnswerObjectEntity getAnswerObject(int index);

    public int answerObjectsCount();
    
    public QuestionType getQuestionType();
    
    public Long getExerciseAttemptId();
    
    
}

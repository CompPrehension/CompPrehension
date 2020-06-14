package com.example.demo.models.businesslogic;

import com.example.demo.models.businesslogic.backend.BackendFact;
import com.example.demo.models.entities.AnswerObject;
import com.example.demo.models.entities.Question;
import com.example.demo.models.entities.QuestionConceptChoice;
import com.example.demo.models.entities.Response;

import java.util.ArrayList;
import java.util.List;

public class MultiChoice extends Choice {

    public MultiChoice(Question questionData) {
        super(questionData);
    }
}

package com.example.demo.models.businesslogic;

import com.example.demo.models.businesslogic.backend.Backend;
import com.example.demo.models.businesslogic.backend.BackendFact;
import com.example.demo.models.businesslogic.frontend.FrontAnswerElement;
import com.example.demo.models.entities.*;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SingleChoice extends Choice {

    public SingleChoice(com.example.demo.models.entities.Question questionData) {
        super(questionData);
    }

}

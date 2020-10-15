package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;

import java.util.List;

public abstract class Backend {

    public abstract List<Mistake> judge(List<Law> laws, List<BackendFact> statement, List<BackendFact> correctAnswer, List<BackendFact> response);
    public abstract List<BackendFact> solve(List<Law> laws, List<BackendFact> statement);

    class BackendAnswer {
        List<Mistake> Mistakes;
        List<BackendFact> CorrectAswer;
    };
    public abstract BackendAnswer solveAndJudge(List<Law> laws, List<BackendFact> statement, List<BackendFact> response);
}

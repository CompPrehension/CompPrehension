package com.example.demo.models.businesslogic;

import com.example.demo.Service.DomainService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProgrammingLanguageExpressionDomain extends Domain {
    public final static String name = "ProgrammingLanguageExpressionDomain";

    public ProgrammingLanguageExpressionDomain(DomainService domainService) {
        super(ProgrammingLanguageExpressionDomain.create(domainService));

        concepts = new ArrayList<>();
        laws = new ArrayList<>();

        concepts.add(Concept.createConcept("Basic arithmetics", domainEntity));
        concepts.add(Concept.createConcept("Associativity", domainEntity));
        concepts.add(Concept.createConcept("Basic logic operators", domainEntity));
        concepts.add(Concept.createConcept("Basic assignment", domainEntity));
        concepts.add(Concept.createConcept("Pointers", domainEntity));
        concepts.add(Concept.createConcept("Arrays", domainEntity));
        Concept precedence = Concept.createConcept("Precedence", domainEntity);
        concepts.add(precedence);

        List<Concept> precedenceConcepts = new ArrayList<>();
        precedenceConcepts.add(precedence);
        laws.add(Law.createLaw("Less operator precedence", true, domainEntity, precedenceConcepts));
    }

    static DomainEntity create(DomainService domainService) {
        if (domainService.hasDomainEntity(ProgrammingLanguageExpressionDomain.name)) {
            return domainService.getDomainEntity(ProgrammingLanguageExpressionDomain.name);
        } else {
            return domainService.createDomainEntity(
                    ProgrammingLanguageExpressionDomain.name,
                    "1"
            );
        }
    }

    @Override
    public void update() {
    }

    @Override
    public ExerciseForm getExerciseForm() {
        return null;
    }

    @Override
    public Exercise processExerciseForm(ExerciseForm ef) {
        return null;
    }

    @Override
    public Question makeQuestion(QuestionRequest questionRequest, Language userLanguage) {
        for (Concept concept : questionRequest.getTargetConcepts()) {
            if (concept.getName().equals("Pointers")) {
                com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
                question.setQuestionText("* * b");
                question.setQuestionType(QuestionType.ORDER);
                return new Ordering(question);
            }
        }

        com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
        question.setQuestionText("a + b + c");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        return new SingleChoice(question);
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        return null;
    }
}
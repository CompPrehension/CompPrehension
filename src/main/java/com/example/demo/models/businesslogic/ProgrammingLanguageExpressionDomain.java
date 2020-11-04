package com.example.demo.models.businesslogic;

import com.example.demo.Service.ConceptService;
import com.example.demo.Service.DomainService;
import com.example.demo.Service.LawService;
import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.utils.HyperText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProgrammingLanguageExpressionDomain extends Domain {
    final static String name = "ProgrammingLanguageExpressionDomain";

    public ProgrammingLanguageExpressionDomain(DomainService domainService, DomainRepository domainRepository, LawService lawService, ConceptService conceptService) {
        super(ProgrammingLanguageExpressionDomain.create(domainService, domainRepository, lawService, conceptService));
    }

    static com.example.demo.models.entities.Domain create(DomainService domainService, DomainRepository domainRepository, LawService lawService, ConceptService conceptService) {
        if (domainService.hasDomain(ProgrammingLanguageExpressionDomain.name)) {
            return domainService.getDomain(ProgrammingLanguageExpressionDomain.name);
        } else {
            List<Concept> concepts = new ArrayList<>();
            List<Law> laws = new ArrayList<>();

            com.example.demo.models.entities.Domain domain = domainService.createDomain(
                    ProgrammingLanguageExpressionDomain.name,
                    "1",
                    laws,
                    concepts
            );

            concepts.add(conceptService.getConcept("Basic arithmetics", domain));
            concepts.add(conceptService.getConcept("Associativity", domain));
            concepts.add(conceptService.getConcept("Basic logic operators", domain));
            concepts.add(conceptService.getConcept("Basic assignment", domain));
            concepts.add(conceptService.getConcept("Pointers", domain));
            concepts.add(conceptService.getConcept("Arrays", domain));
            concepts.add(conceptService.getConcept("Precedence", domain));

            List<Concept> precedenceConcepts = new ArrayList<>();
            precedenceConcepts.add(conceptService.getConcept("Precedence", domain));
            laws.add(lawService.getLaw("Less operator precedence", true, domain, precedenceConcepts));

            return domain;
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
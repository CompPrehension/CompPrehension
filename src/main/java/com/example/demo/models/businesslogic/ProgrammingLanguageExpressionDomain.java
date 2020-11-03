package com.example.demo.models.businesslogic;

import com.example.demo.Service.ConceptService;
import com.example.demo.Service.DomainService;
import com.example.demo.Service.LawService;
import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.models.repository.ConceptRepository;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.models.repository.LawRepository;
import com.example.demo.utils.HyperText;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ProgrammingLanguageExpressionDomain extends Domain {
    final static String name = "ProgrammingLanguageExpressionDomain";

    public ProgrammingLanguageExpressionDomain(com.example.demo.models.entities.Domain domain) {
        super(domain);
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
        return null;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        return null;
    }
}

@Configuration
class ProgrammingLanguageExpressionDomainConfiguration {
    @Bean
    public ProgrammingLanguageExpressionDomain create(DomainService domainService, DomainRepository domainRepository, LawService lawService, ConceptService conceptService) {
        if (domainService.hasDomain(ProgrammingLanguageExpressionDomain.name)) {
            return new ProgrammingLanguageExpressionDomain(domainService.getDomain(ProgrammingLanguageExpressionDomain.name));
        } else {
            List<Concept> concepts = new ArrayList<>();
            List<Law> laws = new ArrayList<>();

            ProgrammingLanguageExpressionDomain result = new ProgrammingLanguageExpressionDomain(domainService.createDomain(
                    ProgrammingLanguageExpressionDomain.name,
                    "1",
                    laws,
                    concepts
            ));

            concepts.add(conceptService.getConcept("Basic arithmetics", result.domain));
            concepts.add(conceptService.getConcept("Associativity", result.domain));
            concepts.add(conceptService.getConcept("Basic logic operators", result.domain));
            concepts.add(conceptService.getConcept("Basic assignment", result.domain));
            concepts.add(conceptService.getConcept("Pointers", result.domain));
            concepts.add(conceptService.getConcept("Arrays", result.domain));
            concepts.add(conceptService.getConcept("Precedence", result.domain));

            List<Concept> precedenceConcepts = new ArrayList<>();
            precedenceConcepts.add(conceptService.getConcept("Precedence", result.domain));
            laws.add(lawService.getLaw("Less operator precedence", true, result.domain, precedenceConcepts));

            result.domain.setConcepts(concepts);
            result.domain.setLaws(laws);
            domainRepository.save(result.domain);

            return result;
        }
    }
}

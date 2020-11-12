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

        Concept operatorConcept = addConcept("operator");
        Concept variableConcept = addConcept("variable");
        Concept literalConcept = addConcept("literal");
        Concept simpleOperandConcept = addConcept("simple_operand");
        Concept precedenceConcept = addConcept("precedence");
        Concept associativityConcept = addConcept("associativity");
        Concept arityConcept = addConcept("arity");
        Concept singleTokenUnaryConcept = addConcept("single_token_unary");
        Concept singleTokenBinaryConcept = addConcept("single_token_binary");
        Concept twoTokenUnaryConcept = addConcept("two_token_unary");
        Concept twoTokenBinaryConcept = addConcept("two_token_binary");
        Concept twoTokenTernaryConcept = addConcept("two_token_ternary");
        Concept operatorEvaluationStateConcept = addConcept("operator_evaluation_state");
        Concept leftAssociativityConcept = addConcept("left_associativity");
        Concept rightAssociativityConcept = addConcept("right_associativity");
        Concept absentAssociativityConcept = addConcept("absent_associativity");
        Concept operatorEvaluatingLeftOperandFirstConcept = addConcept("operator_evaluating_left_operand_first");
        Concept operatorUnaryPlusConcept = addConcept("operator_unary_+");
        Concept operatorBinaryPlusConcept = addConcept("operator_binary_+");
        Concept operatorEqualsConcept = addConcept("operator_==");
        Concept operatorPrefixIncrementConcept = addConcept("operator_prefix_++");

        List<Concept> singleTokenBinaryExecutionConcepts = List.of(
                precedenceConcept,
                associativityConcept,
                operatorConcept,
                singleTokenBinaryConcept,
                simpleOperandConcept,
                operatorEvaluationStateConcept);
        laws.add(Law.createLaw("single_token_binary_execution", true, domainEntity, singleTokenBinaryExecutionConcepts));

        List<Concept> errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedenceLeft = List.of(
                precedenceConcept,
                operatorConcept,
                singleTokenBinaryConcept,
                simpleOperandConcept
        );
        laws.add(Law.createLaw("error_single_token_binary_operator_has_unevaluated_higher_precedence_left", false, domainEntity, errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedenceLeft));
    }

    private Concept addConcept(String name) {
        Concept concept = Concept.createConcept(name, domainEntity);
        concepts.add(concept);
        return concept;
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
            if (concept.getName().equals("associativity")) {
                com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
                question.setQuestionText("* * b");
                question.setQuestionType(QuestionType.ORDER);
                return new Ordering(question);
            }
        }

        com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
        question.setQuestionText("a + b * c");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        return new SingleChoice(question);
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        return null;
    }
}
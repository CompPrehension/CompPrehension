package com.example.demo.models.businesslogic;

import com.example.demo.Service.DomainService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;
import gnu.trove.set.hash.THashSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProgrammingLanguageExpressionDomain extends Domain {
    public final static String name = "ProgrammingLanguageExpressionDomain";

    public ProgrammingLanguageExpressionDomain(DomainService domainService) {
        super(ProgrammingLanguageExpressionDomain.create(domainService));

        concepts = new ArrayList<>();
        positiveLaws = new ArrayList<>();
        negativeLaws = new ArrayList<>();

        Concept operandConcept = addConcept("operand");
        Concept simpleOperandConcept = addConcept("simple_operand");
        Concept operatorConcept = addConcept("operator", List.of(operandConcept));
        Concept variableConcept = addConcept("variable", List.of(simpleOperandConcept));
        Concept literalConcept = addConcept("literal", List.of(simpleOperandConcept));
        Concept precedenceConcept = addConcept("precedence");
        Concept associativityConcept = addConcept("associativity");
        Concept leftAssociativityConcept = addConcept("left_associativity", List.of(associativityConcept));
        Concept rightAssociativityConcept = addConcept("right_associativity", List.of(associativityConcept));
        Concept absentAssociativityConcept = addConcept("absent_associativity", List.of(associativityConcept));
        Concept arityConcept = addConcept("arity");
        Concept unaryConcept = addConcept("unary", List.of(arityConcept));
        Concept binaryConcept = addConcept("binary", List.of(arityConcept));
        Concept ternaryConcept = addConcept("ternary", List.of(arityConcept));
        Concept singleTokenOperatorConcept = addConcept("single_token");
        Concept twoTokenOperatorConcept = addConcept("two_token");
        Concept singleTokenUnaryConcept = addConcept("single_token_unary", List.of(singleTokenOperatorConcept, unaryConcept));
        Concept singleTokenBinaryConcept = addConcept("single_token_binary", List.of(singleTokenOperatorConcept, binaryConcept));
        Concept twoTokenUnaryConcept = addConcept("two_token_unary", List.of(twoTokenOperatorConcept, unaryConcept));
        Concept twoTokenBinaryConcept = addConcept("two_token_binary", List.of(twoTokenOperatorConcept, binaryConcept));
        Concept twoTokenTernaryConcept = addConcept("two_token_ternary", List.of(twoTokenOperatorConcept, binaryConcept));
        Concept operatorEvaluationStateConcept = addConcept("operator_evaluation_state");
        Concept operatorEvaluatingLeftOperandFirstConcept = addConcept("operator_evaluating_left_operand_first", List.of(binaryConcept, operatorEvaluationStateConcept));
        Concept operatorUnaryPlusConcept = addConcept("operator_unary_+", List.of(singleTokenUnaryConcept));
        Concept operatorBinaryPlusConcept = addConcept("operator_binary_+", List.of(singleTokenBinaryConcept));
        Concept operatorBinaryMultipleConcept = addConcept("operator_binary_*", List.of(singleTokenBinaryConcept));
        Concept operatorEqualsConcept = addConcept("operator_==", List.of(singleTokenBinaryConcept));
        Concept prefixOperatorConcept = addConcept("prefix", List.of(unaryConcept));
        Concept postfixOperatorConcept = addConcept("postfix", List.of(unaryConcept));
        Concept operatorPrefixIncrementConcept = addConcept("operator_prefix_++", List.of(singleTokenUnaryConcept, prefixOperatorConcept));

        List<Concept> singleTokenBinaryExecutionConcepts = List.of(
                precedenceConcept,
                associativityConcept,
                operatorConcept,
                singleTokenBinaryConcept,
                simpleOperandConcept,
                operatorEvaluationStateConcept);
        positiveLaws.add(new PositiveLaw(
                "single_token_binary_execution",
                List.of(),
                singleTokenBinaryExecutionConcepts,
                List.of()
        ));

        List<Concept> binaryPlusAssociativityConcepts = List.of(
                leftAssociativityConcept,
                operatorBinaryPlusConcept
        );
        positiveLaws.add(new PositiveLaw(
                "operator_binary_+_associativity_left",
                List.of(),
                binaryPlusAssociativityConcepts,
                List.of()
        ));
        List<Concept> binaryMultipleAssociativityConcepts = List.of(
                leftAssociativityConcept,
                operatorBinaryMultipleConcept
        );
        positiveLaws.add(new PositiveLaw(
                "operator_binary_*_associativity_left",
                List.of(),
                binaryMultipleAssociativityConcepts,
                List.of()
        ));
        List<Concept> unaryPlusAssociativityConcepts = List.of(
                rightAssociativityConcept,
                operatorBinaryPlusConcept
        );
        positiveLaws.add(new PositiveLaw(
                "operator_unary_+_associativity_right",
                List.of(),
                unaryPlusAssociativityConcepts,
                List.of()
        ));
        List<Concept> mulHigherPlusPrecedenceConcepts = List.of(
                operatorBinaryMultipleConcept,
                operatorBinaryPlusConcept,
                precedenceConcept
        );
        positiveLaws.add(new PositiveLaw(
                "precedence_binary_*_higher_binary_+",
                List.of(),
                mulHigherPlusPrecedenceConcepts,
                List.of()
        ));
        List<Concept> unaryPlusHigherMulPrecedenceConcepts = List.of(
                operatorBinaryMultipleConcept,
                operatorBinaryPlusConcept,
                precedenceConcept
        );
        positiveLaws.add(new PositiveLaw(
                "precedence_unary_+_higher_binary_*",
                List.of(),
                unaryPlusHigherMulPrecedenceConcepts,
                List.of()
        ));


        List<Concept> errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence = List.of(
                precedenceConcept,
                operatorConcept,
                singleTokenBinaryConcept
        );
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_higher_precedence_left",
                List.of(),
                errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence,
                List.of(),
                getPositiveLaw("single_token_binary_execution")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                List.of(),
                errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence,
                List.of(),
                getPositiveLaw("single_token_binary_execution")));

        List<Concept> errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity = List.of(
                associativityConcept,
                operatorConcept,
                singleTokenBinaryConcept
        );
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                List.of(),
                errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity,
                List.of(),
                getPositiveLaw("single_token_binary_execution")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right",
                List.of(),
                errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity,
                List.of(),
                getPositiveLaw("single_token_binary_execution")
        ));

        List<Concept> errorNotLeftAssociativityBinaryPlus = List.of(
                associativityConcept,
                operatorBinaryPlusConcept
        );
        negativeLaws.add(new NegativeLaw(
                "error_binary_+_right_associativity",
                List.of(),
                errorNotLeftAssociativityBinaryPlus,
                List.of(),
                getPositiveLaw("operator_binary_+_associativity_left")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_binary_+_absent_associativity",
                List.of(),
                errorNotLeftAssociativityBinaryPlus,
                List.of(),
                getPositiveLaw("operator_binary_+_associativity_left")
        ));
        negativeLaws.add(new NegativeLaw(
                "precedence_binary_*_less_binary_+",
                List.of(),
                mulHigherPlusPrecedenceConcepts,
                List.of(),
                getPositiveLaw("precedence_binary_*_higher_binary_+")
        ));
        negativeLaws.add(new NegativeLaw(
                "precedence_binary_*_equal_binary_+",
                List.of(),
                mulHigherPlusPrecedenceConcepts,
                List.of(),
                getPositiveLaw("precedence_binary_*_higher_binary_+")
        ));
    }

    private Concept addConcept(String name, List<Concept> baseConcepts) {
        Concept concept = new Concept(name, baseConcepts);
        concepts.add(concept);
        return concept;
    }

    private Concept addConcept(String name) {
        Concept concept = new Concept(name);
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
        THashSet<String> conceptNames = new THashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }
        THashSet<String> allowedConceptNames = new THashSet<>();
        for (Concept concept : questionRequest.getAllovedConcepts()) {
            allowedConceptNames.add(concept.getName());
        }
        THashSet<String> deniedConceptNames = new THashSet<>();
        for (Concept concept : questionRequest.getDeniedConcepts()) {
            deniedConceptNames.add(concept.getName());
        }

        if (conceptNames.contains("associativity") &&
                allowedConceptNames.contains("operator_binary_+") &&
                !conceptNames.contains("precedence")) {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("a + b + c");
            question.setAnswerObjects(List.of(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", false),
                    getAnswerObject(question, "+ between b and c", "operator_binary_+", false)
            ));
            question.setQuestionDomainType("OrderOperators");
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(List.of("a", "+", "b", "+", "c")));
            question.setQuestionLaws(List.of(
                    getQuestionLaw(question, "operator_binary_+_associativity_left"),
                    getQuestionLaw(question, "single_token_binary_execution"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_left"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_right"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left"),
                    getQuestionLaw(question, "error_binary_+_right_associativity"),
                    getQuestionLaw(question, "error_binary_+_absent_associativity")
            ));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        } else if (conceptNames.contains("precedence") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*") &&
                deniedConceptNames.contains("associativity")) {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("a + b * c");
            question.setAnswerObjects(List.of(
                    getAnswerObject(question, "+", "operator_binary_+", false),
                    getAnswerObject(question, "*", "operator_binary_*", false)
            ));
            question.setQuestionDomainType("OrderOperators");
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(List.of("a", "+", "b", "*", "c")));
            question.setQuestionLaws(List.of(
                    getQuestionLaw(question, "precedence_binary_*_higher_binary_+"),
                    getQuestionLaw(question, "single_token_binary_execution"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_left"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_right"),
                    getQuestionLaw(question, "precedence_binary_*_less_binary_+"),
                    getQuestionLaw(question, "precedence_binary_*_equal_binary_+")
            ));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        }  else if (conceptNames.contains("precedence") &&
                conceptNames.contains("associativity") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*")) {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("a + b + c * d");
            question.setQuestionDomainType("OrderOperators");
            question.setAreAnswersRequireContext(true);
            question.setAnswerObjects(List.of(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", false),
                    getAnswerObject(question, "+ between c and d", "operator_binary_+", false),
                    getAnswerObject(question, "*", "operator_binary_*", false)
            ));
            question.setStatementFacts(getBackendFacts(List.of("a", "+", "b", "+", "c", "*", "d")));
            question.setQuestionLaws(List.of(
                    getQuestionLaw(question, "precedence_binary_*_higher_binary_+"),
                    getQuestionLaw(question, "operator_binary_+_associativity_left"),
                    getQuestionLaw(question, "single_token_binary_execution"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_left"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_higher_precedence_right"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right"),
                    getQuestionLaw(question, "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left"),
                    getQuestionLaw(question, "error_binary_+_right_associativity"),
                    getQuestionLaw(question, "error_binary_+_absent_associativity"),
                    getQuestionLaw(question, "precedence_binary_*_less_binary_+"),
                    getQuestionLaw(question, "precedence_binary_*_equal_binary_+")
            ));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        } else {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("Choose associativity of operator binary +");
            question.setQuestionType(QuestionType.SINGLE_CHOICE);
            question.setQuestionDomainType("ChooseAssociativity");
            question.setAreAnswersRequireContext(true);
            question.setAnswerObjects(List.of(
                    getAnswerObject(question, "left", "left_associativity", false),
                    getAnswerObject(question, "right", "right_associativity", false),
                    getAnswerObject(question, "no associativity", "absent_associativity", false)
            ));
            question.setQuestionLaws(List.of(
                    getQuestionLaw(question, "operator_binary_+_associativity_left"),
                    getQuestionLaw(question, "error_binary_+_right_associativity"),
                    getQuestionLaw(question, "error_binary_+_absent_associativity")
            ));
            return new SingleChoice(question);
        }
    }

    String getName(int step, int index) {
        return "op-" + step + "-" + index;
    }

    QuestionLaw getQuestionLaw(com.example.demo.models.entities.Question question, String law) {
        QuestionLaw questionLaw = new QuestionLaw();
        questionLaw.setDomainEntity(domainEntity);
        questionLaw.setLawName(law);
        questionLaw.setQuestion(question);
        return questionLaw;
    }

    AnswerObject getAnswerObject(com.example.demo.models.entities.Question question, String text, String concept, boolean isRightCol) {
        AnswerObject answerObject = new AnswerObject();
        answerObject.setHyperText(text);
        answerObject.setRightCol(isRightCol);
        answerObject.setConcept(concept);
        answerObject.setQuestion(question);
        return answerObject;
    }

    List<BackendFact> getBackendFacts(List<String> expression) {
        List<BackendFact> facts = new ArrayList<>();
        int index = 0;
        for (String token : expression) {
            index++;
            for (int step = 0; step <= expression.size(); ++step) {
                String name = getName(step, index);
                facts.add(new BackendFact(name, "rdf:type", "owl:NamedIndividual"));
                facts.add(new BackendFact("owl:NamedIndividual", name, "index", "xsd:int", String.valueOf(index)));
                facts.add(new BackendFact("owl:NamedIndividual", name, "step", "xsd:int", String.valueOf(step)));
            }
            facts.add(new BackendFact("owl:NamedIndividual", getName(0, index), "text", "xsd:string", token));
        }
        facts.add(new BackendFact("owl:NamedIndividual", getName(0, index), "last", "xsd:boolean", "true"));
        return facts;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        return null;
    }
}
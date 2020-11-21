package com.example.demo.models.businesslogic;

import com.example.demo.Service.DomainService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
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
                getAllLaws(),
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
        HashSet<String> conceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getTargetConcepts()) {
            conceptNames.add(concept.getName());
        }
        HashSet<String> allowedConceptNames = new HashSet<>();
        for (Concept concept : questionRequest.getAllowedConcepts()) {
            allowedConceptNames.add(concept.getName());
        }
        HashSet<String> deniedConceptNames = new HashSet<>();
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
        List<BackendFact> facts = getSWRLBackendBaseFacts();
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

    List<BackendFact> getSWRLBackendBaseFacts() {
        List<BackendFact> facts = new ArrayList<>();
        facts.add(new BackendFact("zero_step", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("all_app_to_left", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("all_app_to_right", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("all_eval_to_right", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("ast_edge", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before_all_operands", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before_as_operand", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before_by_third_operator", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before_direct", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("before_third_operator", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("complex_boundaries", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("copy", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("copy_without_marks", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("describe_error", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("find_left_operand", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("find_right_operand", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("has_complex_operator_part", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("has_operand", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("high_priority", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("high_priority_diff_priority", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("high_priority_left_assoc", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("high_priority_right_assoc", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("in_complex", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("more_priority_left_by_step", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("more_priority_right_by_step", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("next_index", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("next_step", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("not_index", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("prev_index", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("prev_operand", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("prev_operation", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("same_step", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_equal_priority", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_in_complex", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_left_assoc", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_more_priority", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_more_priority_left", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_more_priority_right", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_right_assoc", "rdf:type", "owl:ObjectProperty"));
        facts.add(new BackendFact("student_error_strict_operands_order", "rdf:type", "owl:ObjectProperty"));

        facts.add(new BackendFact("app", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("arity", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("associativity", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("complex_beginning", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("complex_ending", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("error_description", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("eval", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("eval_step", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("has_highest_priority_to_left", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("has_highest_priority_to_right", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("index", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("init", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("is_function_call", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("is_operand", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("is_operator_with_strict_operands_order", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("last", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("precedence", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("prefix_postfix", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("real_pos", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("step", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("student_pos", "rdf:type", "owl:DatatypeProperty"));
        facts.add(new BackendFact("text", "rdf:type", "owl:DatatypeProperty"));

        return facts;
    }

    public List<LawFormulation> getAllLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getLawFormulation(
                "zero_step",
                "index(?a, ?a_index) ^ index(?b, ?a_index) ^ step(?b, 0) -> zero_step(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "all_app_to_left",
                "all_app_to_left(?a, ?b) ^ prev_index(?b, ?c) ^ app(?c, true) -> all_app_to_left(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "all_app_to_left_begin",
                "init(?a, true) ^ has_highest_priority_to_left(?a, true) -> all_app_to_left(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "all_app_to_right",
                "all_app_to_right(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> all_app_to_right(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "all_app_to_right_begin",
                "has_highest_priority_to_right(?a, true) ^ init(?a, true) -> all_app_to_right(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "all_eval_to_right",
                "all_eval_to_right(?a, ?b) ^ next_index(?b, ?c) ^ eval(?c, true) -> all_eval_to_right(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "all_eval_to_right_app",
                "all_eval_to_right(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> all_eval_to_right(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "all_eval_to_right_begin",
                "has_highest_priority_to_right(?a, true) ^ init(?a, true) ^ complex_beginning(?a, true) ^ has_highest_priority_to_left(?a, true) -> all_eval_to_right(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "ast_edge_has_complex_operator_part",
                "has_complex_operator_part(?a, ?b) -> ast_edge(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "ast_edge_has_operand",
                "has_operand(?a, ?b) -> ast_edge(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "before",
                "has_operand(?a, ?b) ^ text(?b, ?b_text) ^ swrlb:notEqual(?b_text, \"(\") -> before_direct(?b, ?a) ^ before_as_operand(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "before_all_operands",
                "before_all_operands(?a, ?b) ^ has_operand(?b, ?c) -> before_direct(?a, ?c) ^ before_by_third_operator(?a, ?c) ^ before_all_operands(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "before_before",
                "before(?a, ?b) ^ before(?b, ?c) -> before(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "before_direct",
                "before_direct(?a, ?b) -> before(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "before_function_call",
                "has_operand(?a, ?b) ^ text(?b, \"(\") ^ is_function_call(?b, true) -> before_direct(?b, ?a) ^ before_as_operand(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "before_in_complex",
                "has_operand(?a, ?b) ^ text(?b, \"(\") ^ has_operand(?b, ?c) -> before_direct(?c, ?a) ^ before_by_third_operator(?c, ?a) ^ before_third_operator(?c, ?b)"
        ));
        laws.add(getLawFormulation(
                "before_strict_order_operands",
                "is_operator_with_strict_operands_order(?a, true) ^ text(?a, ?a_text) ^ swrlb:notEqual(?a_text, \"?\") ^ has_operand(?a, ?b) ^ has_operand(?a, ?c) ^ index(?b, ?b_index) ^ index(?c, ?c_index) ^ swrlb:lessThan(?b_index, ?c_index) -> before_direct(?b, ?c) ^ before_all_operands(?b, ?c) ^ before_by_third_operator(?b, ?c) ^ before_third_operator(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "before_strict_order_operands_ternary",
                "text(?a, \"?\") ^ has_operand(?a, ?b) ^ has_operand(?a, ?c) ^ has_operand(?a, ?d) ^ index(?b, ?b_index) ^ index(?c, ?c_index) ^ index(?d, ?d_index) ^ not_index(?c, ?d) ^ swrlb:lessThan(?b_index, ?c_index) ^ swrlb:lessThan(?b_index, ?d_index) -> before_direct(?b, ?c) ^ before_all_operands(?b, ?c) ^ before_by_third_operator(?b, ?c) ^ before_third_operator(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "complex_beggining_false",
                "swrlb:notEqual(?a_text, \"(\") ^ swrlb:notEqual(?a_text, \"[\") ^ swrlb:notEqual(?a_text, \"?\") ^ text(?a, ?a_text) ^ step(?a, 0) -> complex_beginning(?a, false)"
        ));
        laws.add(getLawFormulation(
                "complex_beginning(",
                "text(?a, \"(\") ^ step(?a, 0) -> complex_beginning(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_beginning?",
                "text(?a, \"?\") ^ step(?a, 0) -> complex_beginning(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_beginning[",
                "text(?a, \"[\") ^ step(?a, 0) -> complex_beginning(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_boundaries",
                "in_complex(?a, ?c) ^ next_index(?a, ?b) ^ complex_beginning(?a, false) ^ complex_ending(?b, true) ^ step(?a, 0) -> complex_boundaries(?c, ?b)"
        ));
        laws.add(getLawFormulation(
                "complex_boundaries_empty",
                "next_index(?a, ?b) ^ step(?a, 0) ^ complex_beginning(?a, true) ^ complex_ending(?b, true) -> complex_boundaries(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "complex_ending)",
                "text(?a, \")\") ^ step(?a, 0) -> complex_ending(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_ending:",
                "text(?a, \":\") ^ step(?a, 0) -> complex_ending(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_ending]",
                "text(?a, \"]\") ^ step(?a, 0) -> complex_ending(?a, true)"
        ));
        laws.add(getLawFormulation(
                "complex_ending_false",
                "text(?a, ?a_text) ^ swrlb:notEqual(?a_text, \")\") ^ swrlb:notEqual(?a_text, \"]\") ^ swrlb:notEqual(?a_text, \":\") ^ step(?a, 0) -> complex_ending(?a, false)"
        ));
        laws.add(getLawFormulation(
                "copy_app",
                "copy(?a, ?to) ^ app(?a, true) -> app(?to, true)"
        ));
        laws.add(getLawFormulation(
                "copy_eval",
                "copy(?a, ?to) ^ eval(?a, true) -> eval(?to, true)"
        ));
        laws.add(getLawFormulation(
                "copy_eval_step_to_zero_step",
                "eval_step(?a, ?a_step) ^ zero_step(?a, ?a0) -> eval_step(?a0, ?a_step)"
        ));
        laws.add(getLawFormulation(
                "copy_has_complex_operator_part_to_zero_step",
                "has_complex_operator_part(?a, ?b) ^ zero_step(?a, ?a0) ^ zero_step(?b, ?b0) -> has_complex_operator_part(?a0, ?b0)"
        ));
        laws.add(getLawFormulation(
                "copy_has_operand_to_zero_step",
                "has_operand(?a, ?b) ^ zero_step(?a, ?a0) ^ zero_step(?b, ?b0) -> has_operand(?a0, ?b0)"
        ));
        laws.add(getLawFormulation(
                "copy_init",
                "copy(?a, ?to) ^ init(?a, true) -> init(?to, true)"
        ));
        laws.add(getLawFormulation(
                "copy_to_zero_step",
                "step(?a, 0) ^ step(?b, 1) ^ zero_step(?b, ?a) -> copy_without_marks(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "copy_to_1_step",
                "step(?a, 0) ^ step(?b, 1) ^ zero_step(?b, ?a) -> copy(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks",
                "copy(?a, ?to) -> copy_without_marks(?a, ?to)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_arity",
                "arity(?a, ?a_arity) ^ copy_without_marks(?a, ?to) -> arity(?to, ?a_arity)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_associativity",
                "associativity(?a, ?a_associativity) ^ copy_without_marks(?a, ?to) -> associativity(?to, ?a_associativity)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_complex_beginning",
                "complex_beginning(?a, ?b) ^ copy_without_marks(?a, ?to) -> complex_beginning(?to, ?b)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_complex_boundaries",
                "same_step(?c, ?to) ^ copy_without_marks(?a, ?to) ^ complex_boundaries(?a, ?b) ^ zero_step(?c, ?b0) ^ zero_step(?b, ?b0) -> complex_boundaries(?to, ?c)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_complex_ending",
                "complex_ending(?a, ?b) ^ copy_without_marks(?a, ?to) -> complex_ending(?to, ?b)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_in_complex",
                "same_step(?c, ?to) ^ copy_without_marks(?a, ?to) ^ in_complex(?a, ?b) ^ zero_step(?c, ?b0) ^ zero_step(?b, ?b0) -> in_complex(?to, ?c)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_is_function_call",
                "is_function_call(?a, ?a_fc) ^ copy_without_marks(?a, ?to) -> is_function_call(?to, ?a_fc)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_is_operand",
                "copy_without_marks(?a, ?to) ^ is_operand(?a, ?is_op) -> is_operand(?to, ?is_op)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_is_operator_with_strict_operands_order",
                "copy_without_marks(?a, ?to) ^ is_operator_with_strict_operands_order(?a, ?is_op) -> is_operator_with_strict_operands_order(?to, ?is_op)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_last",
                "last(?a, ?a_last) ^ copy_without_marks(?a, ?to) -> last(?to, ?a_last)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_prefix_postfix",
                "prefix_postfix(?a, ?a_pr) ^ copy_without_marks(?a, ?to) -> prefix_postfix(?to, ?a_pr)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_priority",
                "precedence(?a, ?a_priority) ^ copy_without_marks(?a, ?to) -> precedence(?to, ?a_priority)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_real_pos",
                "real_pos(?a, ?a_rp) ^ copy_without_marks(?a, ?to) -> real_pos(?to, ?a_rp)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_student_pos",
                "copy_without_marks(?a, ?to) ^ student_pos(?a, ?a_sp) -> student_pos(?to, ?a_sp)"
        ));
        laws.add(getLawFormulation(
                "copy_without_marks_text",
                "copy_without_marks(?a, ?to) ^ text(?a, ?a_text) -> text(?to, ?a_text)"
        ));
        laws.add(getLawFormulation(
                "describe_error",
                "swrlb:lessThan(?b_pos, ?a_pos) ^ swrlb:notEqual(?a_pos, 0) ^ student_pos(?b, ?b_pos) ^ swrlb:notEqual(?b_pos, 0) ^ before_direct(?a, ?b) ^ student_pos(?a, ?a_pos) ^ arity(?a, ?a_arity) ^ zero_step(?a, ?a) ^ zero_step(?b, ?b) -> describe_error(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "equal_priority_L_assoc",
                "swrlb:equal(?a_prior, ?b_prior) ^ swrlb:equal(?a_assoc, ?b_assoc) ^ index(?b, ?b_index) ^ precedence(?a, ?a_prior) ^ associativity(?b, ?b_assoc) ^ precedence(?b, ?b_prior) ^ associativity(?a, ?a_assoc) ^ swrlb:equal(?a_assoc, \"L\") ^ swrlb:lessThan(?a_index, ?b_index) ^ index(?a, ?a_index) ^ same_step(?a, ?b) -> high_priority_left_assoc(?a, ?b) ^ high_priority(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "equal_priority_R_assoc",
                "swrlb:equal(?a_prior, ?b_prior) ^ swrlb:equal(?a_assoc, ?b_assoc) ^ index(?b, ?b_index) ^ precedence(?a, ?a_prior) ^ associativity(?b, ?b_assoc) ^ precedence(?b, ?b_prior) ^ associativity(?a, ?a_assoc) ^ swrlb:equal(?a_assoc, \"R\") ^ index(?a, ?a_index) ^ same_step(?a, ?b) ^ swrlb:greaterThan(?a_index, ?b_index) -> high_priority(?a, ?b) ^ high_priority_right_assoc(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "eval_,_in_function_call",
                "text(?a, \",\") ^ init(?a, true) ^ in_complex(?a, ?b) ^ is_function_call(?b, true) -> app(?a, true)"
        ));
        laws.add(getLawFormulation(
                "eval_binary_operation",
                "next_step(?b, ?b_next) ^ next_step(?c, ?c_next) ^ has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"binary\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ find_right_operand(?a, ?c) ^ next_step(?a, ?a_next) ^ same_step(?a, ?c) ^ same_step(?a, ?b) -> has_operand(?a, ?c) ^ copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ app(?c_next, true) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "eval_binary_operation_copy_other",
                "has_highest_priority_to_right(?a, true) ^ arity(?a, \"binary\") ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ find_right_operand(?a, ?c) ^ find_left_operand(?a, ?b) ^ init(?a, true) ^ not_index(?b, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ all_eval_to_right(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ next_step(?a, ?a_next) ^ same_step(?a, ?c) ^ complex_boundaries(?a, ?c) -> copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ eval_step(?a, ?a_step) ^ has_complex_operator_part(?a, ?c) ^ app(?c_next, true) ^ eval(?a_next, true)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation_copy_inner_app",
                "next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ swrlb:lessThan(?a_index, ?other_index) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ app(?other, true) ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ same_step(?a, ?c) ^ index(?a, ?a_index) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation_copy_inner_eval",
                "next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ swrlb:lessThan(?a_index, ?other_index) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ same_step(?a, ?c) ^ index(?a, ?a_index) ^ eval(?other, true) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true) ^ has_operand(?a, ?other)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation_copy_other_left",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ swrlb:lessThan(?other_index, ?a_index) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ not_index(?c, ?other) ^ is_function_call(?a, false) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation_copy_other_right",
                "next_step(?c, ?c_next) ^ swrlb:greaterThan(?other_index, ?c_index) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_complex_operation_copy_others_left_no_function_name",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ find_left_operand(?a, ?d) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ swrlb:lessThan(?other_index, ?a_index) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ is_function_call(?a, true) ^ not_index(?d, ?other) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_function_name",
                "next_step(?function_name, ?function_name_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ all_eval_to_right(?a, ?b) ^ find_left_operand(?a, ?function_name) ^ same_step(?a, ?function_name) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ is_function_call(?a, true) ^ same_step(?a, ?c) ^ complex_boundaries(?a, ?c) -> copy_without_marks(?function_name, ?function_name_next) ^ app(?function_name_next, true) ^ has_complex_operator_part(?a, ?function_name)"
        ));
        laws.add(getLawFormulation(
                "eval_operand_in_complex",
                "init(?a, true) ^ in_complex(?a, ?b) ^ is_operand(?a, true) -> eval(?a, true)"
        ));
        laws.add(getLawFormulation(
                "eval_postfix_operation",
                "next_step(?b, ?b_next) ^ has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"unary\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ next_step(?a, ?a_next) ^ same_step(?a, ?b) -> copy_without_marks(?a, ?a_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "eval_postfix_operation_copy_others",
                "has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ prefix_postfix(?a, \"postfix\") ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_prefix_operation",
                "next_step(?b, ?b_next) ^ has_highest_priority_to_right(?a, true) ^ step(?a, ?a_step) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ prefix_postfix(?a, \"prefix\") ^ has_highest_priority_to_left(?a, true) ^ next_step(?a, ?a_next) ^ find_right_operand(?a, ?b) ^ same_step(?a, ?b) -> copy_without_marks(?a, ?a_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "eval_prefix_operation_copy_others",
                "has_highest_priority_to_right(?a, true) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ next_step(?other, ?other_next) ^ prefix_postfix(?a, \"prefix\") ^ same_step(?a, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ find_right_operand(?a, ?b) ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_ternary_operation",
                "arity(?a, \"ternary\") ^ next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ step(?a, ?a_step) ^ has_highest_priority_to_right(?c, true) ^ find_right_operand(?c, ?e) ^ complex_boundaries(?a, ?c) ^ find_left_operand(?a, ?d) ^ all_eval_to_right(?a, ?b) ^ next_step(?e, ?e_next) ^ init(?a, true) ^ next_step(?d, ?d_next) ^ next_step(?a, ?a_next) ^ has_highest_priority_to_left(?c, true) ^ same_step(?a, ?c) -> has_operand(?a, ?e) ^ copy_without_marks(?d, ?d_next) ^ has_operand(?a, ?d) ^ copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ copy_without_marks(?e, ?e_next) ^ eval_step(?a, ?a_step) ^ has_complex_operator_part(?a, ?c) ^ app(?c_next, true) ^ app(?d_next, true) ^ app(?e_next, true) ^ eval(?a_next, true)"
        ));
        laws.add(getLawFormulation(
                "eval_ternary_operation_copy_inner_app",
                "index(?c, ?c_index) ^ arity(?a, \"ternary\") ^ step(?a, ?a_step) ^ swrlb:lessThan(?a_index, ?other_index) ^ step(?other, ?a_step) ^ eval_step(?a, ?a_step) ^ app(?other, true) ^ next_step(?other, ?other_next) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ complex_boundaries(?a, ?c) ^ index(?a, ?a_index) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true)"
        ));
        laws.add(getLawFormulation(
                "eval_ternary_operation_copy_inner_eval",
                "index(?c, ?c_index) ^ arity(?a, \"ternary\") ^ step(?a, ?a_step) ^ swrlb:lessThan(?a_index, ?other_index) ^ step(?other, ?a_step) ^ eval_step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ complex_boundaries(?a, ?c) ^ index(?a, ?a_index) ^ eval(?other, true) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true) ^ has_operand(?a, ?other)"
        ));
        laws.add(getLawFormulation(
                "eval_ternary_operation_copy_other_left",
                "arity(?a, \"ternary\") ^ eval_step(?a, ?a_step) ^ step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ find_left_operand(?a, ?d) ^ swrlb:lessThan(?other_index, ?a_index) ^ not_index(?d, ?other) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "eval_ternary_operation_copy_other_right",
                "arity(?a, \"ternary\") ^ eval_step(?a, ?a_step) ^ step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ find_right_operand(?c, ?d) ^ swrlb:lessThan(?c_index, ?other_index) ^ not_index(?d, ?other) ^ index(?other, ?other_index) ^ index(?c, ?c_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getLawFormulation(
                "find_left_operand_eval",
                "has_highest_priority_to_right(?a, true) ^ prev_index(?b, ?c) ^ eval(?c, true) ^ has_highest_priority_to_left(?a, true) ^ all_app_to_left(?a, ?b) -> find_left_operand(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "find_left_operand_init",
                "has_highest_priority_to_right(?a, true) ^ prev_index(?b, ?c) ^ has_highest_priority_to_left(?a, true) ^ init(?c, true) ^ all_app_to_left(?a, ?b) -> find_left_operand(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "find_right_operand_eval",
                "has_highest_priority_to_right(?a, true) ^ next_index(?b, ?c) ^ all_app_to_right(?a, ?b) ^ eval(?c, true) ^ has_highest_priority_to_left(?a, true) -> find_right_operand(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "find_right_operand_init",
                "has_highest_priority_to_right(?a, true) ^ next_index(?b, ?c) ^ all_app_to_right(?a, ?b) ^ has_highest_priority_to_left(?a, true) ^ init(?c, true) -> find_right_operand(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_left",
                "more_priority_left_by_step(?a, ?b) ^ index(?b, 1) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_left_in_complex_,",
                "prev_index(?b, ?c) ^ in_complex(?a, ?c) ^ text(?a, \",\") ^ is_function_call(?c, false) ^ more_priority_left_by_step(?a, ?b) ^ has_highest_priority_to_left(?c, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_left_in_complex_not_,",
                "swrlb:notEqual(?a_text, \",\") ^ prev_index(?b, ?c) ^ in_complex(?a, ?c) ^ more_priority_left_by_step(?a, ?b) ^ has_highest_priority_to_left(?c, true) ^ text(?a, ?a_text) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_left_ternary",
                "has_highest_priority_to_left(?c, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_left(?d, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_right",
                "more_priority_right_by_step(?a, ?b) ^ last(?b, true) -> has_highest_priority_to_right(?a, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_right_in_complex",
                "next_index(?b, ?d) ^ has_highest_priority_to_right(?c, true) ^ in_complex(?a, ?c) ^ more_priority_right_by_step(?a, ?b) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_right(?a, true)"
        ));
        laws.add(getLawFormulation(
                "has_highest_priority_to_right_ternary",
                "has_highest_priority_to_right(?d, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_right(?c, true)"
        ));
        laws.add(getLawFormulation(
                "high_priority",
                "precedence(?a, ?a_prior) ^ precedence(?b, ?b_prior) ^ swrlb:lessThan(?a_prior, ?b_prior) ^ same_step(?a, ?b) -> high_priority(?a, ?b) ^ high_priority_diff_priority(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "in_complex_begin",
                "next_index(?a, ?b) ^ complex_beginning(?a, true) ^ complex_ending(?b, false) ^ step(?a, 0) -> in_complex(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "in_complex_step",
                "next_index(?a, ?b) ^ step(?a, 0) ^ complex_beginning(?a, false) ^ in_complex(?a, ?c) ^ complex_ending(?b, false) -> in_complex(?b, ?c)"
        ));
        laws.add(getLawFormulation(
                "in_complex_step_skip_inner_complex",
                "in_complex(?a, ?c) ^ complex_boundaries(?a, ?d) ^ step(?a, 0) -> in_complex(?d, ?c)"
        ));
        laws.add(getLawFormulation(
                "is_operand",
                "swrlb:notEqual(?a_text, \"sizeof\") ^ swrlb:matches(?a_text, \"[a-zA-Z_0-9]+\") ^ step(?a, 1) ^ text(?a, ?a_text) -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getLawFormulation(
                "is_operand_close_bracket",
                "step(?a, 1) ^ text(?a, \"]\") -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getLawFormulation(
                "is_operand_close_parenthesis",
                "text(?a, \")\") ^ step(?a, 1) -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getLawFormulation(
                "more_priority_left_by_step",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ high_priority(?a, ?c) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_left_by_step_app",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ app(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_left_by_step_eval",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ eval(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_left_by_step_first",
                "precedence(?a, ?a_prior) ^ init(?a, true) -> more_priority_left_by_step(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "more_priority_left_by_step_operand",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ is_operand(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_right_by_step",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ high_priority(?a, ?c) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_right_by_step_app",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_right_by_step_eval",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ eval(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "more_priority_right_by_step_first",
                "precedence(?a, ?a_prior) ^ init(?a, true) -> more_priority_right_by_step(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "more_priority_right_by_step_operand",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ is_operand(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getLawFormulation(
                "next_prev",
                "index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:add(?b_index, ?a_index, 1) ^ same_step(?a, ?b) -> next_index(?a, ?b) ^ prev_index(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "next_step",
                "index(?a, ?a_index) ^ index(?b, ?a_index) ^ step(?a, ?a_step) ^ step(?b, ?b_step) ^ swrlb:add(?b_step, ?a_step, 1) -> next_step(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "not_index",
                "index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:notEqual(?a_index, ?b_index) ^ same_step(?a, ?b) -> not_index(?b, ?a) ^ not_index(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "operator +=",
                "step(?a, 1) ^ text(?a, \"+=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator!",
                "step(?a, 1) ^ text(?a, \"!\") -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator!=",
                "step(?a, 1) ^ text(?a, \"!=\") -> precedence(?a, 10) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator%",
                "text(?a, \"%\") ^ step(?a, 1) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator%=",
                "text(?a, \"%=\") ^ step(?a, 1) -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator&",
                "text(?a, \"&\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator&&",
                "step(?a, 1) ^ text(?a, \"&&\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ precedence(?a, 14) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator&=",
                "text(?a, \"&=\") ^ step(?a, 1) -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator(",
                "text(?a, \"(\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> associativity(?a, \"L\") ^ precedence(?a, 0) ^ arity(?a, \"complex\") ^ init(?a, true) ^ complex_beginning(?a, true) ^ is_function_call(?a, false)"
        ));
        laws.add(getLawFormulation(
                "operator*=",
                "step(?a, 1) ^ text(?a, \"*=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator,",
                "step(?a, 1) ^ text(?a, \",\") -> associativity(?a, \"L\") ^ precedence(?a, 17) ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator-=",
                "step(?a, 1) ^ text(?a, \"-=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator->",
                "step(?a, 1) ^ text(?a, \"->\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator.",
                "step(?a, 1) ^ text(?a, \".\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator/",
                "step(?a, 1) ^ text(?a, \"/\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator/=",
                "step(?a, 1) ^ text(?a, \"/=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator:",
                "text(?a, \":\") ^ step(?a, 1) -> arity(?a, \"ternary\") ^ precedence(?a, 16) ^ init(?a, true) ^ complex_ending(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator::",
                "step(?a, 1) ^ text(?a, \"::\") -> associativity(?a, \"L\") ^ precedence(?a, 1) ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator<",
                "step(?a, 1) ^ text(?a, \"<\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getLawFormulation(
                "operator<<",
                "step(?a, 1) ^ text(?a, \"<<\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 7)"
        ));
        laws.add(getLawFormulation(
                "operator<<=",
                "step(?a, 1) ^ text(?a, \"<<=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator<=",
                "step(?a, 1) ^ text(?a, \"<=\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getLawFormulation(
                "operator=",
                "step(?a, 1) ^ text(?a, \"=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator==",
                "step(?a, 1) ^ text(?a, \"==\") -> precedence(?a, 10) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator>",
                "step(?a, 1) ^ text(?a, \">\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getLawFormulation(
                "operator>=",
                "step(?a, 1) ^ text(?a, \">=\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getLawFormulation(
                "operator>>",
                "step(?a, 1) ^ text(?a, \">>\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 7)"
        ));
        laws.add(getLawFormulation(
                "operator>>=",
                "step(?a, 1) ^ text(?a, \">>=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator?",
                "step(?a, 1) ^ text(?a, \"?\") -> arity(?a, \"ternary\") ^ precedence(?a, 16) ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true) ^ complex_beginning(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator^",
                "step(?a, 1) ^ text(?a, \"^\") -> precedence(?a, 12) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator^=",
                "step(?a, 1) ^ text(?a, \"^=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator_binary&",
                "text(?a, \"&\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> precedence(?a, 11) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator_binary*",
                "text(?a, \"*\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator_binary+",
                "text(?a, \"+\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 6)"
        ));
        laws.add(getLawFormulation(
                "operator_binary-",
                "step(?a, 1) ^ text(?a, \"-\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 6)"
        ));
        laws.add(getLawFormulation(
                "operator_function_call",
                "text(?a, \"(\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"complex\") ^ init(?a, true) ^ is_function_call(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator_postfix++",
                "step(?a, 1) ^ text(?a, \"++\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator_postfix--",
                "step(?a, 1) ^ text(?a, \"--\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator_prefix++",
                "step(?a, 1) ^ text(?a, \"++\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator_prefix--",
                "step(?a, 1) ^ text(?a, \"--\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator_subscript",
                "text(?a, \"[\") ^ step(?a, 1) -> associativity(?a, \"L\") ^ arity(?a, \"complex\") ^ init(?a, true) ^ complex_beginning(?a, true) ^ is_function_call(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getLawFormulation(
                "operator_unary*",
                "text(?a, \"*\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator_unary+",
                "text(?a, \"+\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator_unary-",
                "step(?a, 1) ^ text(?a, \"-\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator|",
                "step(?a, 1) ^ text(?a, \"|\") -> precedence(?a, 13) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator|=",
                "step(?a, 1) ^ text(?a, \"|=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "operator||",
                "step(?a, 1) ^ text(?a, \"||\") -> associativity(?a, \"L\") ^ precedence(?a, 15) ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true)"
        ));
        laws.add(getLawFormulation(
                "operator~",
                "step(?a, 1) ^ text(?a, \"~\") -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getLawFormulation(
                "prev_operand",
                "prev_index(?a, ?b) ^ text(?b, ?b_text) ^ is_operand(?b, true) ^ step(?b, 1) -> prev_operand(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "prev_operand_unary_postfix",
                "prev_index(?a, ?b) ^ arity(?b, \"unary\") ^ prefix_postfix(?b, \"postfix\") ^ step(?b, 1) -> prev_operand(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "prev_operation",
                "prev_index(?a, ?b) ^ arity(?b, ?b_arity) ^ swrlb:notEqual(?b_arity, \"unary\") ^ step(?b, 1) -> prev_operation(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "prev_operation_beggining",
                "step(?a, 1) ^ index(?a, 1) -> prev_operation(?a, ?a)"
        ));
        laws.add(getLawFormulation(
                "prev_operation_unary_prefix",
                "prev_index(?a, ?b) ^ arity(?b, \"unary\") ^ prefix_postfix(?b, \"prefix\") ^ step(?b, 1) -> prev_operation(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "same_step",
                "step(?a, ?a_step) ^ step(?b, ?a_step) -> same_step(?a, ?b)"
        ));
        laws.add(getLawFormulation(
                "student_error_in_complex",
                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ text(?c, \"(\") ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_in_complex_bound",
                "before_as_operand(?a, ?b) ^ complex_beginning(?b, true) ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_left_assoc",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_left_assoc(?a, ?b) -> student_error_left_assoc(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_more_priority_left",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_diff_priority(?a, ?b) ^ index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:lessThan(?a_index, ?b_index) -> student_error_more_priority_left(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_more_priority_right",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_diff_priority(?a, ?b) ^ index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:lessThan(?b_index, ?a_index) -> student_error_more_priority_right(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_right_assoc",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_right_assoc(?a, ?b) -> student_error_right_assoc(?b, ?a)"
        ));
        laws.add(getLawFormulation(
                "student_error_strict_operands_order",
                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ is_operator_with_strict_operands_order(?c, true) ^ describe_error(?a, ?b) -> student_error_strict_operands_order(?b, ?a)"
        ));

        return laws;
    }

    LawFormulation getLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = new LawFormulation();
        lawFormulation.setLaw(name);
        lawFormulation.setFormulation(formulation);
        lawFormulation.setBackend("SWRL");
        return lawFormulation;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        return null;
    }
}
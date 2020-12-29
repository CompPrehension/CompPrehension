package com.example.demo.models.businesslogic.domains;

import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.Question;
import com.example.demo.utils.HyperText;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
public class ProgrammingLanguageExpressionDomain extends Domain {
    static final String EVALUATION_ORDER_QUESTION_TYPE = "OrderOperators";

    public ProgrammingLanguageExpressionDomain() {
        name = "ProgrammingLanguageExpressionDomain";
        concepts = new ArrayList<>();
        positiveLaws = new ArrayList<>();
        negativeLaws = new ArrayList<>();

        Concept operandConcept = addConcept("operand");
        Concept simpleOperandConcept = addConcept("simple_operand");
        Concept operatorConcept = addConcept("operator", new ArrayList<>(Arrays.asList(operandConcept)));
        Concept variableConcept = addConcept("variable", new ArrayList<>(Arrays.asList(simpleOperandConcept)));
        Concept literalConcept = addConcept("literal", new ArrayList<>(Arrays.asList(simpleOperandConcept)));
        Concept precedenceConcept = addConcept("precedence");
        Concept associativityConcept = addConcept("associativity");
        Concept leftAssociativityConcept = addConcept("left_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept rightAssociativityConcept = addConcept("right_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept absentAssociativityConcept = addConcept("absent_associativity", new ArrayList<>(Arrays.asList(associativityConcept)));
        Concept arityConcept = addConcept("arity");
        Concept unaryConcept = addConcept("unary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept binaryConcept = addConcept("binary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept ternaryConcept = addConcept("ternary", new ArrayList<>(Arrays.asList(arityConcept)));
        Concept singleTokenOperatorConcept = addConcept("single_token");
        Concept twoTokenOperatorConcept = addConcept("two_token");
        Concept singleTokenUnaryConcept = addConcept("single_token_unary", new ArrayList<>(Arrays.asList(singleTokenOperatorConcept, unaryConcept)));
        Concept singleTokenBinaryConcept = addConcept("single_token_binary", new ArrayList<>(Arrays.asList(singleTokenOperatorConcept, binaryConcept)));
        Concept twoTokenUnaryConcept = addConcept("two_token_unary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, unaryConcept)));
        Concept twoTokenBinaryConcept = addConcept("two_token_binary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, binaryConcept)));
        Concept twoTokenTernaryConcept = addConcept("two_token_ternary", new ArrayList<>(Arrays.asList(twoTokenOperatorConcept, binaryConcept)));
        Concept operatorEvaluationStateConcept = addConcept("operator_evaluation_state");
        Concept operatorEvaluatingLeftOperandFirstConcept = addConcept("operator_evaluating_left_operand_first", new ArrayList<>(Arrays.asList(binaryConcept, operatorEvaluationStateConcept)));
        Concept operatorUnaryPlusConcept = addConcept("operator_unary_+", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept)));
        Concept operatorBinaryPlusConcept = addConcept("operator_binary_+", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorBinaryMultipleConcept = addConcept("operator_binary_*", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept operatorEqualsConcept = addConcept("operator_==", new ArrayList<>(Arrays.asList(singleTokenBinaryConcept)));
        Concept prefixOperatorConcept = addConcept("prefix", new ArrayList<>(Arrays.asList(unaryConcept)));
        Concept postfixOperatorConcept = addConcept("postfix", new ArrayList<>(Arrays.asList(unaryConcept)));
        Concept operatorPrefixIncrementConcept = addConcept("operator_prefix_++", new ArrayList<>(Arrays.asList(singleTokenUnaryConcept, prefixOperatorConcept)));

        Tag CppTag = new Tag();
        CppTag.setName("C++");
        Tag JavaTag = new Tag();
        JavaTag.setName("Java");

        List<Concept> singleTokenBinaryExecutionConcepts = new ArrayList<>(Arrays.asList(
                precedenceConcept,
                associativityConcept,
                operatorConcept,
                singleTokenBinaryConcept,
                simpleOperandConcept,
                operatorEvaluationStateConcept));
        positiveLaws.add(new PositiveLaw(
                "single_token_binary_execution",
                getAllLaws(),
                singleTokenBinaryExecutionConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));

        List<Concept> binaryPlusAssociativityConcepts = new ArrayList<>(Arrays.asList(
                leftAssociativityConcept,
                operatorBinaryPlusConcept
        ));
        positiveLaws.add(new PositiveLaw(
                "operator_binary_+_associativity_left",
                new ArrayList<>(),
                binaryPlusAssociativityConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));
        List<Concept> binaryMultipleAssociativityConcepts = new ArrayList<>(Arrays.asList(
                leftAssociativityConcept,
                operatorBinaryMultipleConcept
        ));
        positiveLaws.add(new PositiveLaw(
                "operator_binary_*_associativity_left",
                new ArrayList<>(),
                binaryMultipleAssociativityConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));
        List<Concept> unaryPlusAssociativityConcepts = new ArrayList<>(Arrays.asList(
                rightAssociativityConcept,
                operatorBinaryPlusConcept
        ));
        positiveLaws.add(new PositiveLaw(
                "operator_unary_+_associativity_right",
                new ArrayList<>(),
                unaryPlusAssociativityConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));
        List<Concept> mulHigherPlusPrecedenceConcepts = new ArrayList<>(Arrays.asList(
                operatorBinaryMultipleConcept,
                operatorBinaryPlusConcept,
                precedenceConcept
        ));
        positiveLaws.add(new PositiveLaw(
                "precedence_binary_*_higher_binary_+",
                new ArrayList<>(),
                mulHigherPlusPrecedenceConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));
        List<Concept> unaryPlusHigherMulPrecedenceConcepts = new ArrayList<>(Arrays.asList(
                operatorBinaryMultipleConcept,
                operatorBinaryPlusConcept,
                precedenceConcept
        ));
        positiveLaws.add(new PositiveLaw(
                "precedence_unary_+_higher_binary_*",
                new ArrayList<>(),
                unaryPlusHigherMulPrecedenceConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag))
        ));


        List<Concept> errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence = new ArrayList<>(Arrays.asList(
                precedenceConcept,
                operatorConcept,
                singleTokenBinaryConcept
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_higher_precedence_left",
                getErrorLaws(),
                errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("single_token_binary_execution")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_higher_precedence_right",
                new ArrayList<>(),
                errorSingleTokenBinaryOperatorHasUnevaluatedHigherPrecedence,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("single_token_binary_execution")));

        List<Concept> errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity = new ArrayList<>(Arrays.asList(
                associativityConcept,
                operatorConcept,
                singleTokenBinaryConcept
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left",
                getLeftAssocErrorLaws(),
                errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("single_token_binary_execution")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right",
                new ArrayList<>(),
                errorSingleTokenBinaryOperatorHasUnevaluatedAssociativity,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("single_token_binary_execution")
        ));

        List<Concept> errorNotLeftAssociativityBinaryPlus = new ArrayList<>(Arrays.asList(
                associativityConcept,
                operatorBinaryPlusConcept
        ));
        negativeLaws.add(new NegativeLaw(
                "error_binary_+_right_associativity",
                new ArrayList<>(),
                errorNotLeftAssociativityBinaryPlus,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("operator_binary_+_associativity_left")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_binary_+_absent_associativity",
                new ArrayList<>(),
                errorNotLeftAssociativityBinaryPlus,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("operator_binary_+_associativity_left")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_precedence_binary_*_less_binary_+",
                new ArrayList<>(),
                mulHigherPlusPrecedenceConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("precedence_binary_*_higher_binary_+")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_precedence_binary_*_equal_binary_+",
                new ArrayList<>(),
                mulHigherPlusPrecedenceConcepts,
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
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
    public com.example.demo.models.businesslogic.Question makeQuestion(QuestionRequest questionRequest, Language userLanguage) {
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
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "+ between b and c", "operator_binary_+", getName(0, 4)))));
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "+", "b", "+", "c"))));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        } else if (conceptNames.contains("precedence") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*") &&
                deniedConceptNames.contains("associativity")) {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("a + b * c");
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "+", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "*", "operator_binary_*", getName(0, 4))
            )));
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "+", "b", "*", "c"))));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        }  else if (conceptNames.contains("precedence") &&
                conceptNames.contains("associativity") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*")) {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("a + b + c * d");
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "+ between c and d", "operator_binary_+", getName(0, 4)),
                    getAnswerObject(question, "*", "operator_binary_*", getName(0, 6))
            )));
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "+", "b", "+", "c", "*", "d"))));
            question.setQuestionType(QuestionType.ORDER);
            return new Ordering(question);
        } else {
            com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
            question.setQuestionText("Choose associativity of operator binary +");
            question.setQuestionType(QuestionType.SINGLE_CHOICE);
            question.setQuestionDomainType("ChooseAssociativity");
            question.setAreAnswersRequireContext(true);
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "left", "left_associativity", "left"),
                    getAnswerObject(question, "right", "right_associativity", "right"),
                    getAnswerObject(question, "no associativity", "absent_associativity", "no associativity")
            )));
            return new SingleChoice(question);
        }
    }

    String getName(int step, int index) {
        return "op__" + step + "__" + index;
    }

    AnswerObject getAnswerObject(com.example.demo.models.entities.Question question, String text, String concept, String domainInfo) {
        AnswerObject answerObject = new AnswerObject();
        answerObject.setHyperText(text);
        answerObject.setRightCol(false);
        answerObject.setDomainInfo(domainInfo);
        answerObject.setConcept(concept);
        answerObject.setQuestion(question);
        return answerObject;
    }

    public List<BackendFact> getBackendFacts(List<String> expression) {
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
            facts.add(new BackendFact("owl:NamedIndividual", getName(0, index), "complex_beginning", "xsd:boolean", Boolean.toString(token.equals("(") || token.equals("[") || token.equals("?"))));
            facts.add(new BackendFact("owl:NamedIndividual", getName(0, index), "complex_ending", "xsd:boolean", Boolean.toString(token.equals(")") || token.equals("]") || token.equals(":"))));
        }
        facts.add(new BackendFact("owl:NamedIndividual", getName(0, index), "last", "xsd:boolean", "true"));
        return facts;
    }

    List<LawFormulation> getSWRLBackendBaseLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getOWLLawFormulation("zero_step", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("all_app_to_left", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("all_app_to_right", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("all_eval_to_right", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("ast_edge", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_all_operands", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_as_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_by_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_direct", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("complex_boundaries", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("copy", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("copy_without_marks", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("describe_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("find_left_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("find_right_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("has_complex_operator_part", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("has_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_diff_priority", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_right_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("in_complex", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("more_priority_left_by_step", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("more_priority_right_by_step", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("next_index", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("next_step", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("not_index", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("prev_index", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("prev_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("prev_operation", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("same_step", "owl:ObjectProperty"));

        laws.add(getOWLLawFormulation("app", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("arity", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("associativity", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("complex_beginning", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("complex_ending", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("error_description", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("eval", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("eval_step", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("has_highest_priority_to_left", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("has_highest_priority_to_right", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("index", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("init", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("is_function_call", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("is_operand", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("is_operator_with_strict_operands_order", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("last", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("precedence", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("prefix_postfix", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("real_pos", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("step", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("student_pos", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("text", "owl:DatatypeProperty"));

        return laws;
    }

    public List<LawFormulation> getAllLaws() {
        List<LawFormulation> laws = getSWRLBackendBaseLaws();
        laws.add(getSWRLLawFormulation(
                "zero_step",
                "index(?a, ?a_index) ^ index(?b, ?a_index) ^ step(?b, 0) -> zero_step(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_app_to_left",
                "all_app_to_left(?a, ?b) ^ prev_index(?b, ?c) ^ app(?c, true) -> all_app_to_left(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_app_to_left_begin",
                "init(?a, true) ^ has_highest_priority_to_left(?a, true) -> all_app_to_left(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_app_to_right",
                "all_app_to_right(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> all_app_to_right(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_app_to_right_begin",
                "has_highest_priority_to_right(?a, true) ^ init(?a, true) -> all_app_to_right(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_eval_to_right",
                "all_eval_to_right(?a, ?b) ^ next_index(?b, ?c) ^ eval(?c, true) -> all_eval_to_right(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_eval_to_right_app",
                "all_eval_to_right(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> all_eval_to_right(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "all_eval_to_right_begin",
                "has_highest_priority_to_right(?a, true) ^ init(?a, true) ^ complex_beginning(?a, true) ^ has_highest_priority_to_left(?a, true) -> all_eval_to_right(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "ast_edge_has_complex_operator_part",
                "has_complex_operator_part(?a, ?b) -> ast_edge(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "ast_edge_has_operand",
                "has_operand(?a, ?b) -> ast_edge(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "before",
                "has_operand(?a, ?b) ^ text(?b, ?b_text) ^ swrlb:notEqual(?b_text, \"(\") -> before_direct(?b, ?a) ^ before_as_operand(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_all_operands",
                "before_all_operands(?a, ?b) ^ has_operand(?b, ?c) -> before_direct(?a, ?c) ^ before_by_third_operator(?a, ?c) ^ before_all_operands(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_before",
                "before(?a, ?b) ^ before(?b, ?c) -> before(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_direct",
                "before_direct(?a, ?b) -> before(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_function_call",
                "has_operand(?a, ?b) ^ text(?b, \"(\") ^ is_function_call(?b, true) -> before_direct(?b, ?a) ^ before_as_operand(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_in_complex",
                "has_operand(?a, ?b) ^ text(?b, \"(\") ^ has_operand(?b, ?c) -> before_direct(?c, ?a) ^ before_by_third_operator(?c, ?a) ^ before_third_operator(?c, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_strict_order_operands",
                "is_operator_with_strict_operands_order(?a, true) ^ text(?a, ?a_text) ^ swrlb:notEqual(?a_text, \"?\") ^ has_operand(?a, ?b) ^ has_operand(?a, ?c) ^ index(?b, ?b_index) ^ index(?c, ?c_index) ^ swrlb:lessThan(?b_index, ?c_index) -> before_direct(?b, ?c) ^ before_all_operands(?b, ?c) ^ before_by_third_operator(?b, ?c) ^ before_third_operator(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "before_strict_order_operands_ternary",
                "text(?a, \"?\") ^ has_operand(?a, ?b) ^ has_operand(?a, ?c) ^ has_operand(?a, ?d) ^ index(?b, ?b_index) ^ index(?c, ?c_index) ^ index(?d, ?d_index) ^ not_index(?c, ?d) ^ swrlb:lessThan(?b_index, ?c_index) ^ swrlb:lessThan(?b_index, ?d_index) -> before_direct(?b, ?c) ^ before_all_operands(?b, ?c) ^ before_by_third_operator(?b, ?c) ^ before_third_operator(?b, ?a)"
        ));
//        laws.add(getLawFormulation(
//                "complex_beggining_false",
//                "swrlb:notEqual(?a_text, \"(\") ^ swrlb:notEqual(?a_text, \"[\") ^ swrlb:notEqual(?a_text, \"?\") ^ text(?a, ?a_text) ^ step(?a, 0) -> complex_beginning(?a, false)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_beginning(",
//                "text(?a, \"(\") ^ step(?a, 0) -> complex_beginning(?a, true)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_beginning?",
//                "text(?a, \"?\") ^ step(?a, 0) -> complex_beginning(?a, true)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_beginning[",
//                "text(?a, \"[\") ^ step(?a, 0) -> complex_beginning(?a, true)"
//        ));
        laws.add(getSWRLLawFormulation(
                "complex_boundaries",
                "in_complex(?a, ?c) ^ next_index(?a, ?b) ^ complex_beginning(?a, false) ^ complex_ending(?b, true) ^ step(?a, 0) -> complex_boundaries(?c, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "complex_boundaries_empty",
                "next_index(?a, ?b) ^ step(?a, 0) ^ complex_beginning(?a, true) ^ complex_ending(?b, true) -> complex_boundaries(?a, ?b)"
        ));
//        laws.add(getLawFormulation(
//                "complex_ending)",
//                "text(?a, \")\") ^ step(?a, 0) -> complex_ending(?a, true)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_ending:",
//                "text(?a, \":\") ^ step(?a, 0) -> complex_ending(?a, true)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_ending]",
//                "text(?a, \"]\") ^ step(?a, 0) -> complex_ending(?a, true)"
//        ));
//        laws.add(getLawFormulation(
//                "complex_ending_false",
//                "text(?a, ?a_text) ^ swrlb:notEqual(?a_text, \")\") ^ swrlb:notEqual(?a_text, \"]\") ^ swrlb:notEqual(?a_text, \":\") ^ step(?a, 0) -> complex_ending(?a, false)"
//        ));
        laws.add(getSWRLLawFormulation(
                "copy_app",
                "copy(?a, ?to) ^ app(?a, true) -> app(?to, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_eval",
                "copy(?a, ?to) ^ eval(?a, true) -> eval(?to, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_eval_step_to_zero_step",
                "eval_step(?a, ?a_step) ^ zero_step(?a, ?a0) -> eval_step(?a0, ?a_step)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_has_complex_operator_part_to_zero_step",
                "has_complex_operator_part(?a, ?b) ^ zero_step(?a, ?a0) ^ zero_step(?b, ?b0) -> has_complex_operator_part(?a0, ?b0)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_has_operand_to_zero_step",
                "has_operand(?a, ?b) ^ zero_step(?a, ?a0) ^ zero_step(?b, ?b0) -> has_operand(?a0, ?b0)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_init",
                "copy(?a, ?to) ^ init(?a, true) -> init(?to, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_to_zero_step",
                "step(?a, 0) ^ step(?b, 1) ^ zero_step(?b, ?a) -> copy_without_marks(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_to_1_step",
                "step(?a, 0) ^ step(?b, 1) ^ zero_step(?b, ?a) -> copy(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks",
                "copy(?a, ?to) -> copy_without_marks(?a, ?to)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_arity",
                "arity(?a, ?a_arity) ^ copy_without_marks(?a, ?to) -> arity(?to, ?a_arity)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_associativity",
                "associativity(?a, ?a_associativity) ^ copy_without_marks(?a, ?to) -> associativity(?to, ?a_associativity)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_complex_beginning",
                "complex_beginning(?a, ?b) ^ copy_without_marks(?a, ?to) -> complex_beginning(?to, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_complex_boundaries",
                "same_step(?c, ?to) ^ copy_without_marks(?a, ?to) ^ complex_boundaries(?a, ?b) ^ zero_step(?c, ?b0) ^ zero_step(?b, ?b0) -> complex_boundaries(?to, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_complex_ending",
                "complex_ending(?a, ?b) ^ copy_without_marks(?a, ?to) -> complex_ending(?to, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_in_complex",
                "same_step(?c, ?to) ^ copy_without_marks(?a, ?to) ^ in_complex(?a, ?b) ^ zero_step(?c, ?b0) ^ zero_step(?b, ?b0) -> in_complex(?to, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_is_function_call",
                "is_function_call(?a, ?a_fc) ^ copy_without_marks(?a, ?to) -> is_function_call(?to, ?a_fc)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_is_operand",
                "copy_without_marks(?a, ?to) ^ is_operand(?a, ?is_op) -> is_operand(?to, ?is_op)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_is_operator_with_strict_operands_order",
                "copy_without_marks(?a, ?to) ^ is_operator_with_strict_operands_order(?a, ?is_op) -> is_operator_with_strict_operands_order(?to, ?is_op)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_last",
                "last(?a, ?a_last) ^ copy_without_marks(?a, ?to) -> last(?to, ?a_last)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_prefix_postfix",
                "prefix_postfix(?a, ?a_pr) ^ copy_without_marks(?a, ?to) -> prefix_postfix(?to, ?a_pr)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_priority",
                "precedence(?a, ?a_priority) ^ copy_without_marks(?a, ?to) -> precedence(?to, ?a_priority)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_real_pos",
                "real_pos(?a, ?a_rp) ^ copy_without_marks(?a, ?to) -> real_pos(?to, ?a_rp)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_student_pos",
                "copy_without_marks(?a, ?to) ^ student_pos(?a, ?a_sp) -> student_pos(?to, ?a_sp)"
        ));
        laws.add(getSWRLLawFormulation(
                "copy_without_marks_text",
                "copy_without_marks(?a, ?to) ^ text(?a, ?a_text) -> text(?to, ?a_text)"
        ));
        laws.add(getSWRLLawFormulation(
                "equal_priority_L_assoc",
                "swrlb:equal(?a_prior, ?b_prior) ^ swrlb:equal(?a_assoc, ?b_assoc) ^ index(?b, ?b_index) ^ precedence(?a, ?a_prior) ^ associativity(?b, ?b_assoc) ^ precedence(?b, ?b_prior) ^ associativity(?a, ?a_assoc) ^ swrlb:equal(?a_assoc, \"L\") ^ swrlb:lessThan(?a_index, ?b_index) ^ index(?a, ?a_index) ^ same_step(?a, ?b) -> high_priority_left_assoc(?a, ?b) ^ high_priority(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "equal_priority_R_assoc",
                "swrlb:equal(?a_prior, ?b_prior) ^ swrlb:equal(?a_assoc, ?b_assoc) ^ index(?b, ?b_index) ^ precedence(?a, ?a_prior) ^ associativity(?b, ?b_assoc) ^ precedence(?b, ?b_prior) ^ associativity(?a, ?a_assoc) ^ swrlb:equal(?a_assoc, \"R\") ^ index(?a, ?a_index) ^ same_step(?a, ?b) ^ swrlb:greaterThan(?a_index, ?b_index) -> high_priority(?a, ?b) ^ high_priority_right_assoc(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_,_in_function_call",
                "text(?a, \",\") ^ init(?a, true) ^ in_complex(?a, ?b) ^ is_function_call(?b, true) -> app(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_binary_operation",
                "next_step(?b, ?b_next) ^ next_step(?c, ?c_next) ^ has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"binary\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ find_right_operand(?a, ?c) ^ next_step(?a, ?a_next) ^ same_step(?a, ?c) ^ same_step(?a, ?b) -> has_operand(?a, ?c) ^ copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ app(?c_next, true) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_binary_operation_copy_other",
                "has_highest_priority_to_right(?a, true) ^ arity(?a, \"binary\") ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ find_right_operand(?a, ?c) ^ find_left_operand(?a, ?b) ^ init(?a, true) ^ not_index(?b, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ all_eval_to_right(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ next_step(?a, ?a_next) ^ same_step(?a, ?c) ^ complex_boundaries(?a, ?c) -> copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ eval_step(?a, ?a_step) ^ has_complex_operator_part(?a, ?c) ^ app(?c_next, true) ^ eval(?a_next, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation_copy_inner_app",
                "next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ swrlb:lessThan(?a_index, ?other_index) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ app(?other, true) ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ same_step(?a, ?c) ^ index(?a, ?a_index) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation_copy_inner_eval",
                "next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ swrlb:lessThan(?a_index, ?other_index) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ not_index(?c, ?other) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ same_step(?a, ?c) ^ index(?a, ?a_index) ^ eval(?other, true) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true) ^ has_operand(?a, ?other)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation_copy_other_left",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ swrlb:lessThan(?other_index, ?a_index) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ not_index(?c, ?other) ^ is_function_call(?a, false) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation_copy_other_right",
                "next_step(?c, ?c_next) ^ swrlb:greaterThan(?other_index, ?c_index) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_complex_operation_copy_others_left_no_function_name",
                "next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ index(?c, ?c_index) ^ find_left_operand(?a, ?d) ^ all_eval_to_right(?a, ?b) ^ arity(?a, \"complex\") ^ init(?a, true) ^ swrlb:lessThan(?other_index, ?a_index) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ next_step(?a, ?a_next) ^ is_function_call(?a, true) ^ not_index(?d, ?other) ^ not_index(?c, ?other) ^ same_step(?a, ?c) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_function_name",
                "next_step(?function_name, ?function_name_next) ^ next_index(?b, ?c) ^ has_highest_priority_to_right(?a, true) ^ all_eval_to_right(?a, ?b) ^ find_left_operand(?a, ?function_name) ^ same_step(?a, ?function_name) ^ arity(?a, \"complex\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ is_function_call(?a, true) ^ same_step(?a, ?c) ^ complex_boundaries(?a, ?c) -> copy_without_marks(?function_name, ?function_name_next) ^ app(?function_name_next, true) ^ has_complex_operator_part(?a, ?function_name)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_operand_in_complex",
                "init(?a, true) ^ in_complex(?a, ?b) ^ is_operand(?a, true) -> eval(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_postfix_operation",
                "next_step(?b, ?b_next) ^ has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ step(?a, ?a_step) ^ arity(?a, \"unary\") ^ init(?a, true) ^ has_highest_priority_to_left(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ next_step(?a, ?a_next) ^ same_step(?a, ?b) -> copy_without_marks(?a, ?a_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_postfix_operation_copy_others",
                "has_highest_priority_to_right(?a, true) ^ find_left_operand(?a, ?b) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ prefix_postfix(?a, \"postfix\") ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_prefix_operation",
                "next_step(?b, ?b_next) ^ has_highest_priority_to_right(?a, true) ^ step(?a, ?a_step) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ prefix_postfix(?a, \"prefix\") ^ has_highest_priority_to_left(?a, true) ^ next_step(?a, ?a_next) ^ find_right_operand(?a, ?b) ^ same_step(?a, ?b) -> copy_without_marks(?a, ?a_next) ^ app(?b_next, true) ^ eval_step(?a, ?a_step) ^ eval(?a_next, true) ^ copy_without_marks(?b, ?b_next) ^ has_operand(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_prefix_operation_copy_others",
                "has_highest_priority_to_right(?a, true) ^ arity(?a, \"unary\") ^ init(?a, true) ^ not_index(?b, ?other) ^ next_step(?other, ?other_next) ^ prefix_postfix(?a, \"prefix\") ^ same_step(?a, ?other) ^ has_highest_priority_to_left(?a, true) ^ not_index(?a, ?other) ^ find_right_operand(?a, ?b) ^ same_step(?a, ?b) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_ternary_operation",
                "arity(?a, \"ternary\") ^ next_step(?c, ?c_next) ^ next_index(?b, ?c) ^ step(?a, ?a_step) ^ has_highest_priority_to_right(?c, true) ^ find_right_operand(?c, ?e) ^ complex_boundaries(?a, ?c) ^ find_left_operand(?a, ?d) ^ all_eval_to_right(?a, ?b) ^ next_step(?e, ?e_next) ^ init(?a, true) ^ next_step(?d, ?d_next) ^ next_step(?a, ?a_next) ^ has_highest_priority_to_left(?c, true) ^ same_step(?a, ?c) -> has_operand(?a, ?e) ^ copy_without_marks(?d, ?d_next) ^ has_operand(?a, ?d) ^ copy_without_marks(?a, ?a_next) ^ copy_without_marks(?c, ?c_next) ^ copy_without_marks(?e, ?e_next) ^ eval_step(?a, ?a_step) ^ has_complex_operator_part(?a, ?c) ^ app(?c_next, true) ^ app(?d_next, true) ^ app(?e_next, true) ^ eval(?a_next, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_ternary_operation_copy_inner_app",
                "index(?c, ?c_index) ^ arity(?a, \"ternary\") ^ step(?a, ?a_step) ^ swrlb:lessThan(?a_index, ?other_index) ^ step(?other, ?a_step) ^ eval_step(?a, ?a_step) ^ app(?other, true) ^ next_step(?other, ?other_next) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ complex_boundaries(?a, ?c) ^ index(?a, ?a_index) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_ternary_operation_copy_inner_eval",
                "index(?c, ?c_index) ^ arity(?a, \"ternary\") ^ step(?a, ?a_step) ^ swrlb:lessThan(?a_index, ?other_index) ^ step(?other, ?a_step) ^ eval_step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ index(?other, ?other_index) ^ swrlb:lessThan(?other_index, ?c_index) ^ complex_boundaries(?a, ?c) ^ index(?a, ?a_index) ^ eval(?other, true) -> copy_without_marks(?other, ?other_next) ^ app(?other_next, true) ^ has_operand(?a, ?other)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_ternary_operation_copy_other_left",
                "arity(?a, \"ternary\") ^ eval_step(?a, ?a_step) ^ step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ find_left_operand(?a, ?d) ^ swrlb:lessThan(?other_index, ?a_index) ^ not_index(?d, ?other) ^ index(?other, ?other_index) ^ index(?a, ?a_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "eval_ternary_operation_copy_other_right",
                "arity(?a, \"ternary\") ^ eval_step(?a, ?a_step) ^ step(?a, ?a_step) ^ next_step(?other, ?other_next) ^ same_step(?a, ?other) ^ complex_boundaries(?a, ?c) ^ find_right_operand(?c, ?d) ^ swrlb:lessThan(?c_index, ?other_index) ^ not_index(?d, ?other) ^ index(?other, ?other_index) ^ index(?c, ?c_index) -> copy(?other, ?other_next)"
        ));
        laws.add(getSWRLLawFormulation(
                "find_left_operand_eval",
                "has_highest_priority_to_right(?a, true) ^ prev_index(?b, ?c) ^ eval(?c, true) ^ has_highest_priority_to_left(?a, true) ^ all_app_to_left(?a, ?b) -> find_left_operand(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "find_left_operand_init",
                "has_highest_priority_to_right(?a, true) ^ prev_index(?b, ?c) ^ has_highest_priority_to_left(?a, true) ^ init(?c, true) ^ all_app_to_left(?a, ?b) -> find_left_operand(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "find_right_operand_eval",
                "has_highest_priority_to_right(?a, true) ^ next_index(?b, ?c) ^ all_app_to_right(?a, ?b) ^ eval(?c, true) ^ has_highest_priority_to_left(?a, true) -> find_right_operand(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "find_right_operand_init",
                "has_highest_priority_to_right(?a, true) ^ next_index(?b, ?c) ^ all_app_to_right(?a, ?b) ^ has_highest_priority_to_left(?a, true) ^ init(?c, true) -> find_right_operand(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_left",
                "more_priority_left_by_step(?a, ?b) ^ index(?b, 1) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_left_in_complex_,",
                "prev_index(?b, ?c) ^ in_complex(?a, ?c) ^ text(?a, \",\") ^ is_function_call(?c, false) ^ more_priority_left_by_step(?a, ?b) ^ has_highest_priority_to_left(?c, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_left_in_complex_not_,",
                "swrlb:notEqual(?a_text, \",\") ^ prev_index(?b, ?c) ^ in_complex(?a, ?c) ^ more_priority_left_by_step(?a, ?b) ^ has_highest_priority_to_left(?c, true) ^ text(?a, ?a_text) -> has_highest_priority_to_left(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_left_ternary",
                "has_highest_priority_to_left(?c, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_left(?d, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_right",
                "more_priority_right_by_step(?a, ?b) ^ last(?b, true) -> has_highest_priority_to_right(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_right_in_complex",
                "next_index(?b, ?d) ^ has_highest_priority_to_right(?c, true) ^ in_complex(?a, ?c) ^ more_priority_right_by_step(?a, ?b) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_right(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "has_highest_priority_to_right_ternary",
                "has_highest_priority_to_right(?d, true) ^ complex_boundaries(?c, ?d) -> has_highest_priority_to_right(?c, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "high_priority",
                "precedence(?a, ?a_prior) ^ precedence(?b, ?b_prior) ^ swrlb:lessThan(?a_prior, ?b_prior) ^ same_step(?a, ?b) -> high_priority(?a, ?b) ^ high_priority_diff_priority(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "in_complex_begin",
                "next_index(?a, ?b) ^ complex_beginning(?a, true) ^ complex_ending(?b, false) ^ step(?a, 0) -> in_complex(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "in_complex_step",
                "next_index(?a, ?b) ^ step(?a, 0) ^ complex_beginning(?a, false) ^ in_complex(?a, ?c) ^ complex_ending(?b, false) -> in_complex(?b, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "in_complex_step_skip_inner_complex",
                "in_complex(?a, ?c) ^ complex_boundaries(?a, ?d) ^ step(?a, 0) -> in_complex(?d, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "is_operand",
                "swrlb:notEqual(?a_text, \"sizeof\") ^ swrlb:matches(?a_text, \"[a-zA-Z_0-9]+\") ^ step(?a, 1) ^ text(?a, ?a_text) -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "is_operand_close_bracket",
                "step(?a, 1) ^ text(?a, \"]\") -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "is_operand_close_parenthesis",
                "text(?a, \")\") ^ step(?a, 1) -> init(?a, true) ^ is_operand(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_left_by_step",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ high_priority(?a, ?c) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_left_by_step_app",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ app(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_left_by_step_eval",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ eval(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_left_by_step_first",
                "precedence(?a, ?a_prior) ^ init(?a, true) -> more_priority_left_by_step(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_left_by_step_operand",
                "more_priority_left_by_step(?a, ?b) ^ prev_index(?b, ?c) ^ is_operand(?c, true) -> more_priority_left_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_right_by_step",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ high_priority(?a, ?c) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_right_by_step_app",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ app(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_right_by_step_eval",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ eval(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_right_by_step_first",
                "precedence(?a, ?a_prior) ^ init(?a, true) -> more_priority_right_by_step(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "more_priority_right_by_step_operand",
                "more_priority_right_by_step(?a, ?b) ^ next_index(?b, ?c) ^ is_operand(?c, true) -> more_priority_right_by_step(?a, ?c)"
        ));
        laws.add(getSWRLLawFormulation(
                "next_prev",
                "index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:add(?b_index, ?a_index, 1) ^ same_step(?a, ?b) -> next_index(?a, ?b) ^ prev_index(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "next_step",
                "index(?a, ?a_index) ^ index(?b, ?a_index) ^ step(?a, ?a_step) ^ step(?b, ?b_step) ^ swrlb:add(?b_step, ?a_step, 1) -> next_step(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "not_index",
                "index(?a, ?a_index) ^ index(?b, ?b_index) ^ swrlb:notEqual(?a_index, ?b_index) ^ same_step(?a, ?b) -> not_index(?b, ?a) ^ not_index(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator +=",
                "step(?a, 1) ^ text(?a, \"+=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator!",
                "step(?a, 1) ^ text(?a, \"!\") -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator!=",
                "step(?a, 1) ^ text(?a, \"!=\") -> precedence(?a, 10) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator%",
                "text(?a, \"%\") ^ step(?a, 1) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator%=",
                "text(?a, \"%=\") ^ step(?a, 1) -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator&",
                "text(?a, \"&\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator&&",
                "step(?a, 1) ^ text(?a, \"&&\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ precedence(?a, 14) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator&=",
                "text(?a, \"&=\") ^ step(?a, 1) -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator(",
                "text(?a, \"(\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> associativity(?a, \"L\") ^ precedence(?a, 0) ^ arity(?a, \"complex\") ^ init(?a, true) ^ complex_beginning(?a, true) ^ is_function_call(?a, false)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator*=",
                "step(?a, 1) ^ text(?a, \"*=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator,",
                "step(?a, 1) ^ text(?a, \",\") -> associativity(?a, \"L\") ^ precedence(?a, 17) ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator-=",
                "step(?a, 1) ^ text(?a, \"-=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator->",
                "step(?a, 1) ^ text(?a, \"->\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator.",
                "step(?a, 1) ^ text(?a, \".\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator/",
                "step(?a, 1) ^ text(?a, \"/\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator/=",
                "step(?a, 1) ^ text(?a, \"/=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator:",
                "text(?a, \":\") ^ step(?a, 1) -> arity(?a, \"ternary\") ^ precedence(?a, 16) ^ init(?a, true) ^ complex_ending(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator::",
                "step(?a, 1) ^ text(?a, \"::\") -> associativity(?a, \"L\") ^ precedence(?a, 1) ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator<",
                "step(?a, 1) ^ text(?a, \"<\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator<<",
                "step(?a, 1) ^ text(?a, \"<<\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 7)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator<<=",
                "step(?a, 1) ^ text(?a, \"<<=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator<=",
                "step(?a, 1) ^ text(?a, \"<=\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator=",
                "step(?a, 1) ^ text(?a, \"=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator==",
                "step(?a, 1) ^ text(?a, \"==\") -> precedence(?a, 10) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator>",
                "step(?a, 1) ^ text(?a, \">\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator>=",
                "step(?a, 1) ^ text(?a, \">=\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 9)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator>>",
                "step(?a, 1) ^ text(?a, \">>\") -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 7)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator>>=",
                "step(?a, 1) ^ text(?a, \">>=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator?",
                "step(?a, 1) ^ text(?a, \"?\") -> arity(?a, \"ternary\") ^ precedence(?a, 16) ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true) ^ complex_beginning(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator^",
                "step(?a, 1) ^ text(?a, \"^\") -> precedence(?a, 12) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator^=",
                "step(?a, 1) ^ text(?a, \"^=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_binary&",
                "text(?a, \"&\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> precedence(?a, 11) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_binary*",
                "text(?a, \"*\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ precedence(?a, 5) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_binary+",
                "text(?a, \"+\") ^ step(?a, 1) ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 6)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_binary-",
                "step(?a, 1) ^ text(?a, \"-\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true) ^ precedence(?a, 6)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_function_call",
                "text(?a, \"(\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"complex\") ^ init(?a, true) ^ is_function_call(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_postfix++",
                "step(?a, 1) ^ text(?a, \"++\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_postfix--",
                "step(?a, 1) ^ text(?a, \"--\") ^ prev_operand(?a, ?b) -> associativity(?a, \"L\") ^ arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"postfix\") ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_prefix++",
                "step(?a, 1) ^ text(?a, \"++\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_prefix--",
                "step(?a, 1) ^ text(?a, \"--\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_subscript",
                "text(?a, \"[\") ^ step(?a, 1) -> associativity(?a, \"L\") ^ arity(?a, \"complex\") ^ init(?a, true) ^ complex_beginning(?a, true) ^ is_function_call(?a, true) ^ precedence(?a, 2)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_unary*",
                "text(?a, \"*\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_unary+",
                "text(?a, \"+\") ^ step(?a, 1) ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator_unary-",
                "step(?a, 1) ^ text(?a, \"-\") ^ prev_operation(?a, ?b) -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator|",
                "step(?a, 1) ^ text(?a, \"|\") -> precedence(?a, 13) ^ associativity(?a, \"L\") ^ arity(?a, \"binary\") ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator|=",
                "step(?a, 1) ^ text(?a, \"|=\") -> precedence(?a, 16) ^ arity(?a, \"binary\") ^ init(?a, true) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "operator||",
                "step(?a, 1) ^ text(?a, \"||\") -> associativity(?a, \"L\") ^ precedence(?a, 15) ^ arity(?a, \"binary\") ^ is_operator_with_strict_operands_order(?a, true) ^ init(?a, true)"
        ));
        laws.add(getSWRLLawFormulation(
                "operator~",
                "step(?a, 1) ^ text(?a, \"~\") -> arity(?a, \"unary\") ^ init(?a, true) ^ prefix_postfix(?a, \"prefix\") ^ precedence(?a, 3) ^ associativity(?a, \"R\")"
        ));
        laws.add(getSWRLLawFormulation(
                "prev_operand",
                "prev_index(?a, ?b) ^ text(?b, ?b_text) ^ is_operand(?b, true) ^ step(?b, 1) -> prev_operand(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "prev_operand_unary_postfix",
                "prev_index(?a, ?b) ^ arity(?b, \"unary\") ^ prefix_postfix(?b, \"postfix\") ^ step(?b, 1) -> prev_operand(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "prev_operation",
                "prev_index(?a, ?b) ^ arity(?b, ?b_arity) ^ swrlb:notEqual(?b_arity, \"unary\") ^ step(?b, 1) -> prev_operation(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "prev_operation_beggining",
                "step(?a, 1) ^ index(?a, 1) -> prev_operation(?a, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "prev_operation_unary_prefix",
                "prev_index(?a, ?b) ^ arity(?b, \"unary\") ^ prefix_postfix(?b, \"prefix\") ^ step(?b, 1) -> prev_operation(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "same_step",
                "step(?a, ?a_step) ^ step(?b, ?a_step) -> same_step(?a, ?b)"
        ));

        return laws;
    }

    public List<LawFormulation> getErrorLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getOWLLawFormulation("before_as_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_by_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_direct", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("complex_beginning", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("describe_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_diff_priority", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_right_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("is_operator_with_strict_operands_order", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("student_pos_less", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_equal_priority", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_in_complex", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_priority", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_priority_left", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_priority_right", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_right_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_strict_operands_order", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("text", "owl:DatatypeProperty"));
        laws.add(getSWRLLawFormulation(
                "describe_error",
                "student_pos_less(?b, ?a) ^ before_direct(?a, ?b) -> describe_error(?a, ?b)"
        ));
        laws.add(getSWRLLawFormulation(
                "student_error_in_complex",
                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ text(?c, \"(\") ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "student_error_in_complex_bound",
                "before_as_operand(?a, ?b) ^ complex_beginning(?b, true) ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "student_error_more_priority",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_diff_priority(?a, ?b) -> student_error_more_priority(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "student_error_right_assoc",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_right_assoc(?a, ?b) -> student_error_right_assoc(?b, ?a)"
        ));
        laws.add(getSWRLLawFormulation(
                "student_error_strict_operands_order",
                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ is_operator_with_strict_operands_order(?c, true) ^ describe_error(?a, ?b) -> student_error_strict_operands_order(?b, ?a)"
        ));
        return laws;
    }

    public List<LawFormulation> getLeftAssocErrorLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getOWLLawFormulation("before_as_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("describe_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_priority_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_left_assoc", "owl:ObjectProperty"));
        laws.add(getSWRLLawFormulation(
                "student_error_left_assoc",
                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_priority_left_assoc(?a, ?b) -> student_error_left_assoc(?b, ?a)"
        ));
        return laws;
    }

    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        List<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        List<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    getPositiveLaw("single_token_binary_execution"),
                    getPositiveLaw("operator_binary_+_associativity_left"),
                    getPositiveLaw("single_token_binary_execution")
            ));
        }
        return new ArrayList<>(Arrays.asList());
    }

    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<NegativeLaw> result = new ArrayList<>(Arrays.asList(
                    getNegativeLaw("error_single_token_binary_operator_has_unevaluated_higher_precedence_left"),
                    getNegativeLaw("error_single_token_binary_operator_has_unevaluated_higher_precedence_right"),
                    getNegativeLaw("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right"),
                    getNegativeLaw("error_binary_+_right_associativity"),
                    getNegativeLaw("error_binary_+_absent_associativity"),
                    getNegativeLaw("error_precedence_binary_*_less_binary_+"),
                    getNegativeLaw("error_precedence_binary_*_equal_binary_+")
            ));
            for (Tag tag : tags) {
                if (tag.getName().equals("C++")) {
                    result.add(getNegativeLaw("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left"));
                }
            }
            return result;
        }
        return new ArrayList<>(Arrays.asList());
    }

    LawFormulation getSWRLLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = new LawFormulation();
        lawFormulation.setLaw(name);
        lawFormulation.setFormulation(formulation);
        lawFormulation.setBackend("SWRL");
        return lawFormulation;
    }

    LawFormulation getOWLLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = new LawFormulation();
        lawFormulation.setLaw(name);
        lawFormulation.setFormulation(formulation);
        lawFormulation.setBackend("OWL");
        return lawFormulation;
    }

    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFact> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "has_operand",
                    "before_direct",
                    "before_by_third_operator",
                    "before_third_operator",
                    "before_as_operand",
                    "is_operator_with_strict_operands_order",
                    "high_priority_diff_priority",
                    "high_priority_left_assoc",
                    "high_priority_right_assoc"
            ));
        }
        return new ArrayList<>();
    }

    public List<String> getViolationVerbs(String questionDomainType, List<BackendFact> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "student_error_more_priority",
                    "student_error_left_assoc",
                    "student_error_right_assoc",
                    "student_error_in_complex",
                    "student_error_strict_operands_order",
                    "text",
                    "index"
            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFact> responseToFacts(String questionDomainType, List<Response> responses, List<AnswerObject> answerObjects) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<BackendFact> result = new ArrayList<>();
            int pos = 1;
            HashSet<String> used = new HashSet<>();
            for (Response response : responses) {
                for (String earlier : used) {
                    result.add(new BackendFact(
                            "owl:NamedIndividual",
                            earlier,
                            "student_pos_less",
                            "owl:NamedIndividual",
                            response.getLeftAnswerObject().getDomainInfo()
                    ));
                }
                used.add(response.getLeftAnswerObject().getDomainInfo());
                pos = pos + 1;
            }

            for (AnswerObject answerObject : answerObjects) {
                if (!used.contains(answerObject.getDomainInfo())) {
                    for (String earlier : used) {
                        result.add(new BackendFact(
                                "owl:NamedIndividual",
                                earlier,
                                "student_pos_less",
                                "owl:NamedIndividual",
                                answerObject.getDomainInfo()
                        ));
                    }
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    static Optional<Integer> getIndexFromName(String name, boolean allowNotZeroStep) {
        Assertions.assertTrue(name.startsWith("op__"), name);
        String[] parts = name.split("__");
        assertEquals(3, parts.length, name);
        if (allowNotZeroStep || parts[1].equals("0")) {
            return Optional.of(Integer.parseInt(parts[2]));
        }
        return Optional.empty();
    }

    @Override
    public List<Mistake> interpretSentence(List<BackendFact> violations) {
        List<Mistake> mistakes = new ArrayList<>();
        Map<String, BackendFact> nameToText = new HashMap<>();
        Map<String, BackendFact> nameToPos = new HashMap<>();
        for (BackendFact violation : violations) {
            if (violation.getVerb().equals("text")) {
                nameToText.put(violation.getSubject(), violation);
            } else if (violation.getVerb().equals("index")) {
                nameToPos.put(violation.getSubject(), violation);
            }
        }

        for (BackendFact violation : violations) {
            Mistake mistake = new Mistake();
            if (violation.getVerb().equals("student_error_more_priority")) {
                if (getIndexFromName(violation.getSubject(), false).orElse(0) > getIndexFromName(violation.getObject(), false).orElse(0)) {
                    mistake.setLawName("error_single_token_binary_operator_has_unevaluated_higher_precedence_left");
                } else {
                    mistake.setLawName("error_single_token_binary_operator_has_unevaluated_higher_precedence_right");
                }
            } else if (violation.getVerb().equals("student_error_left_assoc")) {
                mistake.setLawName("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left");
            } else if (violation.getVerb().equals("student_error_right_assoc")) {
                mistake.setLawName("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right");
            }
            if (mistake.getLawName() != null) {
                mistake.setViolationFacts(new ArrayList<>(Arrays.asList(
                        violation,
                        nameToText.get(violation.getObject()),
                        nameToText.get(violation.getSubject()),
                        nameToPos.get(violation.getObject()),
                        nameToPos.get(violation.getSubject())
                )));
                mistakes.add(mistake);
            }
        }
        return mistakes;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (Mistake mistake : mistakes) {
            result.add(makeExplanation(mistake, feedbackType));
        }
        return result;
    }

    public static String getOperatorTextDescription(String errorText) {
        if (errorText.equals("(")) {
            return "parenthesis ";
        } else if (errorText.equals("[")) {
            return "brackets ";
        } else if (errorText.contains("(")) {
            return "function call ";
        }
        return "operator ";
    }

    HyperText makeExplanation(Mistake mistake, FeedbackType feedbackType) {
        BackendFact base = null;
        BackendFact third = null;
        Map<String, String> nameToText = new HashMap<>();
        Map<String, String> nameToPos = new HashMap<>();

        for (BackendFact fact : mistake.getViolationFacts()) {
            if (fact.getVerb().equals("before_third_operator")) {
                third = fact;
            } else if (fact.getVerb().equals("index")) {
                nameToPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("text")) {
                nameToText.put(fact.getSubject(), fact.getObject());
            } else {
                base = fact;
            }
        }

        String errorText = nameToText.get(base.getSubject());
        String reasonText = nameToText.get(base.getObject());

        String thirdOperatorPos = third == null ? "" : nameToPos.get(third.getObject());
        String thirdOperatorText = third == null ? "" : nameToText.get(third.getObject());

        String reasonPos = nameToPos.get(base.getObject());
        String errorPos = nameToPos.get(base.getSubject());

        String what = getOperatorTextDescription(reasonText) + reasonText + " on pos " + reasonPos
                + " should be evaluated before " + getOperatorTextDescription(errorText) + errorText + " on pos " + errorPos;
        String reason = "";

        String errorType = mistake.getLawName();

        if (errorType.equals("error_single_token_binary_operator_has_unevaluated_higher_precedence_left") ||
                errorType.equals("error_single_token_binary_operator_has_unevaluated_higher_precedence_right")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has higher precedence";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left") && errorText.equals(reasonText)) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has left associativity and is evaluated from left to right";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has the same precedence and left associativity";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right") && errorText.equals(reasonText)) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has right associativity and is evaluated from right to left";
        } else if (errorType.equals("error_single_token_binary_operator_has_unevaluated_same_precedence_right_associativity_right")) {
            reason = " because " + getOperatorTextDescription(reasonText) + reasonText + " has the same precedence and right associativity";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && errorText.equals("(")) {
//            reason = " because function arguments are evaluated before function call​";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && errorText.equals("[")) {
//            reason = " because expression in brackets is evaluated before brackets";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && thirdOperatorText.equals("(")) {
//            reason = " because expression in parenthesis is evaluated before operators​ outside of them";
//        } else if (error.Type == StudentErrorType.IN_COMPLEX && thirdOperatorText.equals("[")) {
//            reason = " because expression in brackets is evaluated before operator outside of them​​";
//        } else if (error.Type == StudentErrorType.STRICT_OPERANDS_ORDER) {
//            reason = " because the left operand of the " + getOperatorTextDescription(thirdOperatorText) + thirdOperatorText + " on pos " + thirdOperatorPos + " must be evaluated before its right operand​";
        } else {
            reason = " because unknown error";
        }

        return new HyperText(what + "\n" + reason);
    }
}

package com.example.demo.models.businesslogic.domains;

import com.example.demo.models.businesslogic.*;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import com.example.demo.models.entities.QuestionOptions.QuestionOptionsEntity;
import com.example.demo.utils.HyperText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // read domain Laws and formulations (rules) from file ...
        ClassLoader CLDR = this.getClass().getClassLoader();
        InputStream inputStream = CLDR.getResourceAsStream("com/example/demo/models/businesslogic/domains/programming-language-expression-domain-laws.json");

        RuntimeTypeAdapterFactory<Law> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Law.class, "positive")
                        .registerSubtype(PositiveLaw.class, "true")
                        .registerSubtype(NegativeLaw.class, "false");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        Law[] lawForms = gson.fromJson(
                new InputStreamReader(inputStream),
                Law[].class);

        for (Law lawForm : lawForms) {
            if (lawForm.isPositiveLaw()) {
                positiveLaws.add((PositiveLaw) lawForm);
            } else {
                negativeLaws.add((NegativeLaw) lawForm);
            }
        }

        // Make more negative Laws, also prepare concept lists for that ...
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
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList(CppTag, JavaTag)),
                getPositiveLaw("precedence_binary_*_higher_binary_+")
        ));
        negativeLaws.add(new NegativeLaw(
                "error_precedence_binary_*_equal_binary_+",
                new ArrayList<>(),
                new ArrayList<>(),
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
    public List<HyperText> getFullSolutionTrace(Question question) {
    	return null;
    }

    @Override
    public void update() {
    }

    @Override
    public ExerciseForm getExerciseForm() {
        return null;
    }

    @Override
    public ExerciseEntity processExerciseForm(ExerciseForm ef) {
        return null;
    }

    @Override
    public Question makeQuestion(QuestionRequest questionRequest, Language userLanguage) {

        // Prepare concept name sets ...
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

        QuestionOptionsEntity orderQuestionOptions = OrderQuestionOptionsEntity.builder()
                .requireContext(true)
                .showTrace(false)
                .disableOnSelected(true)
                .showOrderNumbers(true)
                .orderNumberSuffix("/")
                .build();

        if (conceptNames.contains("associativity") &&
                allowedConceptNames.contains("operator_binary_+") &&
                !conceptNames.contains("precedence")) {
            // make an Ordering question ...
            QuestionEntity question = new QuestionEntity();
            question.setExerciseAttempt(questionRequest.getExerciseAttempt());
            question.setQuestionText(QuestionTextToHtml("a + b + c"));
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "+ between b and c", "operator_binary_+", getName(0, 4)))));
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "+", "b", "+", "c"))));
            question.setQuestionType(QuestionType.ORDER);
            question.setOptions(orderQuestionOptions);
            return new Ordering(question);
        } else if (conceptNames.contains("precedence") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*") &&
                deniedConceptNames.contains("associativity")) {
            // make an Ordering question ...
            QuestionEntity question = new QuestionEntity();
            question.setExerciseAttempt(questionRequest.getExerciseAttempt());
            question.setQuestionText(QuestionTextToHtml("a == b < c"));
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "==", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "<", "operator_binary_*", getName(0, 4))
            )));
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "==", "b", "<", "c"))));
            question.setQuestionType(QuestionType.ORDER);
            question.setOptions(orderQuestionOptions);
            return new Ordering(question);
        }  else if (conceptNames.contains("precedence") &&
                conceptNames.contains("associativity") &&
                allowedConceptNames.contains("operator_binary_+") &&
                allowedConceptNames.contains("operator_binary_*")) {
            // make an Ordering question ...
            QuestionEntity question = new QuestionEntity();
            question.setExerciseAttempt(questionRequest.getExerciseAttempt());
            question.setQuestionText(QuestionTextToHtml("a + b + c * d"));
            question.setQuestionDomainType(EVALUATION_ORDER_QUESTION_TYPE);
            question.setAreAnswersRequireContext(true);
            question.setAnswerObjects(new ArrayList<>(Arrays.asList(
                    getAnswerObject(question, "+ between a and b", "operator_binary_+", getName(0, 2)),
                    getAnswerObject(question, "+ between c and d", "operator_binary_+", getName(0, 4)),
                    getAnswerObject(question, "*", "operator_binary_*", getName(0, 6))
            )));
            question.setStatementFacts(getBackendFacts(new ArrayList<>(Arrays.asList("a", "+", "b", "+", "c", "*", "d"))));
            question.setQuestionType(QuestionType.ORDER);
            question.setOptions(orderQuestionOptions);
            return new Ordering(question);
        } else {
            // make a SingleChoice question ...
            QuestionEntity question = new QuestionEntity();
            question.setExerciseAttempt(questionRequest.getExerciseAttempt());
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

    public static String QuestionTextToHtml(String text) {
        StringBuilder sb = new StringBuilder(text);

        // Wrap every token of the expression with <span> and some metadata ...
        Pattern pattern = Pattern.compile("\\<\\=|\\>\\=|\\=\\=|\\!\\=|\\<\\<|\\>\\>|\\+|\\-|\\*|\\/|\\<|\\>|\\w+");
        Matcher matcher = pattern.matcher(sb.toString());
        int idx = 0;
        int anwerIdx = -1;
        int offset = 0;
        while (offset < sb.length() && matcher.find(offset)) {
            String match = matcher.group(0);
            String replaceStr = match.matches("\\w")
                    ? "<span data-comp-ph-pos='" + (++idx) +"' class='comp-ph-expr-const'>" + matcher.group(0) +"</span>"
                    : "<span data-comp-ph-pos='" + (++idx) +"' id='answer_" + (++anwerIdx) +"' class='comp-ph-expr-op-btn'>" + matcher.group(0) +"</span>";

            sb.replace(matcher.start(), matcher.end(), replaceStr);
            offset = matcher.start() + replaceStr.length() ;
            matcher = pattern.matcher(sb.toString());
        }

        sb = new StringBuilder(sb.toString().replaceAll("\\*", "&#8727"));
        sb.insert(0, "<p class='comp-ph-expr'>"); sb.append("</p>");
        sb.insert(0, "<p>Приоритет операций в порядке убывания</p>");
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
    }

    String getName(int step, int index) {
        return "op__" + step + "__" + index;
    }

    AnswerObjectEntity getAnswerObject(QuestionEntity question, String text, String concept, String domainInfo) {
        AnswerObjectEntity answerObject = new AnswerObjectEntity();
        answerObject.setHyperText(text);
        answerObject.setRightCol(false);
        answerObject.setDomainInfo(domainInfo);
        answerObject.setConcept(concept);
        answerObject.setQuestion(question);
        return answerObject;
    }

    public List<BackendFactEntity> getBackendFacts(List<String> expression) {
        List<BackendFactEntity> facts = new ArrayList<>();
        int index = 0;
        for (String token : expression) {
            index++;
            for (int step = 0; step <= expression.size(); ++step) {
                String name = getName(step, index);
                facts.add(new BackendFactEntity(name, "rdf:type", "owl:NamedIndividual"));
                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "index", "xsd:int", String.valueOf(index)));
                facts.add(new BackendFactEntity("owl:NamedIndividual", name, "step", "xsd:int", String.valueOf(step)));
            }
            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "text", "xsd:string", token));
            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "complex_beginning", "xsd:boolean", Boolean.toString(token.equals("(") || token.equals("[") || token.equals("?"))));
            facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "complex_ending", "xsd:boolean", Boolean.toString(token.equals(")") || token.equals("]") || token.equals(":"))));
        }
        facts.add(new BackendFactEntity("owl:NamedIndividual", getName(0, index), "last", "xsd:boolean", "true"));
        return facts;
    }

    public List<LawFormulation> getErrorLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getOWLLawFormulation("is_operand", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("student_pos_number", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("before_as_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_by_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_direct", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("before_third_operator", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("complex_beginning", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("describe_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_precedence_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_precedence_diff_precedence", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_precedence_right_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("is_operator_with_strict_operands_order", "owl:DatatypeProperty"));
        laws.add(getOWLLawFormulation("student_pos_less", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_equal_precedence", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_in_complex", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_precedence", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_precedence_left", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_more_precedence_right", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_right_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_strict_operands_order", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("text", "owl:DatatypeProperty"));
        laws.add(getJenaLawFormulation(
                "describe_error",
//                "student_pos_less(?b, ?a) ^ before_direct(?a, ?b) -> describe_error(?a, ?b)"
                "(?b my:student_pos_less ?a), (?a my:before_direct ?b) -> (?a my:describe_error ?b)."
        ));
        laws.add(getJenaLawFormulation(
                "student_error_in_complex",
//                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ text(?c, \"(\") ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
                "(?a my:before_by_third_operator ?b), (?a my:before_third_operator ?c), (?c my:text \"(\"), (?a my:describe_error ?b) -> (?b my:student_error_in_complex ?a)."
        ));
        laws.add(getJenaLawFormulation(
                "student_error_in_complex_bound",
//                "before_as_operand(?a, ?b) ^ complex_beginning(?b, true) ^ describe_error(?a, ?b) -> student_error_in_complex(?b, ?a)"
                "(?a my:before_as_operand ?b), (?b my:complex_beginning \"true\"^^xsd:boolean), (?a my:describe_error ?b) -> (?b my:student_error_in_complex ?a)."
        ));
        laws.add(getJenaLawFormulation(
                "student_error_more_precedence",
//                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_precedence_diff_precedence(?a, ?b) -> student_error_more_precedence(?b, ?a)"
                "(?a my:before_as_operand ?b), (?a my:describe_error ?b), (?a my:high_precedence_diff_precedence ?b) -> (?b my:student_error_more_precedence ?a)."
        ));
        laws.add(getJenaLawFormulation(
                "student_error_right_assoc",
//                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_precedence_right_assoc(?a, ?b) -> student_error_right_assoc(?b, ?a)"
                "(?a my:before_as_operand ?b), (?a my:describe_error ?b), (?a my:high_precedence_right_assoc ?b) -> (?b my:student_error_right_assoc ?a)."
        ));
        laws.add(getJenaLawFormulation(
                "student_error_strict_operands_order",
//                "before_by_third_operator(?a, ?b) ^ before_third_operator(?a, ?c) ^ is_operator_with_strict_operands_order(?c, true) ^ describe_error(?a, ?b) -> student_error_strict_operands_order(?b, ?a)"
                "(?a my:before_by_third_operator ?b), (?a my:before_third_operator ?c), (?c my:is_operator_with_strict_operands_order \"true\"^^xsd:boolean), (?a my:describe_error ?b) -> (?b my:student_error_strict_operands_order ?a)."
        ));
        return laws;
    }

    public List<LawFormulation> getLeftAssocErrorLaws() {
        List<LawFormulation> laws = new ArrayList<>();
        laws.add(getOWLLawFormulation("before_as_operand", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("describe_error", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("high_precedence_left_assoc", "owl:ObjectProperty"));
        laws.add(getOWLLawFormulation("student_error_left_assoc", "owl:ObjectProperty"));
        laws.add(getJenaLawFormulation(
                "student_error_left_assoc",
//                "before_as_operand(?a, ?b) ^ describe_error(?a, ?b) ^ high_precedence_left_assoc(?a, ?b) -> student_error_left_assoc(?b, ?a)"
                "(?a my:before_as_operand ?b), (?a my:describe_error ?b), (?a my:high_precedence_left_assoc ?b) -> (?b my:student_error_left_assoc ?a)."
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

    // filter positive laws by question type and tags
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<PositiveLaw> positiveLaws = new ArrayList<>();
            for (PositiveLaw law : getPositiveLaws()) {
                boolean needLaw = true;
                for (Tag tag : law.getTags()) {
                    boolean inQuestionTags = false;
                    for (Tag questionTag : tags) {
                        if (questionTag.getName().equals(tag.getName())) {
                            inQuestionTags = true;
                        }
                    }
                    if (!inQuestionTags) {
                        needLaw = false;
                    }
                }
                if (needLaw) {
                    positiveLaws.add(law);
                }
            }
            return positiveLaws;
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
                    getNegativeLaw("error_precedence_binary_*_equal_binary_+"),
                    getNegativeLaw("error_single_token_binary_operator_has_unevaluated_same_precedence_left_associativity_left")
            ));
            return result;
        }
        return new ArrayList<>(Arrays.asList());
    }

    LawFormulation getSWRLLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = new LawFormulation();
        lawFormulation.setName(name);
        lawFormulation.setFormulation(formulation);
        lawFormulation.setBackend("SWRL");
        return lawFormulation;
    }

    LawFormulation getJenaLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = getSWRLLawFormulation(name, formulation);
        lawFormulation.setBackend("Jena");
        return lawFormulation;
    }

    LawFormulation getOWLLawFormulation(String name, String formulation) {
        LawFormulation lawFormulation = new LawFormulation();
        lawFormulation.setName(name);
        lawFormulation.setFormulation(formulation);
        lawFormulation.setBackend("OWL");
        return lawFormulation;
    }

    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "has_operand",
                    "before_direct",
                    "before_by_third_operator",
                    "before_third_operator",
                    "before_as_operand",
                    "is_operator_with_strict_operands_order",
                    "high_precedence_diff_precedence",
                    "high_precedence_left_assoc",
                    "high_precedence_right_assoc",
                    "is_operand"
            ));
        }
        return new ArrayList<>();
    }

    public List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            return new ArrayList<>(Arrays.asList(
                    "student_error_more_precedence",
                    "student_error_left_assoc",
                    "student_error_right_assoc",
                    "student_error_in_complex",
                    "student_error_strict_operands_order",
                    "text",
                    "index",
                    "before_direct",
                    "student_pos_number",
                    "is_operand"
            ));
        }
        return new ArrayList<>();
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects) {
        if (questionDomainType.equals(EVALUATION_ORDER_QUESTION_TYPE)) {
            List<BackendFactEntity> result = new ArrayList<>();
            int pos = 1;
            HashSet<String> used = new HashSet<>();
            for (ResponseEntity response : responses) {
                result.add(new BackendFactEntity(
                        "owl:NamedIndividual",
                        response.getLeftAnswerObject().getDomainInfo(),
                        "student_pos_number",
                        "xsd:int",
                        String.valueOf(pos)
                ));
                for (String earlier : used) {
                    result.add(new BackendFactEntity(
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

            for (AnswerObjectEntity answerObject : answerObjects) {
                if (!used.contains(answerObject.getDomainInfo())) {
                    for (String earlier : used) {
                        result.add(new BackendFactEntity(
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
    public ProcessSolutionResult processSolution(List<BackendFactEntity> solution) {
        Map<String, List<String>> before = new HashMap<>();
        Map<String, String> studentPos = new HashMap<>();
        HashSet<String> isOperand = new HashSet<>();
        HashSet<String> allTokens = new HashSet<>();
        for (BackendFactEntity fact : solution) {
            if (fact.getVerb().equals("before_direct")) {
                if (!before.containsKey(fact.getObject())) {
                    before.put(fact.getObject(), new ArrayList<String>());
                }
                before.get(fact.getObject()).add(fact.getSubject());
                allTokens.add(fact.getObject());
                allTokens.add(fact.getSubject());
            } else if (fact.getVerb().equals("student_pos_number")) {
                studentPos.put(fact.getSubject(), fact.getObject());
            } else if (fact.getVerb().equals("is_operand")) {
                isOperand.add(fact.getSubject());
            }
        }

        int IterationsLeft = 0;
        int CountCorrectOptions = 0;
        for (String operator : allTokens) {
            if (!operator.startsWith("op__0") || isOperand.contains(operator)) {
                continue;
            }
            boolean can = !studentPos.containsKey(operator);
            if (before.containsKey(operator)) {
                List<String> deps = before.get(operator);
                for (String dep : deps) {
                    if (!studentPos.containsKey(dep) && !isOperand.contains(dep)) {
                        can = false;
                    }
                }
            }
            if (can) {
                CountCorrectOptions++;
            }
            if (!studentPos.containsKey(operator)) {
                IterationsLeft++;
            }
        }

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.CountCorrectOptions = CountCorrectOptions;
        result.IterationsLeft = IterationsLeft;
        return result;
    }

    @Override
    public InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations) {
        List<MistakeEntity> mistakes = new ArrayList<>();

        // retrieve subjects' info from facts ...
        Map<String, BackendFactEntity> nameToText = new HashMap<>();
        Map<String, BackendFactEntity> nameToPos = new HashMap<>();
        Map<String, List<String>> before = new HashMap<>();

        for (BackendFactEntity violation : violations) {
            if (violation.getVerb().equals("text")) {
                nameToText.put(violation.getSubject(), violation);
            } else if (violation.getVerb().equals("index")) {
                nameToPos.put(violation.getSubject(), violation);
            } else if (violation.getVerb().equals("before_direct")) {
                if (!before.containsKey(violation.getObject())) {
                    before.put(violation.getObject(), new ArrayList<String>());
                }
                before.get(violation.getObject()).add(violation.getSubject());
            }
        }

        // filter facts and fill mistakes list ...
        for (BackendFactEntity violation : violations) {
            MistakeEntity mistake = new MistakeEntity();
            if (violation.getVerb().equals("student_error_more_precedence")) {
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

        InterpretSentenceResult result = new InterpretSentenceResult();
        result.mistakes = mistakes;

        ProcessSolutionResult processResult = processSolution(violations);
        result.CountCorrectOptions = processResult.CountCorrectOptions;
        result.IterationsLeft = processResult.IterationsLeft;
        return result;
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<MistakeEntity> mistakes, FeedbackType feedbackType) {
        ArrayList<HyperText> result = new ArrayList<>();
        for (MistakeEntity mistake : mistakes) {
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

    HyperText makeExplanation(MistakeEntity mistake, FeedbackType feedbackType) {

        // retrieve subjects' info from facts, and find base and third ...
        BackendFactEntity base = null;
        BackendFactEntity third = null;
        Map<String, String> nameToText = new HashMap<>();
        Map<String, String> nameToPos = new HashMap<>();
        for (BackendFactEntity fact : mistake.getViolationFacts()) {
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

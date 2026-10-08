package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationMemberTreeTest {

    private static final String FIELD_ACCESS_SKILL = "field_access";
    private static final String METHOD_CALL_SKILL = "method_call";
    private static final String CONSTRUCTION_SKILL = "object_construction";
    private static final String LOOKUP_SKILL = "member_lookup";
    private static final String VISIBILITY_SKILL = "member_visibility";
    private static final String STATIC_ACCESS_SKILL = "static_member_access";

    // Person: поля name, статическое count, закрытое secret, защищённое nickname, метод average и статический from_csv;
    // Student наследует Person и добавляет grades. У float_field и append нет объявившего типа среди классов:
    // их берут, когда обращаются к члену, которого у типа нет.
    private static final String SITUATION = """
            obj t_int : py_int {}
            obj t_float : py_float {}
            obj t_none : py_NoneType {}
            obj t_error : py_TypeError {}
            obj t_attribute_error : py_AttributeError {}
            obj upper : Method { returns(t_str); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj split : Method { returns(t_list_str); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj join : Method { returns(t_str); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj append : Method { returns(t_none); isStatic = false; visibility = Visibility:public; mutatesReceiver = true; }
            obj pop : Method { returns(t_int); isStatic = false; visibility = Visibility:public; mutatesReceiver = true; }
            obj t_str : py_str { elementType(t_str); declares(upper); declares(split); declares(join); }
            obj t_list_int : py_list { elementType(t_int); declares(append); declares(pop); }
            obj t_list_str : py_list { elementType(t_str); }
            obj name : Field { hasType(t_str); isStatic = false; visibility = Visibility:public; }
            obj count : Field { hasType(t_int); isStatic = true; visibility = Visibility:public; }
            obj secret : Field { hasType(t_str); isStatic = false; visibility = Visibility:private; }
            obj nickname : Field { hasType(t_str); isStatic = false; visibility = Visibility:protected; }
            obj grades : Field { hasType(t_list_int); isStatic = false; visibility = Visibility:public; }
            obj float_field : Field { hasType(t_float); isStatic = false; visibility = Visibility:public; }
            obj average : Method { returns(t_float); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj from_csv : Method { returns(t_person); isStatic = true; visibility = Visibility:public; mutatesReceiver = false; }
            obj t_person : py_class { declares(name); declares(count); declares(secret); declares(nickname); declares(average); declares(from_csv); }
            obj t_student : py_class { directlyInheritsFrom(t_person); declares(grades); }
            obj person : Variable { hasType(t_person); }
            obj student : Variable { hasType(t_student); }
            obj text : Variable { hasType(t_str); }
            obj marks : Variable { hasType(t_list_int); }
            obj words : Variable { hasType(t_list_str); }
            obj Person : ClassReference { refersTo(t_person); }
            """;

    /** Ответ по правилам поиска поля засчитывается: поля родителя, статические поля, закрытые поля и отсутствующие поля. */
    @ParameterizedTest
    @CsvSource({
            "student, name,        '',        t_str",
            "student, grades,      '',        t_list_int",
            "person,  count,       '',        t_int",
            "Person,  count,       '',        t_int",
            "person,  float_field, '',        t_attribute_error",
            "Person,  name,        '',        t_attribute_error",
            "person,  secret,      '',        t_attribute_error",
            "person,  secret,      t_person,  t_str",
            "person,  nickname,    '',        t_str",
            "student, secret,      t_student, t_attribute_error",
    })
    void fieldAnswerByLookupRuleIsCorrect(String owner, String field, String enclosingClass, String answer) {
        // Act.
        var verdict = judgeFieldAccess(owner, field, enclosingClass, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Тип объекта вместо типа поля объясняется тем, что результат взят у того, у чего берут поле. */
    @Test
    void ownerTypeInsteadOfFieldTypeIsOwnerType() {
        // Act.
        var verdict = judgeFieldAccess("student", "name", "", "t_student");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("owner_type"), verdict.hypotheses());
        assertEquals(Set.of(FIELD_ACCESS_SKILL), verdict.skills());
    }

    /** Ошибка при обращении к существующему полю объясняется тем, что студент счёл поле отсутствующим. */
    @Test
    void errorForExistingFieldIsMissingMemberAssumed() {
        // Act.
        var verdict = judgeFieldAccess("person", "name", "", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("missing_member_assumed"), verdict.hypotheses());
        assertEquals(Set.of(LOOKUP_SKILL), verdict.skills());
    }

    /** Ошибка при обращении к полю родителя объясняется только тем, что наследник не получает членов родителя. */
    @Test
    void errorForInheritedFieldIsInheritanceIgnored() {
        // Act.
        var verdict = judgeFieldAccess("student", "name", "", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("inheritance_ignored"), verdict.hypotheses());
        assertEquals(Set.of(LOOKUP_SKILL), verdict.skills());
    }

    /** Ошибка при обращении к защищённому полю снаружи объясняется верой, что защищённое снаружи недоступно. */
    @Test
    void errorForProtectedFieldIsProtectedAssumedRestricted() {
        // Act.
        var verdict = judgeFieldAccess("person", "nickname", "", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("protected_assumed_restricted"), verdict.hypotheses());
        assertEquals(Set.of(VISIBILITY_SKILL), verdict.skills());
    }

    /** Тип закрытого поля при обращении снаружи класса объясняется верой, что закрытое доступно отовсюду. */
    @Test
    void fieldTypeForPrivateFieldOutsideIsPrivateAssumedAccessible() {
        // Act.
        var verdict = judgeFieldAccess("person", "secret", "", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("private_assumed_accessible"), verdict.hypotheses());
        assertEquals(Set.of(VISIBILITY_SKILL), verdict.skills());
    }

    /** Тип закрытого поля родителя в методе наследника объясняется верой, что наследник видит закрытое родителя. */
    @Test
    void fieldTypeForParentPrivateFieldInHeirIsPrivateInheritedAssumedAccessible() {
        // Act.
        var verdict = judgeFieldAccess("student", "secret", "t_student", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("private_inherited_assumed_accessible"), verdict.hypotheses());
    }

    /** Ошибка при чтении статического поля через объект объясняется верой, что оно доступно только через класс. */
    @Test
    void errorForStaticFieldViaObjectIsStaticViaInstanceInaccessible() {
        // Act.
        var verdict = judgeFieldAccess("person", "count", "", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("static_via_instance_inaccessible"), verdict.hypotheses());
    }

    /** Тип поля, которого у типа объекта нет, объясняется тем, что студент приписал типу чужое поле. */
    @Test
    void typeOfMissingFieldIsForeignMember() {
        // Act.
        var verdict = judgeFieldAccess("person", "float_field", "", "t_float");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("foreign_member"), verdict.hypotheses());
    }

    /** Ошибка неприменимой операции вместо ошибки отсутствующего поля объясняется путаницей видов ошибок. */
    @Test
    void inapplicableOperationErrorForMissingFieldIsErrorKindConfused() {
        // Act.
        var verdict = judgeFieldAccess("person", "float_field", "", "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("error_kind_confused"), verdict.hypotheses());
        assertEquals(Set.of("error_kind"), verdict.skills());
    }

    /** Тип поля объекта при обращении через класс объясняется верой, что поле объекта можно прочитать через класс. */
    @Test
    void fieldTypeForInstanceFieldViaClassIsInstanceMemberViaClass() {
        // Act.
        var verdict = judgeFieldAccess("Person", "name", "", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("instance_member_via_class"), verdict.hypotheses());
        assertEquals(Set.of(STATIC_ACCESS_SKILL), verdict.skills());
    }

    /**
     * Ответ по правилам вызова метода засчитывается: встроенные и объявленные в коде методы, изменяющие объект,
     * статические методы, метод объекта через класс и отсутствующий метод.
     */
    @ParameterizedTest
    @CsvSource({
            "text,    upper,    t_str",
            "text,    split,    t_list_str",
            "marks,   append,   t_none",
            "marks,   pop,      t_int",
            "student, average,  t_float",
            "Person,  from_csv, t_person",
            "person,  from_csv, t_person",
            "Person,  average,  t_error",
            "text,    append,   t_attribute_error",
    })
    void methodAnswerByCallRuleIsCorrect(String owner, String method, String answer) {
        // Act.
        var verdict = judgeMethodCall(owner, method, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Тип списка в ответ на append объясняется верой, что изменяющий метод возвращает изменённый объект. */
    @Test
    void receiverTypeForMutatorIsMutatorReturnsReceiver() {
        // Act.
        var verdict = judgeMethodCall("marks", "append", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("mutator_returns_receiver"), verdict.hypotheses());
        assertEquals(Set.of(METHOD_CALL_SKILL), verdict.skills());
    }

    /** Ответ str на split объясняется верой, что метод возвращает значение того же типа, что и объект. */
    @Test
    void receiverTypeForSplitIsReceiverType() {
        // Act.
        var verdict = judgeMethodCall("text", "split", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("receiver_type"), verdict.hypotheses());
    }

    /** Ответ None на pop объясняется верой, что метод только выполняет действие и ничего не возвращает. */
    @Test
    void noneForPopIsNoResultAssumed() {
        // Act.
        var verdict = judgeMethodCall("marks", "pop", "t_none");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("no_result_assumed"), verdict.hypotheses());
    }

    /** Тип списка в ответ на join со списком в аргументе объясняется тем, что результат взят у аргумента. */
    @Test
    void argumentTypeForJoinIsArgumentType() {
        // Act.
        var verdict = judgeSituation(SITUATION + """
                var E = obj op : py_method_call { hasOperand<OperandPlacement:left>(text); hasOperand<OperandPlacement:right>(words); accesses(join); }
                var T = t_list_str
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("argument_type"), verdict.hypotheses());
    }

    /** None в ответ на append у строки объясняется тем, что студент приписал строке метод списка. */
    @Test
    void resultOfMissingMethodIsForeignMember() {
        // Act.
        var verdict = judgeMethodCall("text", "append", "t_none");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("foreign_member"), verdict.hypotheses());
    }

    /** Тип результата метода объекта, вызванного через класс, объясняется верой, что объект для вызова не нужен. */
    @Test
    void resultForInstanceMethodViaClassIsInstanceMemberViaClass() {
        // Act.
        var verdict = judgeMethodCall("Person", "average", "t_float");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("instance_member_via_class"), verdict.hypotheses());
    }

    /**
     * Ошибку отсутствующего члена при вызове метода объекта через класс объясняют верное решение с путаницей видов
     * ошибок и мнение, что такого метода у класса нет.
     */
    @Test
    void missingMemberErrorForInstanceMethodViaClassIsErrorKindConfusedOrMissingMember() {
        // Act.
        var verdict = judgeMethodCall("Person", "average", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of("error_kind_confused"), Set.of("missing_member_assumed")), verdict.reasonings());
    }

    /** Ошибка при вызове статического метода через класс объясняется верой, что метод вызывают только у объекта. */
    @Test
    void errorForStaticMethodViaClassIsStaticViaClassInaccessible() {
        // Act.
        var verdict = judgeMethodCall("Person", "from_csv", "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("static_via_class_inaccessible"), verdict.hypotheses());
        assertEquals(Set.of(STATIC_ACCESS_SKILL), verdict.skills());
    }

    /** Ошибка отсутствующего члена при вызове статического метода через объект — вера, что так его не вызвать. */
    @Test
    void missingMemberErrorForStaticMethodViaObjectIsStaticViaInstanceInaccessible() {
        // Act.
        var verdict = judgeMethodCall("person", "from_csv", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of("static_via_instance_inaccessible")), verdict.reasonings());
    }

    /**
     * Ошибка другого вида при вызове статического метода через объект объясняется тем же решением и ещё одним
     * заблуждением — о виде ошибки: ошибочные решения складываются в одно рассуждение.
     */
    @Test
    void inapplicableOperationErrorForStaticMethodViaObjectCombinesTwoMisconceptions() {
        // Act.
        var verdict = judgeMethodCall("person", "from_csv", "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of("static_via_instance_inaccessible", "error_kind_confused")), verdict.reasonings());
    }

    /** Тип объекта в ответ на закрытое поле снаружи объясняют вместе два заблуждения: о закрытости и о результате. */
    @Test
    void ownerTypeForPrivateFieldOutsideCombinesTwoMisconceptions() {
        // Act.
        var verdict = judgeFieldAccess("person", "secret", "", "t_person");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of("private_assumed_accessible", "owner_type")), verdict.reasonings());
    }

    /**
     * Верный ответ TypeError на Person.average() даёт и сочетание двух заблуждений: метода у класса нет, а об
     * отсутствующем члене сообщает TypeError. Такое ошибочное рассуждение сохраняется.
     */
    @Test
    void correctAnswerReachedByTwoMisconceptionsKeepsMisreasoning() {
        // Act.
        var verdict = judgeMethodCall("Person", "average", "t_error");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.reasonings().contains(Set.of("missing_member_assumed", "error_kind_confused")),
                verdict.reasonings().toString());
    }

    /** Вызов класса создаёт объект этого класса. */
    @Test
    void constructionGivesObjectOfClass() {
        // Act.
        var verdict = judgeConstruction("t_person");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(RULE), verdict.hypotheses());
    }

    /** Ответ None на вызов класса объясняется тем, что конструктор ничего не возвращает. */
    @Test
    void noneForConstructionIsConstructorReturnsNone() {
        // Act.
        var verdict = judgeConstruction("t_none");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("constructor_returns_none"), verdict.hypotheses());
        assertEquals(Set.of(CONSTRUCTION_SKILL), verdict.skills());
    }

    private static @NotNull TypeEvaluationTreeFixture.Verdict judgeFieldAccess(@NotNull String owner, @NotNull String field,
                                                                             @NotNull String enclosingClass, @NotNull String answer) {
        var enclosing = enclosingClass.isEmpty() ? "" : "enclosedIn(%s);".formatted(enclosingClass);
        return judgeSituation(SITUATION + """
                var E = obj op : py_field_access { hasOperand<OperandPlacement:left>(%s); accesses(%s); %s }
                var T = %s
                """.formatted(owner, field, enclosing, answer));
    }

    private static @NotNull TypeEvaluationTreeFixture.Verdict judgeMethodCall(@NotNull String owner, @NotNull String method,
                                                                            @NotNull String answer) {
        return judgeSituation(SITUATION + """
                var E = obj op : py_method_call { hasOperand<OperandPlacement:left>(%s); accesses(%s); }
                var T = %s
                """.formatted(owner, method, answer));
    }

    private static @NotNull TypeEvaluationTreeFixture.Verdict judgeConstruction(@NotNull String answer) {
        return judgeSituation(SITUATION + """
                var E = obj op : py_constructor_call { hasOperand<OperandPlacement:left>(Person); }
                var T = %s
                """.formatted(answer));
    }
}

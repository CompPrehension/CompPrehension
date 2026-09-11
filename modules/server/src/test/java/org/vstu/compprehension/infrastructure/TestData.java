package org.vstu.compprehension.infrastructure;

import java.util.List;

public final class TestData {

    private TestData() {
    }

    public static final String EDUCATION_RESOURCE_URL = "https://lms.test.local";
    public static final String MAIN_COURSE_EXTERNAL_ID = "ext-course-1";
    public static final String OTHER_COURSE_EXTERNAL_ID = "ext-course-2";

    public static final long EDUCATION_RESOURCE_ID = -1L;

    public static final long MAIN_COURSE_ID = -1L;
    public static final long OTHER_COURSE_ID = -2L;

    public static final long GLOBAL_POOL_EXERCISE_ID = -1L;
    public static final long MAIN_COURSE_EXERCISE_ID = -2L;
    public static final long INHERITED_EXERCISE_ID = -3L;
    public static final long OTHER_COURSE_EXERCISE_ID = -4L;
    public static final long EXPRESSION_DT_EXERCISE_ID = -5L;
    public static final int EXPRESSION_DT_EXERCISE_QUESTIONS = 2;

    public static final long GLOBAL_ADMIN_ID = -1L;
    public static final long GLOBAL_EXERCISE_AUTHOR_ID = -2L;
    public static final long GLOBAL_STUDENT_ID = -3L;
    public static final long MAIN_COURSE_TEACHER_ID = -4L;
    public static final long MAIN_COURSE_ASSISTANT_ID = -5L;
    public static final long MAIN_COURSE_STUDENT_ID = -6L;
    public static final long OTHER_COURSE_TEACHER_ID = -7L;
    public static final long USER_WITHOUT_ROLES_ID = -8L;
    public static final long EDUCATION_RESOURCE_ADMIN_ID = -9L;

    public static final int EXPRESSION_QUESTION_METADATA_ID = -1;

    public record BankQuestion(int metadataId, String expression, long[] evaluationOrder) {
        public long operatorAt(int step) {
            return evaluationOrder[step];
        }

        public long endEvaluationAnswerId() {
            return evaluationOrder.length;
        }

        public int steps() {
            return evaluationOrder.length;
        }
    }

    public static final BankQuestion MEMBER_ACCESS_PLUS = new BankQuestion(-1, "wp->sx + sb_w", new long[] {0, 1});
    public static final BankQuestion MODULO_PLUS = new BankQuestion(-2, "byte % 10 + '0'", new long[] {0, 1});
    public static final BankQuestion MUL_PLUS_MINUS = new BankQuestion(-3, "10 * z + c - '0'", new long[] {0, 1, 2});
    public static final BankQuestion ASSIGN_UNARY_MINUS_PLUS = new BankQuestion(-4, "pre = -pre + 2", new long[] {1, 2, 0});
    public static final BankQuestion PARENTHESES_AND_UNARY_MINUS = new BankQuestion(-5, "(i & -i)", new long[] {1, 0});

    public static final List<BankQuestion> EXPRESSION_BANK = List.of(
            MEMBER_ACCESS_PLUS, MODULO_PLUS, MUL_PLUS_MINUS, ASSIGN_UNARY_MINUS_PLUS, PARENTHESES_AND_UNARY_MINUS);

    public static BankQuestion bankQuestion(int metadataId) {
        return EXPRESSION_BANK.stream()
                .filter(q -> q.metadataId() == metadataId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет вопроса банка с metadataId " + metadataId));
    }

    public static final String DOMAIN_ID = "ProgrammingLanguageExpressionDTDomain";
    public static final String STRATEGY_ID = "StaticStrategy";
    public static final String BACKEND_ID = "ProductionBackend";
}

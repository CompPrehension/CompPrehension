package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.utils.tokens.ComplexOperatorToken;
import org.vstu.meaningtree.utils.tokens.OperatorToken;
import org.vstu.meaningtree.utils.tokens.Token;
import org.vstu.meaningtree.utils.tokens.TokenList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class QuestionDynamicDataAppender {
    /**
     * Adds dynamic data required for student interaction
     * Method create question text and answer objects.
     * Old question format will be automatically converted to new format
     * @param q domain question object
     * @param attempt exercise attempt
     * @param bank question bank
     * @param lang programming language of question
     * @param domain target domain
     * @return filled domain question object
     */
    public static Question appendQuestionData(Question q, @Nullable ExerciseAttemptEntity attempt, QuestionBank bank,
                                              SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain, Language userLang) {
        var meta = q.getMetadata();
        if (meta != null && meta.getVersion() < MeaningTreeOrderQuestionBuilder.MIN_VERSION) {
            q = MeaningTreeOrderQuestionBuilder.fastBuildFromExisting(q, lang, domain);
            QuestionDataEntity dataEntity = new QuestionDataEntity(null, SerializableQuestion.fromQuestion(q));
            bank.saveQuestionDataEntity(dataEntity);
            meta.setQuestionData(dataEntity);
            bank.saveMetadataEntity(q.getMetadata());
        }

        TokenList tokens = MeaningTreeRDFHelper.backendFactsToTokens(q.getStatementFacts(), lang);
        q.setAnswerObjects(new ArrayList<>(MeaningTreeOrderQuestionBuilder.generateAnswerObjects(tokens).stream().map(
                (SerializableQuestion.AnswerObject obj) -> {
                    AnswerObjectEntity ansEntity = new AnswerObjectEntity();
                    ansEntity.setConcept(obj.getConcept());
                    ansEntity.setDomainInfo(obj.getDomainInfo());
                    ansEntity.setHyperText(obj.getHyperText());
                    ansEntity.setAnswerId(obj.getAnswerId());
                    ansEntity.setRightCol(obj.isRightCol());
                    return ansEntity;
                }).toList()));
        q.getQuestionData().setQuestionText(questionToHtml(tokens, domain, userLang, q.getMetadata() == null ? -1 : q.getMetadata().getId()));
        q.getQuestionData().setExerciseAttempt(attempt);
        q.getQuestionData().setDomainEntity(domain.getDomainEntity());
        return q;
    }

    /**
     * Creates question HTML representation
     * @param tokens tokens of question
     * @param domain target domain
     * @param lang user locale
     * @return question html string
     */
    static String questionToHtml(TokenList tokens,
                                 ProgrammingLanguageExpressionDTDomain domain,
                                 Language lang, int metaId
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(domain.getMessage("BASE_QUESTION_TEXT", lang));
        sb.append("<p class='comp-ph-expr'>");
        HashMap<Integer, Integer> complexEndingsIds = new HashMap<>();
        int idx = 0;
        int answerIdx = -1;
        for (Token t: tokens) {
            String tokenValue = t.value;
            if (t instanceof OperatorToken) {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' id='answer_")
                        .append(complexEndingsIds.containsKey(idx - 1) ? complexEndingsIds.get(idx - 1) : ++answerIdx)
                        .append("' class='comp-ph-expr-op-btn'").append(">").append(tokenValue).append("</span>");

                if (t instanceof ComplexOperatorToken complex && complex.isOpening()) {
                    int pos = tokens.findClosingComplex(idx - 1);
                    if (pos != -1) {
                        complexEndingsIds.put(pos, answerIdx);
                    }
                }
            } else {
                sb.append("<span data-comp-ph-pos='").append(++idx).append("' class='comp-ph-expr-const'").append(">").append(tokenValue).append("</span>");
            }
        }

        sb.append("<br/><button data-comp-ph-pos='").append(++idx).append("' id='answer_").append(++answerIdx).append("' class='btn comp-ph-complete-btn' data-comp-ph-value=''>").append(
                lang != null && domain != null ? domain.getMessage("student_end_evaluation", lang) : "everything is evaluated"
        ).append("</button>");

        sb.append("<!-- Original expression: ");
        sb.append(tokens.stream().map((Token t) -> t.value).collect(Collectors.joining(" ")));
        sb.append(' ');
        sb.append("-->");
        sb.append("<!-- Metadata id: ");
        sb.append(metaId);
        sb.append("-->");
        sb.append("</p>");
        String text = sb.toString();

        sb = new StringBuilder(text
                .replaceAll("\\*", "&#8727")
                .replaceAll("\\n", "<br>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
        sb.insert(0, "<div class='comp-ph-question'>"); sb.append("</div>");
        return sb.toString();
    }
}

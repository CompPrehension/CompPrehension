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
import org.vstu.meaningtree.utils.tokens.TokenList;

import java.util.ArrayList;

public class QuestionDynamicDataAppender {
    /**
     * Добавить информацию в вопрос перед его выдачей в зависимости от языка программирования
     * В вопрос добавляется объекты ответа, текст вопроса.
     * Если вопрос в старом формате, то перед выдачей он преобразуется и сохраняется в новом формате
     * @param q вопрос
     * @param attempt попытка выполнения упражнения
     * @param bank банк вопросов
     * @param lang язык программирования, на котором должен быть вопрос
     * @param domain домен, для которого вопрос предназначается
     * @return заполненный вопрос или новый объект вопроса в новом формате
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
        q.getQuestionData().setQuestionText(MeaningTreeOrderQuestionBuilder.questionToHtml(tokens, domain, userLang, q.getMetadata() == null ? -1 : q.getMetadata().getId()));
        q.getQuestionData().setExerciseAttempt(attempt);
        return q;
    }
}

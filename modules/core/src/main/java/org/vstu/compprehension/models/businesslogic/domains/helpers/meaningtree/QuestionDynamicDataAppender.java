package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.utils.tokens.TokenList;

import java.util.ArrayList;

public class QuestionDynamicDataAppender {
    public static Question appendQuestionData(Question q, ExerciseAttemptEntity attempt, QuestionBank bank,
                                              SupportedLanguage lang, ProgrammingLanguageExpressionDTDomain domain) {
        if (q.getMetadata().getVersion() < MeaningTreeOrderQuestionBuilder.MIN_VERSION) {
            q = MeaningTreeOrderQuestionBuilder.fastBuildFromExisting(q, lang, domain);
            QuestionDataEntity dataEntity = new QuestionDataEntity(null, SerializableQuestion.fromQuestion(q));
            bank.saveQuestionDataEntity(dataEntity);
            QuestionMetadataEntity meta = q.getMetadata();
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
        q.getQuestionData().setQuestionText(MeaningTreeOrderQuestionBuilder.questionToHtml(tokens, domain, attempt.getUser().getPreferred_language()));
        q.getQuestionData().setExerciseAttempt(attempt);
        return q;
    }
}

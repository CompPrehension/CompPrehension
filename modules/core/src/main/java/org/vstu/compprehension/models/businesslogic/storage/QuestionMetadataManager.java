package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import lombok.val;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.math.BigInteger;
import java.util.*;

@Getter
public class QuestionMetadataManager {

    Domain domain;
    QuestionMetadataBaseRepository questionRepository;
    QuestionGroupStat wholeBankStat;
    HashMap<String, Long> conceptName2bit;
    HashMap<String, Long> lawName2bit;
    HashMap<String, Long> violationName2bit;

    public QuestionMetadataManager(
            Domain domain,
            QuestionMetadataBaseRepository questionMetadataRepository
    ) {
        this.questionRepository = questionMetadataRepository;

        initBankStat(domain);
    }

    public static long namesToBitmask(Collection<String> names, Map<String, Long> name2bitMapping) {
        return names.stream()
                .map(s -> name2bitMapping.getOrDefault(s, 0L))
                .reduce((a,b) -> a | b).orElse(0L);
    }

    private void initBankStat(Domain domain) {
        Checkpointer ch = new Checkpointer();

        Map<String, Object> data = questionRepository.getStatOnComplexityField(domain.getShortName());
        NumericStat complStat = new NumericStat();
        complStat.setCount(((BigInteger)data.get("count")).intValue());
        complStat.setMin  ((Double) Optional.ofNullable(data.get("min") ).orElse(0.0));
        complStat.setMean ((Double) Optional.ofNullable(data.get("mean")).orElse(0.5));
        complStat.setMax  ((Double) Optional.ofNullable(data.get("max") ).orElse(1.0));

        wholeBankStat = new QuestionGroupStat();
        wholeBankStat.setComplexityStat(complStat);

//        ch.hit("initBankStat - stats prepared");
        ch.since_start("initBankStat - completed");
    }

    List<QuestionMetadataEntity> findQuestionsAroundComplexityWithoutQIds(
            QuestionRequest qr,
            double complexityMaxDifference,
            int limit,
            int randomPoolLimit
    ) {
        // lists cannot be empty in SQL: workaround
        val templatesIds = qr.getDeniedQuestionTemplateIds();
        if (templatesIds == null || templatesIds.isEmpty()) {
            qr.setDeniedQuestionTemplateIds(List.of(0));
        }
        val questionsIds = qr.getDeniedQuestionMetaIds();
        if (questionsIds == null || questionsIds.isEmpty()) {
            qr.setDeniedQuestionMetaIds(List.of(0));
        }
        if (randomPoolLimit < limit)
            randomPoolLimit = limit;
        Iterable<? extends QuestionMetadataEntity> iter = questionRepository.findSampleAroundComplexityWithoutQIds(qr, complexityMaxDifference,
                limit, randomPoolLimit);
        ArrayList<QuestionMetadataEntity> foundQuestions = new ArrayList<>();
        iter.forEach(foundQuestions::add);
        return foundQuestions;
    }
}

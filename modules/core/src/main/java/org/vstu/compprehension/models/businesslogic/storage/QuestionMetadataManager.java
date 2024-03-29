package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.util.*;

@Log4j2
@Getter
public class QuestionMetadataManager {

    private final Domain domain;
    private final QuestionMetadataRepository questionRepository;
    private QuestionGroupStat wholeBankStat;
    //private final HashMap<String, Long> conceptName2bit;
    //private final HashMap<String, Long> lawName2bit;
    //private final HashMap<String, Long> violationName2bit;

    public QuestionMetadataManager(Domain domain, QuestionMetadataRepository questionMetadataRepository
    ) {
        this.domain = domain;
        this.questionRepository = questionMetadataRepository;
    }

    public static long namesToBitmask(Collection<String> names, Map<String, Long> name2bitMapping) {
        return names.stream()
                .map(s -> name2bitMapping.getOrDefault(s, 0L))
                .reduce((a,b) -> a | b).orElse(0L);
    }

    public QuestionGroupStat getWholeBankStat() {
        ensureBankStatLoaded();
        return wholeBankStat;
    }

    private void ensureBankStatLoaded() {
        if (wholeBankStat != null) {
            return;
        }

        Checkpointer ch = new Checkpointer(log);
        var stats = questionRepository.getStatOnComplexityField(domain.getShortName());
        NumericStat complStat = new NumericStat();
        complStat.setCount((int)(long)stats.getCount());
        complStat.setMin  (Optional.ofNullable(stats.getMin()).orElse(0.0));
        complStat.setMean (Optional.ofNullable(stats.getMean()).orElse(0.5));
        complStat.setMax  (Optional.ofNullable(stats.getMax()).orElse(1.0));

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
        ensureBankStatLoaded();

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

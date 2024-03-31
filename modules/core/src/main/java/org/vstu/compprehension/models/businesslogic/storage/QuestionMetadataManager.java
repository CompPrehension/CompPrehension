package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.HashMap;
import java.util.Optional;

@Log4j2
public class QuestionMetadataManager {
    private final QuestionMetadataRepository questionRepository;
    private HashMap<String, NumericStat> complexityStats;

    public QuestionMetadataManager(QuestionMetadataRepository questionMetadataRepository) {
        this.questionRepository = questionMetadataRepository;
    }

    public NumericStat getComplexityStats(String domainShortname) {
        ensureBankStatLoaded(domainShortname);
        return complexityStats.get(domainShortname);
    }

    private void ensureBankStatLoaded(String domainShortname) {
        if (complexityStats != null) {
            return;
        }

        var stats = questionRepository.getStatOnComplexityField(domainShortname);
        var complexityStats = new NumericStat(
                (int)(long)stats.getCount(),
                Optional.ofNullable(stats.getMin()).orElse(0.0),
                Optional.ofNullable(stats.getMean()).orElse(0.5),
                Optional.ofNullable(stats.getMax()).orElse(1.0)
        );
        this.complexityStats.put(domainShortname, complexityStats);
    }
}

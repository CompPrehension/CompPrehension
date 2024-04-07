package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.storage.stats.NumericStat;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class QuestionMetadataManager {
    private final QuestionMetadataRepository questionRepository;
    private final ConcurrentHashMap<String, ComplexityStats> complexityStats;
    
    private record ComplexityStats(LocalDateTime createDate, NumericStat stats) {}

    public QuestionMetadataManager(QuestionMetadataRepository questionMetadataRepository) {
        this.questionRepository = questionMetadataRepository;
        this.complexityStats    = new ConcurrentHashMap<>();
    }

    public NumericStat getComplexityStats(String domainShortname) {
        return ensureBankStatLoaded(domainShortname).stats();
    }

    private @NotNull ComplexityStats ensureBankStatLoaded(String domainShortname) {
        var nowShifted   = LocalDateTime.now().plusMinutes(-15);
        var currentStats = complexityStats.get(domainShortname);
        if (currentStats != null && currentStats.createDate().isAfter(nowShifted)) {
            return currentStats;
        }

        var stats = questionRepository.getStatOnComplexityField(domainShortname);
        var complexityStats = new NumericStat(
                (int)(long)stats.getCount(),
                Optional.ofNullable(stats.getMin()).orElse(0.0),
                Optional.ofNullable(stats.getMean()).orElse(0.5),
                Optional.ofNullable(stats.getMax()).orElse(1.0)
        );
        var newStats = new ComplexityStats(LocalDateTime.now(), complexityStats);
        this.complexityStats.put(domainShortname, newStats);
        return newStats;
    }
}

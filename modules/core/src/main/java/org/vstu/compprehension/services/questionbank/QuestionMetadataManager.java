package org.vstu.compprehension.services.questionbank;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.repositories.data.QuestionBankDataRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class QuestionMetadataManager {
    private final QuestionBankDataRepository bankRepository;
    private final ConcurrentHashMap<String, ComplexityStats> complexityStats;

    private record ComplexityStats(LocalDateTime createDate, NumericStat stats) {}

    public QuestionMetadataManager(QuestionBankDataRepository bankRepository) {
        this.bankRepository = bankRepository;
        this.complexityStats = new ConcurrentHashMap<>();
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

        var stats = bankRepository.getComplexityStats(domainShortname);
        // Подстановки на случай пустого банка: без вопросов домена шкала сложности
        // вырождается, и нормализовать запрос не по чему.
        var complexityStats = new NumericStat(
                (int) stats.count(),
                Optional.ofNullable(stats.min()).orElse(0.0),
                Optional.ofNullable(stats.mean()).orElse(0.5),
                Optional.ofNullable(stats.max()).orElse(1.0)
        );
        var newStats = new ComplexityStats(LocalDateTime.now(), complexityStats);
        this.complexityStats.put(domainShortname, newStats);
        return newStats;
    }
}

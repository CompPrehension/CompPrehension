package org.vstu.compprehension.models.businesslogic;

import its.model.definition.DomainModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFTransformer;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeUtils;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.QuestionDynamicDataAppender;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("dev")
@Disabled("manual export task — remove @Disabled to run")
public class ExpressionDTLoqiRdfExportTask {
    private static final String DOMAIN_SHORTNAME = "expression_dt";
    private static final int BATCH_SIZE = 8192;

    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionMetadataRepository qMetaRepo;
    @Autowired
    private QuestionBank qBank;

    private ExerciseAttemptEntity attempt;
    private ExerciseEntity exercise;
    private ProgrammingLanguageExpressionDTDomain domain;

    @BeforeAll
    public void setUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(
                "ProgrammingLanguageExpressionDTDomain");
        exercise = new ExerciseEntity();
        exercise.setDomain(domain.getDomainEntity());
        exercise.setBackendId("DTReasoner");
        exercise.setTags("");
        exercise.setOptions(new ExerciseOptionsEntity(null, true,
                true, true, true,
                true, true, 7, null, null));
        exercise.setName("expression-dt-export");
        exercise.setStages(Collections.singletonList(new ExerciseStageEntity()));
        exercise.setStrategyId("StaticStrategy");
        exerciseRepository.save(exercise);
        attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);
    }

    @AfterAll
    public void tearDown() {
        exerciseAttemptRepository.delete(attempt);
        exerciseRepository.delete(exercise);
    }

    @Test
    public void exportAll() throws IOException {
        Path outDir = Path.of(System.getProperty("export.dir", "export/expression_dt"));
        Files.createDirectories(outDir);

        int fromId = Integer.getInteger("export.fromId", 0);
        int limit = Integer.getInteger("export.limit", Integer.MAX_VALUE);
        Set<String> nameFilter = parseNameFilter(System.getProperty("export.names"));

        int lastId = fromId;
        int exported = 0;

        outer:
        while (true) {
            List<QuestionMetadataEntity> batch = qMetaRepo.loadPageWithData(lastId, BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }

            for (QuestionMetadataEntity meta : batch) {
                lastId = meta.getId();
                if (!DOMAIN_SHORTNAME.equals(meta.getDomainShortname())) {
                    continue;
                }
                if (!nameFilter.isEmpty() && !nameFilter.contains(meta.getName())) {
                    continue;
                }
                try {
                    exportOne(meta, outDir);
                    exported++;
                    System.err.printf("OK id=%d name=%s%n", meta.getId(), meta.getName());
                } catch (Exception e) {
                    System.err.printf("FAILED id=%d name=%s: %s%n", meta.getId(), meta.getName(), e.getMessage());
                    e.printStackTrace();
                }
                if (exported >= limit) {
                    break outer;
                }
            }
        }

        System.err.printf("Exported %d question(s) to %s%n", exported, outDir.toAbsolutePath());
    }

    private void exportOne(QuestionMetadataEntity meta, Path outDir) {
        Question q = prepareQuestion(meta);
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
        String langTag = lang.toString().substring(0, 1).toUpperCase() + lang.toString().substring(1).toLowerCase();

        DomainModel model = MeaningTreeRDFTransformer.questionToDomainModel(
                domain.getDomainSolvingModels().getFirst(),
                q.getStatementFacts(),
                List.of(),
                List.of(domain.getTag(langTag)),
                false
        );

        String baseName = meta.getId() + "_" + sanitizeFileName(meta.getName());
        MeaningTreeRDFTransformer.dumpModelLoqi(model, outDir.resolve(baseName + ".loqi").toFile());
        MeaningTreeRDFTransformer.dumpModelTtl(model, outDir.resolve(baseName + ".ttl").toFile());
        MeaningTreeRDFTransformer.dumpModelJsonLd(model, outDir.resolve(baseName + ".jsonld").toFile());
    }

    private Question prepareQuestion(QuestionMetadataEntity meta) {
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
        Question q = meta.getQuestionData().getData().toQuestion(domain, meta);
        return QuestionDynamicDataAppender.appendQuestionData(q, attempt, qBank, lang, domain, Language.ENGLISH);
    }

    private static Set<String> parseNameFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

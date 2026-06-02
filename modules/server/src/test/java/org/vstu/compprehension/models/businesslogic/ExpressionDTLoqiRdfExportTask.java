package org.vstu.compprehension.models.businesslogic;

import its.model.definition.DomainModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFHelper;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFTransformer;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeUtils;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.security.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
})
/**
 * Manual export task — excluded from default {@code mvn test}; run this class explicitly.
 * <p>VM options (optional): {@code -Dexport.dir=export/expression_dt -Dexport.limit=3}
 */
public class ExpressionDTLoqiRdfExportTask {
    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;
    private static final String DOMAIN_SHORTNAME = "expression_dt";
    private static final int BATCH_SIZE = 8192;

    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private QuestionMetadataRepository qMetaRepo;

    private ProgrammingLanguageExpressionDTDomain domain;

    @BeforeAll
    public void setUp() {
        System.err.println("ExpressionDT export: setUp started");
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(
                "ProgrammingLanguageExpressionDTDomain");
        System.err.println("ExpressionDT export: setUp finished");
    }

    @Test
    public void exportAll() throws IOException {
        Path outDir = Path.of(System.getProperty("export.dir", "export/expression_dt"));
        Files.createDirectories(outDir);

        int fromId = readIntProperty("export.fromId", 0);
        int limit = readIntProperty("export.limit", Integer.MAX_VALUE);
        Set<String> nameFilter = parseNameFilter(System.getProperty("export.names"));

        System.err.printf(
                "ExpressionDT export: dir=%s fromId=%d limit=%d names=%s%n",
                outDir.toAbsolutePath(), fromId, limit, nameFilter.isEmpty() ? "*" : nameFilter
        );

        int lastId = fromId;
        int exported = 0;
        int batchNo = 0;

        outer:
        while (exported < limit) {
            int batchLimit = Math.min(BATCH_SIZE, limit - exported);
            System.err.printf("ExpressionDT export: loading batch #%d after id=%d (limit %d)...%n",
                    ++batchNo, lastId, batchLimit);
            long t0 = System.currentTimeMillis();
            List<QuestionMetadataEntity> batch = qMetaRepo.loadPageWithData(
                    lastId, DOMAIN_SHORTNAME, batchLimit);
            System.err.printf("ExpressionDT export: batch #%d loaded %d row(s) in %d ms%n",
                    batchNo, batch.size(), System.currentTimeMillis() - t0);
            if (batch.isEmpty()) {
                break;
            }

            for (QuestionMetadataEntity meta : batch) {
                lastId = meta.getId();
                if (!nameFilter.isEmpty() && !nameFilter.contains(meta.getName())) {
                    continue;
                }
                try {
                    exportOne(meta, outDir, exported + 1, limit);
                    exported++;
                    System.err.printf("OK id=%d name=%s (%d/%d)%n",
                            meta.getId(), meta.getName(), exported, limit);
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

    private static int readIntProperty(String name, int defaultValue) {
        String raw = System.getProperty(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(raw.trim());
    }

    private void exportOne(QuestionMetadataEntity meta, Path outDir, int index, int limit) {
        boolean skipJsonLd = Boolean.parseBoolean(System.getProperty("export.skipJsonLd", "false"));
        long t0 = System.currentTimeMillis();
        logStep(meta.getId(), index, limit, "start");

        Question q = prepareQuestionForExport(meta);
        logStep(meta.getId(), index, limit, "prepared in " + (System.currentTimeMillis() - t0) + " ms");

        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
        String langTag = lang.toString().substring(0, 1).toUpperCase() + lang.toString().substring(1).toLowerCase();

        long t1 = System.currentTimeMillis();
        DomainModel model = MeaningTreeRDFTransformer.questionToDomainModel(
                domain.getDomainSolvingModels().getFirst(),
                q.getStatementFacts(),
                List.of(),
                List.of(domain.getTag(langTag)),
                false
        );
        logStep(meta.getId(), index, limit, "domain model in " + (System.currentTimeMillis() - t1) + " ms");

        String baseName = meta.getId() + "_" + sanitizeFileName(meta.getName());
        long t2 = System.currentTimeMillis();
        MeaningTreeRDFTransformer.dumpModelLoqi(model, outDir.resolve(baseName + ".loqi").toFile());
        logStep(meta.getId(), index, limit, "loqi in " + (System.currentTimeMillis() - t2) + " ms");

        long t3 = System.currentTimeMillis();
        MeaningTreeRDFTransformer.dumpModelTtl(model, outDir.resolve(baseName + ".ttl").toFile());
        logStep(meta.getId(), index, limit, "ttl in " + (System.currentTimeMillis() - t3) + " ms");

        if (!skipJsonLd) {
            long t4 = System.currentTimeMillis();
            MeaningTreeRDFTransformer.dumpModelJsonLd(model, outDir.resolve(baseName + ".jsonld").toFile());
            logStep(meta.getId(), index, limit, "jsonld in " + (System.currentTimeMillis() - t4) + " ms");
        }
    }

    /**
     * Read-only preparation: no {@link org.vstu.compprehension.models.businesslogic.storage.QuestionBank} writes.
     */
    private Question prepareQuestionForExport(QuestionMetadataEntity meta) {
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
        Question q = meta.getQuestionData().getData().toQuestion(domain, meta);
        if (meta.getVersion() < 12) {
            Question rebuilt = MeaningTreeOrderQuestionBuilder.fastBuildFromExisting(q, lang, domain);
            if (rebuilt != null) {
                q = rebuilt;
            }
        }
        q.getQuestionData().setStatementFacts(
                MeaningTreeRDFHelper.applyRuntimeFixes(q.getStatementFacts()));
        return q;
    }

    private static void logStep(int id, int index, int limit, String step) {
        System.err.printf("  [%d/%d] id=%d %s%n", index, limit, id, step);
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

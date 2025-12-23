package org.vstu.compprehension.models.businesslogic;

import domains.ControlFlowDTDomain;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
public class CtrlFlowLoadQuestionsTest {
    @Autowired
    DomainFactory domainFactory;

    @Autowired
    private QuestionMetadataRepository qMetaRepo;

    @Autowired
    private QuestionMetadataRepository qDataRepo;

    @Autowired
    private QuestionBank storage;

    private ControlFlowDTDomain domain;

    @BeforeAll
    public void tearUp() {
        domain = (ControlFlowDTDomain) domainFactory.getDomain("ControlFlowDTDomain");
    }

    public static List<String> findFiles(Path path, String[] fileExtensions) throws IOException {

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }

        List<String> result;
        try (Stream<Path> walk = Files.walk(path, 1)) {  // 1: only current dir
            result = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
                    .filter(f -> isEndWith(f.toLowerCase(), fileExtensions))
                    .collect(Collectors.toList());
        }
        return result;

    }

    private static boolean isEndWith(String file, String[] fileExtensions) {
        boolean result = false;
        for (String fileExtension : fileExtensions) {
            if (file.endsWith(fileExtension)) {
                result = true;
                break;
            }
        }
        return result;
    }

    @Test
    @Commit
    public void fillQuestions() {
        String dir = System.getProperty("test.questionDir");
        Assertions.assertNotNull(dir, "Specify -Dtest.questionDir=/your-questions-path");
        List<String> fileList;
        try {
            fileList = findFiles(Path.of(dir), new String[] {".json"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var metaList = new ArrayList<QuestionMetadataEntity>();
        var questions = fileList.stream()
                .map(SerializableQuestionTemplate::deserialize)
                .filter(Objects::nonNull).toList();
        for (var q : questions) {
            log.info("Sending question: {}", q.getCommonQuestion().getQuestionData().getQuestionName());

            QuestionDataEntity questionData = new QuestionDataEntity();
            var metaFound = qMetaRepo.findByName(q.getMetadataList().getFirst().getName());
            if (!metaFound.isEmpty()) {
                var replaceQ = metaFound.getFirst().getQuestionData();
                replaceQ.setData(q.getCommonQuestion());
                var meta = q.getMetadataList().getFirst().toMetadataEntity();
                meta.setId(metaFound.getFirst().getId());
                meta.setQuestionData(replaceQ);
                metaList.add(meta);
            } else {
                questionData.setData(q.getCommonQuestion());
                var meta = q.getMetadataList().getFirst().toMetadataEntity();
                meta.setQuestionData(questionData);
                metaList.add(meta);
            }
        }
        storage.saveMetadataWithDataEntities(metaList);
        log.info("Saved all questions to DB.");
    }
}

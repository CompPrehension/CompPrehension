package org.vstu.compprehension.domain;

import org.vstu.compprehension.businesslogic.domains.ControlFlowDTDomain;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.data.questionbank.NewBankQuestionData;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Disabled("Не работает в test-containers.")
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
        var toSave = new ArrayList<NewBankQuestionData>();
        var questions = fileList.stream()
                .map(SerializableQuestionTemplate::deserialize)
                .filter(Objects::nonNull).toList();
        for (var q : questions) {
            log.info("Sending question: {}", q.getCommonQuestion().getQuestionData().getQuestionName());

            var meta = q.getMetadataList().getFirst().toMetadataData();
            var metaFound = qMetaRepo.findByName(meta.getName());
            if (!metaFound.isEmpty()) {
                meta.setId(metaFound.getFirst().getId());
            }
            toSave.add(new NewBankQuestionData(q.getCommonQuestion(), List.of(meta)));
        }
        storage.saveQuestions(toSave);
        log.info("Saved all questions to DB.");
    }
}

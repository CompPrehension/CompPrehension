package org.vstu.compprehension.models.businesslogic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionDataRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.meaningtree.SupportedLanguage;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ProgrammingLanguageExpressionDTDomainTest {
    @Autowired
    DomainFactory domainFactory;

    @Autowired
    QuestionDataRepository qdata_repo;

    @Autowired
    QuestionMetadataRepository qmeta_repo;

    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    @Test
    public void testName() {
        Domain domain = domainFactory.getDomain(domainId);
        assertEquals(domain.getName(), "ProgrammingLanguageExpressionDTDomain");
    }

    @Test
    public void testDatabaseConversion() {
        ProgrammingLanguageExpressionDTDomain domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        int[] ids = {64325, 64496, 66965};
        for (int id : ids) {
            Optional<QuestionDataEntity> data = qdata_repo.findById(id);
            if (data.isPresent()) {
                QuestionDataEntity entity = data.get();
                SerializableQuestion newData = MeaningTreeOrderQuestionBuilder.fastBuildFromExisting(entity.getData(), SupportedLanguage.PYTHON, domain);
                entity.setData(newData);
                qdata_repo.save(entity);
                QuestionMetadataEntity metaEntity = newData.toMetadataEntity();
                metaEntity.setQuestionData(entity);
                metaEntity.setId(id);
                qmeta_repo.save(metaEntity);
            }
        }
    }

    @Test
    public void generateExampleQuestion()  {
        ProgrammingLanguageExpressionDTDomain domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        List<SerializableQuestion> questions = MeaningTreeOrderQuestionBuilder
                .newQuestion("a && b && (c || d)", SupportedLanguage.CPP, domain)
                .build(SupportedLanguage.PYTHON);
    }
}
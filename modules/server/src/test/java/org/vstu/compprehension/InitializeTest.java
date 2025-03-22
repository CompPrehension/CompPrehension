package org.vstu.compprehension;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InitializeTest {
    @Autowired
    private DomainRepository domainRepository;

    private void createTestData() {
        var newDomain = new DomainEntity();
        newDomain.setName("##TEST_TRANSACTION_ROLLBACK##");
        newDomain.setShortName("##TEST_TRANSACTION_ROLLBACK##");
        newDomain.setVersion("1");
        newDomain.setOptions(new DomainOptionsEntity());

        domainRepository.save(newDomain);
    }

    @Test
    public void fakeDomainExists() {
        // Create test domain
        createTestData();

        var domains = domainRepository.findAll();
        var domainNames = domains.stream()
                .map(DomainEntity::getName)
                .collect(Collectors.toSet());

        assertEquals(5, domainNames.size(), "There are 5 domains in the database");
        assertTrue(domainNames.contains("##TEST_TRANSACTION_ROLLBACK##"));
        assertTrue(domainNames.contains("ControlFlowStatementsDomain"));
        assertTrue(domainNames.contains("ControlFlowStatementsDTDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDTDomain"));
    }

    @Test
    public void noFakeDomain() {
        var domains = domainRepository.findAll();
        var domainNames = domains.stream()
                .map(DomainEntity::getName)
                .collect(Collectors.toSet());

        assertEquals(4, domainNames.size(), "There are 4 domains in the database");
        assertTrue(domainNames.contains("ControlFlowStatementsDomain"));
        assertTrue(domainNames.contains("ControlFlowStatementsDTDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDTDomain"));
    }
}

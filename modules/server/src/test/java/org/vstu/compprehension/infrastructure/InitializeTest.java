package org.vstu.compprehension.infrastructure;

import org.vstu.compprehension.data.domain.DomainOptionsData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.entities.DomainEntity;
import org.vstu.compprehension.repositories.entity.DomainRepository;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class InitializeTest extends AbstractIntegrationTest {

    private static final String FAKE_DOMAIN = "##TEST_TRANSACTION_ROLLBACK##";

    @Autowired
    private DomainRepository domainRepository;

    private void createTestData() {
        var newDomain = new DomainEntity();
        newDomain.setName(FAKE_DOMAIN);
        newDomain.setShortName(FAKE_DOMAIN);
        newDomain.setVersion("1");
        newDomain.setOptions(new DomainOptionsData());

        domainRepository.save(newDomain);
    }

    private java.util.Set<String> domainNames() {
        return domainRepository.findAll().stream()
                .map(DomainEntity::getName)
                .collect(Collectors.toSet());
    }

    /** Домены заводятся миграциями. */
    @Test
    public void migrationsCreateKnownDomains() {
        var domainNames = domainNames();

        assertTrue(domainNames.contains("ControlFlowStatementsDomain"));
        assertTrue(domainNames.contains("ControlFlowStatementsDTDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDomain"));
        assertTrue(domainNames.contains("ProgrammingLanguageExpressionDTDomain"));
    }

    /** Записанное тестом видно ему самому. */
    @Test
    public void fakeDomainExists() {
        createTestData();

        assertTrue(domainNames().contains(FAKE_DOMAIN));
    }

    /** Соседнему тесту не видно: транзакция откатывается. */
    @Test
    public void noFakeDomain() {
        assertFalse(domainNames().contains(FAKE_DOMAIN));
    }
}

package org.vstu.compprehension.infrastructure;

import org.vstu.compprehension.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

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
        newDomain.setOptions(new DomainOptionsEntity());

        domainRepository.save(newDomain);
    }

    private java.util.Set<String> domainNames() {
        return domainRepository.findAll().stream()
                .map(DomainEntity::getName)
                .collect(Collectors.toSet());
    }

    /** Домены заводятся миграциями, поэтому в базе есть заранее известный набор. */
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

    /** И не видно соседнему: транзакция теста откатывается, база остаётся чистой для остальных. */
    @Test
    public void noFakeDomain() {
        assertFalse(domainNames().contains(FAKE_DOMAIN));
    }
}

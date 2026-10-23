package org.vstu.compprehension.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class InitializeTest extends AbstractIntegrationTest {

    private static final String FAKE_URL = "##TEST_TRANSACTION_ROLLBACK##";

    @Autowired
    private ExternalSystemDataRepository externalSystems;

    private boolean hasFakeResource() {
        return externalSystems.findEducationResource(FAKE_URL).isPresent();
    }

    /** Записанное тестом видно ему самому. */
    @Test
    public void fakeResourceExists() {
        externalSystems.createEducationResourceIfAbsent(FAKE_URL, EducationResourceTrustStatus.UNTRUSTED);

        assertTrue(hasFakeResource());
    }

    /** Соседнему тесту не видно: транзакция откатывается. */
    @Test
    public void noFakeResource() {
        assertFalse(hasFakeResource());
    }
}

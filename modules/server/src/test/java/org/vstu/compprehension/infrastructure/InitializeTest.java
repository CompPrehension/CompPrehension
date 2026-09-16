package org.vstu.compprehension.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class InitializeTest extends AbstractIntegrationTest {

    private static final String FAKE_URL = "##TEST_TRANSACTION_ROLLBACK##";

    @Autowired
    private EducationResourceRepository educationResourceRepository;

    private boolean hasFakeResource() {
        return educationResourceRepository.findByUrlAndType(FAKE_URL, EducationResourceType.UNKNOWN).isPresent();
    }

    /** Записанное тестом видно ему самому. */
    @Test
    public void fakeResourceExists() {
        educationResourceRepository.save(new EducationResourceEntity(FAKE_URL, EducationResourceType.UNKNOWN));

        assertTrue(hasFakeResource());
    }

    /** Соседнему тесту не видно: транзакция откатывается. */
    @Test
    public void noFakeResource() {
        assertFalse(hasFakeResource());
    }
}

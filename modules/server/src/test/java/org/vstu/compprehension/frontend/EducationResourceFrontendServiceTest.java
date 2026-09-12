package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class EducationResourceFrontendServiceTest extends AbstractIntegrationTest {

    private static final String NEW_LMS_URL = "https://new-lms.test.local";

    @Autowired private EducationResourceFrontendService service;

    /** Поиск по адресу и типу. */
    @Test
    void findIdByUrlAndTypeFindsKnownResource() {
        // Act & Assert.
        assertEquals(Optional.of(TestData.EducationResources.ID),
                service.findIdByUrlAndType(TestData.EducationResources.URL, EducationResourceType.MOODLE));
        assertEquals(Optional.empty(),
                service.findIdByUrlAndType(TestData.EducationResources.URL, EducationResourceType.UNKNOWN));
        assertEquals(Optional.empty(),
                service.findIdByUrlAndType(NEW_LMS_URL, EducationResourceType.MOODLE));
    }

    /** Доверенный ресурс возвращается как есть. */
    @Test
    void getOrCreateTrustedIdReturnsTrustedResource() {
        // Act.
        var id = service.getOrCreateTrustedId(TestData.EducationResources.URL, EducationResourceType.MOODLE);

        // Assert.
        assertEquals(TestData.EducationResources.ID, id);
        assertEquals(id, service.getOrCreateTrustedId(TestData.EducationResources.URL, EducationResourceType.MOODLE));
    }

    /** Новый ресурс создаётся недоверенным и не пропускается. */
    @Test
    void getOrCreateTrustedIdCreatesUntrustedResourceAndRejectsIt() {
        // Act & Assert.
        assertThrows(SecurityException.class, () -> service.getOrCreateTrustedId(NEW_LMS_URL, EducationResourceType.MOODLE));
        assertTrue(service.findIdByUrlAndType(NEW_LMS_URL, EducationResourceType.MOODLE).isPresent());
    }
}

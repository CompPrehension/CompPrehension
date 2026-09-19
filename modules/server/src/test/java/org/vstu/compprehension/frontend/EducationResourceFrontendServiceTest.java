package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    /** Существующий ресурс возвращается со своим статусом, переданный статус игнорируется. */
    @Test
    void getOrCreateKeepsTrustStatusOfExistingResource() {
        // Act.
        var resource = service.getOrCreate(TestData.EducationResources.URL, EducationResourceType.MOODLE,
                EducationResourceTrustStatus.UNTRUSTED);

        // Assert.
        assertEquals(TestData.EducationResources.ID, resource.id());
        assertEquals(EducationResourceTrustStatus.TRUSTED, resource.trustStatus());
    }

    /** Новый ресурс создаётся с переданным статусом и повторно не пересоздаётся. */
    @Test
    void getOrCreateCreatesResourceWithGivenTrustStatus() {
        // Act.
        var created = service.getOrCreate(NEW_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED);
        var again = service.getOrCreate(NEW_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.UNTRUSTED);

        // Assert.
        assertEquals(EducationResourceTrustStatus.TRUSTED, created.trustStatus());
        assertEquals(NEW_LMS_URL, created.url());
        assertEquals(created, again);
        assertEquals(Optional.of(created.id()), service.findIdByUrlAndType(NEW_LMS_URL, EducationResourceType.MOODLE));
    }
}

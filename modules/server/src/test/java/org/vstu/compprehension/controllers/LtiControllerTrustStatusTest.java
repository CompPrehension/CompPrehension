package org.vstu.compprehension.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LtiControllerTrustStatusTest {

    private static final List<String> TRUSTED_HOSTS = List.of("vstu.ru", ".Compprehension.RU", " ", "");

    /** Доверие определяется по совпадению с доверенным хостом или его поддоменом. */
    @ParameterizedTest
    @CsvSource({
            "https://vstu.ru,                       TRUSTED",
            "https://moodle.vstu.ru,                TRUSTED",
            "http://edu.moodle.vstu.ru:8081/path,   TRUSTED",
            "https://MOODLE.VSTU.RU,                TRUSTED",
            "https://moodle.vstu.ru.,               TRUSTED",
            "https://lms.compprehension.ru,         TRUSTED",
            "https://evilvstu.ru,                   UNTRUSTED",
            "https://vstu.ru.evil.com,              UNTRUSTED",
            "https://vstu.org,                      UNTRUSTED",
            "not a url,                             UNTRUSTED",
            "vstu.ru,                               UNTRUSTED",
    })
    void trustStatusOfMatchesTrustedHostOrSubdomain(String url, EducationResourceTrustStatus expected) {
        // Act.
        var status = LtiController.trustStatusOf(url, TRUSTED_HOSTS);

        // Assert.
        assertEquals(expected, status);
    }
}

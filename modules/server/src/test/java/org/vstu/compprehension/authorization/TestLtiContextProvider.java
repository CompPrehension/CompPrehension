package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.services.LtiContextProvider;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.businesslogic.lti.LtiDeepLinkingContext;

import java.util.List;
import java.util.Optional;

@Primary
@Component
@Profile("test")
public class TestLtiContextProvider implements LtiContextProvider {

    private static final ThreadLocal<LtiContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<LtiDeepLinkingContext> DEEP_LINKING = new ThreadLocal<>();

    /** Запуск из курса, заданного в data.sql. */
    public static void launchedFromCourse(String externalCourseId) {
        launchedFromLms(TestData.EducationResources.ID, externalCourseId);
    }

    /** Запуск из курса произвольной LMS. */
    public static void launchedFromLms(long educationResourceId, String externalCourseId) {
        launched(educationResourceId, externalCourseId, null);
    }

    /** Запуск из курса, заданного в data.sql, в котором LMS сообщила адрес списка участников. */
    public static void launchedFromCourseWithMemberships(String externalCourseId, String membershipsUrl) {
        launched(TestData.EducationResources.ID, externalCourseId, membershipsUrl);
    }

    private static void launched(long educationResourceId, String externalCourseId, String membershipsUrl) {
        CONTEXT.set(new LtiContext(
                null,
                TestData.EducationResources.URL,
                "test-client",
                educationResourceId,
                externalCourseId == null ? null : new LtiCourseContext(externalCourseId, "Test course"),
                null,
                membershipsUrl));
    }

    /** Запуск в режиме deep-linking. */
    public static void withDeepLinkingSession() {
        DEEP_LINKING.set(new LtiDeepLinkingContext(
                TestData.EducationResources.URL,
                "test-client",
                "test-deployment",
                "https://lms.test.local/lti/contentitem_return.php",
                null,
                null,
                List.of()));
    }

    public static void reset() {
        CONTEXT.remove();
        DEEP_LINKING.remove();
    }

    @Override
    public Optional<LtiContext> getCurrentLtiContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    @Override
    public Optional<LtiDeepLinkingContext> getCurrentDeepLinkingContext() {
        return Optional.ofNullable(DEEP_LINKING.get());
    }
}

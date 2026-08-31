package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiDeepLinkingContext;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

import java.util.List;
import java.util.Optional;

@Primary
@Component
@Profile("test")
public class TestLtiContextProvider implements LtiContextProvider {

    private static final ThreadLocal<LtiContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<LtiDeepLinkingContext> DEEP_LINKING = new ThreadLocal<>();

    /** Запуск из курса образовательного ресурса, заданного в data.sql. */
    public static void launchedFromCourse(String externalCourseId) {
        CONTEXT.set(new LtiContext(
                null,
                new LtiCourseContext(externalCourseId, "Test course"),
                TestData.EDUCATION_RESOURCE_URL,
                "Test LMS",
                EducationResourceType.MOODLE,
                null));
    }

    /** Запуск, пришедший как deep-linking: без него эндпоинты сборки активностей недоступны. */
    public static void withDeepLinkingSession() {
        DEEP_LINKING.set(new LtiDeepLinkingContext(
                TestData.EDUCATION_RESOURCE_URL,
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

package org.vstu.compprehension.infrastructure;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.services.LtiMembershipProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LMS для тестов: списки участников курсов задаёт сам тест.
 */
@Primary
@Component
@Profile("test")
public class TestLtiMembershipProvider implements LtiMembershipProvider {

    private static final Map<String, List<LtiCourseMemberData>> MEMBERS = new ConcurrentHashMap<>();
    private static final Set<String> UNAVAILABLE = ConcurrentHashMap.newKeySet();

    public static void hasMembers(@NotNull String membershipsUrl, @NotNull LtiCourseMemberData... members) {
        MEMBERS.put(membershipsUrl, List.of(members));
    }

    public static void isUnavailable(@NotNull String membershipsUrl) {
        UNAVAILABLE.add(membershipsUrl);
    }

    public static void reset() {
        MEMBERS.clear();
        UNAVAILABLE.clear();
    }

    @Override
    public @NotNull List<LtiCourseMemberData> fetchMembers(@NotNull LtiRegistrationData tool, @NotNull String membershipsUrl) {
        if (UNAVAILABLE.contains(membershipsUrl) || !MEMBERS.containsKey(membershipsUrl)) {
            throw new IllegalStateException("LMS does not answer at " + membershipsUrl);
        }
        return MEMBERS.get(membershipsUrl);
    }
}

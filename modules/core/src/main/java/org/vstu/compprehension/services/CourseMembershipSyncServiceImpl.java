package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.businesslogic.lti.LtiCourseRoles;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.data.lti.LtiCourseMembershipSourceData;
import org.vstu.compprehension.data.user.EducationResourceUserData;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;
import org.vstu.compprehension.repositories.data.LtiCourseMembershipDataRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
class CourseMembershipSyncServiceImpl implements CourseMembershipSyncService {

    private final LtiCourseMembershipDataRepository memberships;
    private final ExternalSystemDataRepository externalSystems;
    private final LtiMembershipProvider lms;
    private final RoleAssignmentService roleAssignmentService;

    @Override
    public void syncAll() {
        var sources = memberships.findSourcesOfTrustedLms();
        if (sources.isEmpty()) {
            log.info("No courses to sync found");
            return;
        }

        log.info("Syncing members of {} courses with LMS", sources.size());
        int synced = 0;
        int skipped = 0;
        int failed = 0;
        for (var source : sources) {
            try {
                if (syncCourse(source)) {
                    synced++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                failed++;
                log.warn("Members of course {} were not synced from {}: {}",
                        source.courseId(), source.membershipsUrl(), ex.getMessage(), ex);
            }
        }
        log.info("Course members synced with LMS: {} courses, {} skipped, {} failed", synced, skipped, failed);
    }

    /**
     * Список участников запрашивается вне транзакции, чтобы ожидание LMS не держало соединение с БД.
     * Сверяются роли только пользователей, связанных с LMS курса: роли, выданные иначе, не трогаются.
     * Роль участника, у которого она уже есть, задаёт запуск: Moodle при запуске проверяет права в самом
     * элементе курса, а в списке участников — только права в курсе, и учитель без права редактирования
     * приходит в нём как Learner.
     *
     * @return {@code false}, если курс пропущен: его инструмент больше не подключён
     */
    private boolean syncCourse(@NotNull LtiCourseMembershipSourceData source) {
        var tool = externalSystems.findLtiRegistration(source.issuer(), source.clientId()).orElse(null);
        if (tool == null) {
            log.info("Course {} is not synced: LTI tool {} of {} is no longer registered",
                    source.courseId(), source.clientId(), source.issuer());
            return false;
        }
        List<LtiCourseMemberData> members = lms.fetchMembers(tool, source.membershipsUrl());
        List<LtiCourseMemberData> activeMembers = members.stream().filter(LtiCourseMemberData::active).toList();

        Map<String, Long> userIdByLmsUserId = externalSystems.findEducationResourceUsers(source.educationResourceId(),
                        activeMembers.stream().map(LtiCourseMemberData::userId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(EducationResourceUserData::externalId, EducationResourceUserData::userId));
        Map<Long, Role> roleOfMember = activeMembers.stream()
                .filter(member -> userIdByLmsUserId.containsKey(member.userId()))
                .collect(Collectors.toMap(member -> userIdByLmsUserId.get(member.userId()),
                        member -> LtiCourseRoles.resolveCourseRole(member.roles()),
                        // Постраничный список со смещением повторяет участника, если курс меняется во время чтения.
                        (first, repeated) -> first));

        log.info("Course {}: {} members in LMS, {} active of them signed in here",
                source.courseId(), members.size(), roleOfMember.size());

        roleAssignmentService.reconcileCourseMembers(source.educationResourceId(), source.courseId(), roleOfMember);
        return true;
    }
}

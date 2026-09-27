package org.vstu.compprehension.service.gradepassback;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.data.outbox.AttemptFinishedEvent;
import org.vstu.compprehension.entities.external_system.EducationResourceUserEntity;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.repositories.data.ExerciseAttemptDataRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.OutboxEventRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;
import org.vstu.compprehension.service.outbox.OutboxProcessor;
import org.vstu.compprehension.services.EducationResourceService;
import org.vstu.compprehension.services.ExerciseAttemptDataService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradePassbackEventHandlerTest extends AbstractIntegrationTest {

    @Autowired private GradePassbackEventHandler handler;
    @Autowired private OutboxProcessor processor;
    @Autowired private ExerciseAttemptDataService exerciseAttemptService;
    @Autowired private ExerciseAttemptDataRepository exerciseAttemptDataRepository;
    @Autowired private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private RecordingLms lms;
    @Autowired private UserRepository userRepository;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private EducationResourceUserRepository educationResourceUserRepository;
    @Autowired private EducationResourceService educationResourceService;

    private final List<Long> createdAttemptIds = new ArrayList<>();

    @TestConfiguration
    static class LmsConfig {
        @Bean
        RecordingLms recordingLms() {
            return new RecordingLms();
        }
    }

    /** LMS, которая запоминает полученные оценки и отказывает для выбранных попыток. */
    static class RecordingLms implements GradePassbackStrategy {
        final List<Long> gradedAttemptIds = new ArrayList<>();
        final List<Instant> gradedAt = new ArrayList<>();
        final List<String> gradedExternalUserIds = new ArrayList<>();
        final Set<Long> failingAttemptIds = new HashSet<>();

        @Override
        public boolean supports(@NotNull GradePassbackTargetData target) {
            return true;
        }

        @Override
        public void passGrade(@NotNull GradePassbackTargetData target, double grade, @NotNull Instant gradedAt) {
            if (failingAttemptIds.contains(target.attemptId())) {
                throw new IllegalStateException("400 Incorrect score received");
            }
            gradedAttemptIds.add(target.attemptId());
            this.gradedAt.add(gradedAt);
            gradedExternalUserIds.add(target.externalUserId());
        }
    }

    @AfterEach
    void cleanUp() {
        outboxEventRepository.deleteAllInBatch();
        exerciseAttemptRepository.deleteAllById(createdAttemptIds);
        lms.gradedAttemptIds.clear();
        lms.gradedAt.clear();
        lms.gradedExternalUserIds.clear();
        lms.failingAttemptIds.clear();
    }

    /** Студент завершил попытку — оценка доходит до LMS со временем завершения. */
    @Test
    void finishedAttemptGradeReachesLms() {
        // Arrange.
        long attemptId = createAttempt(TestData.Users.MAIN_COURSE_STUDENT_ID);
        var beforeFinish = Instant.now();
        exerciseAttemptService.ensureAttemptStatus(attemptId, Decision.FINISH);

        // Act.
        processor.processDueEvents(Instant.now());

        // Assert.
        assertEquals(List.of(attemptId), lms.gradedAttemptIds);
        assertFalse(lms.gradedAt.getFirst().isBefore(beforeFinish));
    }

    /** Завершение попытки откатилось — оценка в LMS не уходит. */
    @Test
    void rolledBackFinishSendsNoGrade() {
        // Arrange.
        long attemptId = createAttempt(TestData.Users.MAIN_COURSE_STUDENT_ID);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            exerciseAttemptService.ensureAttemptStatus(attemptId, Decision.FINISH);
            status.setRollbackOnly();
        });

        // Act.
        processor.processDueEvents(Instant.now());

        // Assert.
        assertTrue(lms.gradedAttemptIds.isEmpty());
    }

    /** Отказ LMS — ошибка с попыткой и причиной: очередь сохранит её и повторит отправку. */
    @Test
    void lmsRefusalFailsWithReason() {
        // Arrange.
        long attemptId = createAttempt(TestData.Users.MAIN_COURSE_STUDENT_ID);
        lms.failingAttemptIds.add(attemptId);
        var event = new AttemptFinishedEvent(attemptId, 1.0, Instant.now());

        // Act.
        var error = assertThrows(IllegalStateException.class, () -> handler.handle(event));

        // Assert.
        assertEquals("Grade passback via RecordingLms failed for attempt " + attemptId
                + ": 400 Incorrect score received", error.getMessage());
    }

    /** Оценки тех, кто в курсе не студент, в журнал LMS не отправляются. */
    @Test
    void gradeOfNonStudentIsNotSent() {
        // Arrange.
        long attemptId = createAttempt(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        handler.handle(new AttemptFinishedEvent(attemptId, 1.0, Instant.now()));

        // Assert.
        assertTrue(lms.gradedAttemptIds.isEmpty());
    }

    /** Оценка удалённой попытки отбрасывается без ошибки: иначе очередь повторяла бы её впустую. */
    @Test
    void gradeOfDeletedAttemptIsDropped() {
        // Arrange.
        var event = new AttemptFinishedEvent(Long.MAX_VALUE, 1.0, Instant.now());

        // Act.
        handler.handle(event);

        // Assert.
        assertTrue(lms.gradedAttemptIds.isEmpty());
    }

    /** У студента учётки в нескольких LMS — оценка уходит за его учётку в LMS курса. */
    @Test
    @Transactional
    void gradeIsSentForStudentAccountInCourseLms() {
        // Arrange.
        var student = userRepository.getReferenceById(TestData.Users.MAIN_COURSE_STUDENT_ID);
        var otherLms = educationResourceService.getOrCreate(
                "https://other-lms.test.local", EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED);
        educationResourceUserRepository.save(new EducationResourceUserEntity(
                student, educationResourceRepository.getReferenceById(otherLms.id()), "other-lms-student"));
        educationResourceUserRepository.save(new EducationResourceUserEntity(
                student, educationResourceRepository.getReferenceById(TestData.EducationResources.ID), "course-lms-student"));
        long attemptId = createAttempt(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        handler.handle(new AttemptFinishedEvent(attemptId, 1.0, Instant.now()));

        // Assert.
        assertEquals(List.of("course-lms-student"), lms.gradedExternalUserIds);
    }

    private long createAttempt(long userId) {
        long attemptId = exerciseAttemptDataRepository.create(TestData.Exercises.MAIN_COURSE_ID,
                userId, TestData.Courses.MAIN_ID, null, null).attemptId();
        createdAttemptIds.add(attemptId);
        return attemptId;
    }
}

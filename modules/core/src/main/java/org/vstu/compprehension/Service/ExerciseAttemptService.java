package org.vstu.compprehension.Service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository.AttemptOwner;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class ExerciseAttemptService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;
    private final CourseService courseService;
    private final EducationResourceService educationResourceService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;

    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository,
                                  ExerciseService exerciseService,
                                  UserRepository userRepository,
                                  LtiContextProvider ltiContextProvider,
                                  GradePassbackService gradePassbackService,
                                  CourseService courseService,
                                  EducationResourceService educationResourceService,
                                  AuthService authService,
                                  AuthScopeFactory authScopes) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
        this.ltiContextProvider = ltiContextProvider;
        this.gradePassbackService = gradePassbackService;
        this.courseService = courseService;
        this.educationResourceService = educationResourceService;
        this.authService = authService;
        this.authScopes = authScopes;
    }

    @Transactional(readOnly = true)
    public Optional<ExerciseAttemptEntity> findById(Long attemptId) {
        return exerciseAttemptRepository.findById(attemptId);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessAttempt(long userId, long attemptId) {
        AttemptOwner owner = exerciseAttemptRepository.findOwnerByAttemptId(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("No attempt with id " + attemptId));
        ensureOwnerOrPrivileged(userId, owner, attemptId);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessQuestion(long userId, long questionId) {
        AttemptOwner owner = exerciseAttemptRepository.findOwnerByQuestionId(questionId)
                .orElseThrow(() -> new IllegalArgumentException("No attempt for question " + questionId));
        ensureOwnerOrPrivileged(userId, owner, questionId);
    }

    private void ensureOwnerOrPrivileged(long userId, AttemptOwner owner, long targetId) {
        if (owner.userId() != null && owner.userId() == userId) {
            authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authScopes.courseOrGlobal(owner.courseId()));
            return;
        }
        if (authService.isAuthorized(userId, SystemPermission.EDIT_EXERCISE, authScopes.courseOrGlobal(owner.courseId()))) {
            return;
        }
        throw new SecurityException(String.format(
                "User %s is not allowed to access attempt data %s", userId, targetId));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ExerciseAttemptEntity createNewAttempt(@NotNull Long exerciseId, @NotNull Long userId, Long courseId) {
        Long resolvedCourseId = courseId != null
                ? courseId
                : ltiContextProvider.getCurrentLtiContext()
                    .flatMap(this::resolveCourseIdFromContext)
                    .orElse(null);

        CourseEntity course = null;
        if (resolvedCourseId != null) {
            ExerciseCourseLinkEntity exerciseCourse = courseService.findExerciseCourseLinkOrThrow(exerciseId, resolvedCourseId);
            course = exerciseCourse.getCourse();
            exerciseAttemptRepository.changeExistingAttemptsStatusByCourse(
                    exerciseId, resolvedCourseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        } else {
            exerciseAttemptRepository.changeExistingAttemptsStatus(
                    exerciseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);
        }

        var exercise = exerciseService.getExercise(exerciseId);
        var user = userRepository.findById(userId).orElseThrow();

        var ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setCourse(course);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>());

        ltiContextProvider.getCurrentLtiContext().ifPresent(ctx -> {
            ea.setLtiLineitemUrl(ctx.lineitemUrl());
            if (ctx.course() != null) {
                ea.setLtiContextId(ctx.course().courseId());
            }
        });

        exerciseAttemptRepository.save(ea);
        return ea;
    }

    private Optional<Long> resolveCourseIdFromContext(LtiContext ctx) {
        LtiCourseContext course = ctx.course();
        if (course == null || course.courseId() == null) {
            return Optional.empty();
        }
        return educationResourceService.findByUrlAndType(ctx.lmsUrl(), ctx.lmsType())
                .flatMap(eduRes -> courseService.findByExternalIdAndResourceId(course.courseId(), eduRes.getId()))
                .map(CourseEntity::getId);
    }

    public void ensureAttemptStatus(ExerciseAttemptEntity attempt, Decision decision) {
        if (decision == Decision.FINISH && attempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            attempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
            exerciseAttemptRepository.save(attempt);
            double grade = exerciseAttemptRepository.calculateFinalGrade(attempt.getId())
                    .orElse(0.0);
            gradePassbackService.passGrade(attempt, grade);
        } else {
            exerciseAttemptRepository.save(attempt);
        }
    }
}

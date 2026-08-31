package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.EducationResourceService;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiDeepLinkingContext;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Permission;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.service.lti.DeepLinkingResponseService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("api/lti/deep-link")
@RequiredArgsConstructor
public class LtiDeepLinkingController {

    private final LtiContextProvider ltiContextProvider;
    private final UserService userService;
    private final AuthService authService;
    private final CourseService courseService;
    private final EducationResourceService educationResourceService;
    private final DeepLinkingResponseService deepLinkingResponseService;

    public record DeepLinkBuildRequest(List<Long> exerciseIds) {
    }

    public record DeepLinkBuildResponse(String jwt, String returnUrl) {
    }

    public record DeepLinkExistingResponse(List<Long> exerciseIds) {
    }

    /**
     * Собирает подписанный {@code LtiDeepLinkingResponse} для выбранных упражнений курса.
     * Фронт авто-сабмитит {@code jwt} формой на {@code returnUrl} (Moodle создаёт активности).
     */
    @SneakyThrows
    @PostMapping("build")
    @ResponseBody
    public DeepLinkBuildResponse build(@RequestBody DeepLinkBuildRequest body) {
        LtiDeepLinkingContext dl = requireDeepLinking();
        long courseId = requireAuthorizedCourse();

        if (body == null || body.exerciseIds() == null || body.exerciseIds().isEmpty()) {
            throw new IllegalArgumentException("exerciseIds must not be empty");
        }

        List<DeepLinkingResponseService.DeepLinkItem> items = new ArrayList<>();
        for (Long exerciseId : body.exerciseIds()) {
            ExerciseEntity exercise = courseService.findExerciseCourseLink(exerciseId, courseId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Exercise %s is not in course %s", exerciseId, courseId)))
                    .getExercise();
            items.add(new DeepLinkingResponseService.DeepLinkItem(exerciseId, exercise.getName()));
        }

        String jwt = deepLinkingResponseService.buildSignedResponse(dl, items);
        return new DeepLinkBuildResponse(jwt, dl.deepLinkReturnUrl());
    }

    /**
     * exercise_id уже добавленных в курс активностей (через AGS line items) — чтобы фронт
     * пометил их как добавленные. Fail-soft: при недоступности AGS вернёт пустой список.
     */
    @GetMapping("existing")
    @ResponseBody
    public DeepLinkExistingResponse existing() {
        LtiDeepLinkingContext dl = requireDeepLinking();
        requireAuthorizedCourse();
        return new DeepLinkExistingResponse(
                new ArrayList<>(deepLinkingResponseService.fetchExistingExerciseIds(dl)));
    }

    private LtiDeepLinkingContext requireDeepLinking() {
        return ltiContextProvider.getCurrentDeepLinkingContext()
                .orElseThrow(() -> new IllegalArgumentException("No active deep-linking session"));
    }

    @SneakyThrows
    private long requireAuthorizedCourse() {
        LtiContext ctx = ltiContextProvider.getCurrentLtiContext()
                .orElseThrow(() -> new IllegalArgumentException("LTI context absent"));
        // Read-only: education resource и курс уже созданы (и проверены на trusted) при LTI-запуске,
        // на котором основана эта deep-linking-сессия, поэтому здесь только lookup без side effects.
        LtiCourseContext course = ctx.course();
        if (course == null || course.courseId() == null) {
            throw new IllegalArgumentException("No course in LTI context");
        }
        EducationResourceEntity eduRes = educationResourceService.findByUrlAndType(ctx.lmsUrl(), ctx.lmsType())
                .orElseThrow(() -> new IllegalArgumentException("Unknown education resource"));
        CourseEntity courseEntity = courseService.findByExternalIdAndResourceId(course.courseId(), eduRes.getId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found for LTI context"));

        long userId = userService.getCurrentUser().getId();
        authService.ensureAuthorizedInCourse(userId, Permission.MANAGE_COURSE_CONTENT, courseEntity.getId());
        return courseEntity.getId();
    }
}

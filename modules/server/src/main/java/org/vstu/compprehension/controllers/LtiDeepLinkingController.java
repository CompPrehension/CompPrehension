package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.CourseFrontendService;
import org.vstu.compprehension.frontend.EducationResourceFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.businesslogic.lti.LtiDeepLinkingContext;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.service.lti.DeepLinkingResponseService;
import org.vstu.compprehension.services.LtiContextProvider;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("api/lti/deep-link")
@RequiredArgsConstructor
public class LtiDeepLinkingController {

    private final LtiContextProvider ltiProvider;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;
    private final CourseFrontendService courseService;
    private final EducationResourceFrontendService educationResourceService;
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

        List<DeepLinkingResponseService.DeepLinkItem> items =
                courseService.getExerciseRefsInCourseOrThrow(courseId, body.exerciseIds()).stream()
                        .map(ref -> new DeepLinkingResponseService.DeepLinkItem(ref.exerciseId(), ref.name()))
                        .toList();

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
        return ltiProvider.getCurrentDeepLinkingContext()
                .orElseThrow(() -> new IllegalArgumentException("No active deep-linking session"));
    }

    @SneakyThrows
    private long requireAuthorizedCourse() {
        LtiContext ctx = ltiProvider.getCurrentLtiContext()
                .orElseThrow(() -> new IllegalArgumentException("LTI context absent"));
        // Read-only: education resource и курс уже созданы (и проверены на trusted) при LTI-запуске,
        // на котором основана эта deep-linking-сессия, поэтому здесь только lookup без side effects.
        LtiCourseContext course = ctx.course();
        if (course == null || course.courseId() == null) {
            throw new IllegalArgumentException("No course in LTI context");
        }
        long eduResId = educationResourceService.findIdByUrlAndType(ctx.lmsUrl(), ctx.lmsType())
                .orElseThrow(() -> new IllegalArgumentException("Unknown education resource"));
        long courseId = courseService.findCourseIdByExternalIdAndResourceId(course.courseId(), eduResId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found for LTI context"));

        long userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.MANAGE_COURSE_CONTENT, authService.course(courseId));
        return courseId;
    }
}

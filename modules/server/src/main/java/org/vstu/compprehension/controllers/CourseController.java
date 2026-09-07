package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.CourseFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;

import java.util.List;

@Controller
@RequestMapping("api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseFrontendService courseService;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    @SneakyThrows
    @RequestMapping(value = {"my"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getMyCourses() {
        return courseService.getUserCourses(userService.getCurrentUserId());
    }

    @SneakyThrows
    @RequestMapping(value = {"memberships"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getExerciseMemberships(@RequestParam("exerciseId") long exerciseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authService.global());
        return courseService.getExerciseMemberships(exerciseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/add"}, method = {RequestMethod.POST})
    public void add(@RequestParam("exerciseId") long exerciseId,
                    @RequestParam("courseId") long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.MANAGE_COURSE_CONTENT, authService.course(courseId));
        courseService.addExerciseToCourse(exerciseId, courseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/remove"}, method = {RequestMethod.DELETE})
    public void remove(@RequestParam("exerciseId") long exerciseId,
                       @RequestParam("courseId") long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.MANAGE_COURSE_CONTENT, authService.course(courseId));
        courseService.removeExerciseFromCourse(exerciseId, courseId);
    }
}

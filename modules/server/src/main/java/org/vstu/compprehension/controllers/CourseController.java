package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.AuthScopeFactory;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;

import java.util.List;

@Controller
@RequestMapping("api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;

    @SneakyThrows
    @RequestMapping(value = {"my"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getMyCourses() {
        var currentUser = userService.getCurrentUser();
        return courseService.getUserCourses(currentUser);
    }

    @SneakyThrows
    @RequestMapping(value = {"memberships"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getExerciseMemberships(@RequestParam("exerciseId") long exerciseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.global());
        return courseService.getExerciseMemberships(exerciseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/add"}, method = {RequestMethod.POST})
    public void add(@RequestParam("exerciseId") long exerciseId,
                    @RequestParam("courseId") long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.MANAGE_COURSE_CONTENT, authScopes.course(courseId));
        courseService.addExerciseToCourse(exerciseId, courseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/remove"}, method = {RequestMethod.DELETE})
    public void remove(@RequestParam("exerciseId") long exerciseId,
                       @RequestParam("courseId") long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.MANAGE_COURSE_CONTENT, authScopes.course(courseId));
        courseService.removeExerciseFromCourse(exerciseId, courseId);
    }
}

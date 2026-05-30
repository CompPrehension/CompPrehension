package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.entities.EnumData.Role;

import java.util.List;

@Controller
@RequestMapping("api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;

    @SneakyThrows
    @RequestMapping(value = {"my"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getMyCourses() {
        var currentUser = userService.getCurrentUser();
        List<CourseDto> courses = courseService.getUserCourses(currentUser);
        return courses;
    }

    @SneakyThrows
    @RequestMapping(value = {"memberships"}, method = {RequestMethod.GET})
    @ResponseBody
    public List<CourseDto> getExerciseMemberships(@RequestParam("exerciseId") long exerciseId) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unauthorized");
        }
        return courseService.getExerciseMemberships(exerciseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/add"}, method = {RequestMethod.POST})
    public void add(@RequestParam("exerciseId") long exerciseId,
                    @RequestParam("courseId") long courseId) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unauthorized");
        }
        courseService.addExerciseToCourse(exerciseId, courseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise/remove"}, method = {RequestMethod.DELETE})
    public void remove(@RequestParam("exerciseId") long exerciseId,
                       @RequestParam("courseId") long courseId) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unauthorized");
        }
        courseService.removeExerciseFromCourse(exerciseId, courseId);
    }
}

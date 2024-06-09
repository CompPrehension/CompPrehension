package org.vstu.compprehension.adapters;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.EducationResourceService;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.EducationResourceRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final EducationResourceRepository educationResourceRepository;
    private final EducationResourceService educationResourceService;

    public CourseServiceImpl(CourseRepository courseRepository, EducationResourceRepository educationResourceRepository, EducationResourceService educationResourceService) {
        this.courseRepository = courseRepository;
        this.educationResourceRepository = educationResourceRepository;
        this.educationResourceService = educationResourceService;
    }

    @Override
    public CourseEntity getCurrentCourse() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);

        var courseTitle = Optional.ofNullable(parsedIdToken.getClaimAsMap("https://purl.imsglobal.org/spec/lti/claim/context"))
                .flatMap(x -> Optional.ofNullable(x.get("title").toString()))
                .orElse(null);

        if (courseTitle == null) {
            return courseRepository.findById(getInitialCourseId()).orElse(null);
        }

        var fullUrlString = parsedIdToken.getIssuer().toString();
        var educationResource = fromLtiEducationResource(fullUrlString);

        return getOrCreateCourse(courseTitle, educationResource.getId());
    }

    @Override
    public long getInitialCourseId() {
        try {
            var initialCourse = courseRepository.findByName("global").orElse(null);
            return initialCourse.getId();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Database is not initialized and does not contain global course");
        }
    }

    @Override
    public CourseEntity getOrCreateCourse(String name, Long educationResourceId) {
        Optional<CourseEntity> foundCourse = courseRepository.findByName(name);
        if (foundCourse.isPresent()) {
            return foundCourse.get();
        }

        var educationResource = educationResourceRepository.findById(educationResourceId);
        if (educationResource.isEmpty()) {
            throw new EntityNotFoundException("Education resource not found");
        }

        CourseEntity course = new CourseEntity();
        course.setName(name);
        course.setEducationResources(educationResource.get());
        return courseRepository.save(course);
    }

    @Override
    public Long getCourseIdFromQuestion(Long questionId) {
        return courseRepository.findCourseIdByQuestionId(questionId).orElse(null);
    }

    @Override
    public Long getCourseIdFromAttempt(Long attemptId) {
        return courseRepository.findCourseIdByAttemptId(attemptId).orElse(null);
    }

    @Override
    public Long getCourseIdFromExercise(Long exerciseId) {
        return courseRepository.findCourseIdByExerciseId(exerciseId).orElse(null);
    }

    private static OidcIdToken getToken(Authentication authentication) throws Exception {
        var principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser)) {
            throw new Exception("Unexpected authorized user format");
        }
        var parsedIdToken = ((OidcUser) principal).getIdToken();
        if (parsedIdToken == null) {
            throw new Exception("No id_token found");
        }
        return parsedIdToken;
    }

    private EducationResourceEntity fromLtiEducationResource(String fullUrlString) {
        URL fullUrl;
        try {
            fullUrl = new URL(fullUrlString);
        } catch (MalformedURLException e) {
            return null;
        }

        return educationResourceService.getOrCreateEducationResource(fullUrl.getHost(), fullUrl.getHost());
    }
}

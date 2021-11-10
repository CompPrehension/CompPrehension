package org.vstu.compprehension.controllers;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import lombok.var;
import org.apache.jena.shared.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.controllers.interfaces.ExerciseController;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.utils.Mapper;
import org.vstu.compprehension.dto.question.QuestionDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("basic")
@Log4j2
public class BasicExerciseController implements ExerciseController {

    @Autowired
    private FrontendService frontendService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Override
    public String launch(Model model, Long exerciseId, HttpServletRequest request) {
        var session = request.getSession();
        if (!session.isNew()) {
            session.invalidate();
            session = request.getSession();
        }

        session.setAttribute("exerciseId", exerciseId);

        if (exerciseId == null) {
            log.error(new Exception("exerciseId param is required"));
        }

        return "index";
    }

    /**
     * Routes all "/pages/**" requests to the corresponding pages
     * @param request Current request
     * @return Html
     */
    @RequestMapping(value = {"/pages/**"}, method = { RequestMethod.GET })
    public String pages(HttpServletRequest request) {
        return "index";
    }

    @Override
    public FeedbackDto addQuestionAnswer(InteractionDto interaction, HttpServletRequest request) throws Exception {
        return frontendService.addQuestionAnswer(interaction);
    }

    @Override
    public QuestionDto generateQuestion(Long exAttemptId, HttpServletRequest request) throws Exception {
        val locale = LocaleContextHolder.getLocale();;
        return frontendService.generateQuestion(exAttemptId);
    }

    @Override
    public QuestionDto generateSupplementaryQuestion(SupplementaryQuestionRequestDto questionRequest, HttpServletRequest request) throws Exception {
        val locale = LocaleContextHolder.getLocale();;
        return frontendService.generateSupplementaryQuestion(questionRequest.getExerciseAttemptId(), questionRequest.getQuestionId(), questionRequest.getViolationLaws());
    }

    @Override
    public QuestionDto getQuestion(Long questionId, HttpServletRequest request) throws Exception {
        return frontendService.getQuestion(questionId);
    }

    @Override
    public FeedbackDto generateNextCorrectAnswer(@RequestParam Long questionId, HttpServletRequest request) throws Exception {
        val locale = LocaleContextHolder.getLocale();;
        return frontendService.generateNextCorrectAnswer(questionId);
    }

    @Override
    public SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception {
        val session = request.getSession();
        val currentSessionInfo = (SessionInfoDto)session.getAttribute("sessionInfo");
        if (currentSessionInfo != null) {
            return currentSessionInfo;
        }

        val user = getCurrentUser(request);
        val exerciseId = (Long)session.getAttribute("exerciseId");
        val exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("exercise"));
        val sessionInfo = SessionInfoDto.builder()
                .sessionId(session.getId())
                .exercise(new ExerciseInfoDto(exerciseId, exercise.getOptions()))
                .user(user)
                .language(user.getPreferredLanguage().toLocaleString())
                .build();
        session.setAttribute("sessionInfo", sessionInfo);

        return sessionInfo;
    }

    @Override
    public UserInfoDto getCurrentUser(HttpServletRequest request) throws Exception {
        val session = request.getSession();
        val currentUserInfo = (UserInfoDto)session.getAttribute("currentUserInfo");
        if (currentUserInfo != null) {
            return currentUserInfo;
        }

        val userEntity = userService.createOrUpdateFromAuthentication();
        val userEntityDto = Mapper.toDto(userEntity);
        session.setAttribute("currentUserInfo", userEntityDto);
        session.setAttribute("currentUserId", userEntityDto.getId());

        return userEntityDto;
    }

    @Override
    public ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId) {
        return frontendService.getExerciseStatistics(exerciseId);
    }

    @Override
    public List<Long> getExercises(HttpServletRequest request) throws Exception {
        return exerciseRepository.findAllIds();
    }

    @Override
    public ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        val userId = getCurrentUser(request).getId();
        return frontendService.getExistingExerciseAttempt(exerciseId, userId);
    }

    @Override
    public ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        val userId = getCurrentUser(request).getId();
        return frontendService.createExerciseAttempt(exerciseId, userId);
    }
}

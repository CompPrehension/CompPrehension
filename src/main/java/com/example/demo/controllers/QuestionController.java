package com.example.demo.controllers;

import com.example.demo.Service.*;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.businesslogic.backend.BackendFact;
import com.example.demo.models.businesslogic.backend.OntologyBackend;
import com.example.demo.models.businesslogic.frontend.QuestionMistakes;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.InteractionType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.utils.HyperText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResponseExtractor;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//TODO
public class QuestionController {

    @Autowired
    private QuestionAttemptService questionAttemptService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private InteractionService interactionService;
    
    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private ResponseService responseService;
    
    @Autowired
    private MistakeService mistakeService;
    
    @Autowired
    private ExerciseAttemptService exerciseAttemptService;
    
    @Autowired
    private ExplanationTemplateInfoService explanationTemplateInfoService;
    
    private Core core = new Core();
    
    private Strategy strategy = new Strategy();
    
    @GetMapping("/questionAttempt/{questionAttempt_id}/explanation")
    public ResponseEntity getQuestionFeedback(@PathVariable Long questionAttempt_id,
                                              Model model) {
        Feedback feedback = new Feedback();
        QuestionAttempt qa = questionAttemptService.getQuestionAttempt(
                questionAttempt_id);
        DisplayingFeedbackType dft = strategy.determineDisplayingFeedbackType(qa);
        List<Mistake> mistakesInLastResponse = null;
        List<Interaction> interactions = qa.getInteractions();
        
        //Найти последнюю интеракцию, в которой есть ошибки
        if (interactions != null && interactions.size() != 0) {
            
            Collections.sort(interactions, (o1, o2) -> {
                if (o1.getOrderNumber() > o2.getOrderNumber()) {
                    return 1;
                } else if (o1.getOrderNumber() > o2.getOrderNumber()) {
                    return -1;
                } else {
                    return 0;
                }
            });
            for (int i = interactions.size() - 1; i < 0; --i) {

                if (interactions.get(i).getMistakes() != null ||
                        interactions.get(i).getMistakes().size() != 0) {

                    mistakesInLastResponse = interactions.get(i).getMistakes();
                }
            }
        }
        
        if (mistakesInLastResponse != null) {

            Interaction interaction = new Interaction();
            
            FeedbackType feedbackType = strategy.determineFeedbackType(qa);
            interaction.setInteractionType(InteractionType.REQUEST_EXPLANATION);
            interaction.setQuestionAttempt(qa);
            
            long domainId = qa.getExerciseAttempt().getExercise().getDomain().getId();

            feedback.setHyperText(core.getDomain(domainId).makeExplanation(
                    mistakesInLastResponse, feedbackType).getText());
            
            interaction.setFeedback(feedback);
            interactionService.saveInteraction(interaction);
            
            model.addAttribute("feedback", feedback);
            model.addAttribute("displayingFeedbackType", dft);
        }
        
        //Если стратегия решила показать студент объяснение
        if (dft == DisplayingFeedbackType.SHOW) {
            //Создаем интеракцию о том, что студент увидит фидбек 
            Interaction interaction = new Interaction();
            interaction.setQuestionAttempt(qa);
            interaction.setFeedback(feedback);
            interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
            interactionService.saveInteraction(interaction);
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/questionAttempt/{questionAttempt_id}")
    public ResponseEntity recordQuestionResponse(@PathVariable Long questionAttempt_id,
                                                 @RequestParam List<Response> responses,
                                                 Model model) {

        HyperText explanation = null;
        
        Interaction interaction = new Interaction();
        interaction.setResponses(responses);
        interaction.setInteractionType(InteractionType.SEND_RESPONSE);
        
        QuestionAttempt qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        Question question = questionService.generateBusinessLogicQuestion(qa.getQuestion());
        question.addFullResponse(responses);
        List<BackendFact> facts = question.responseToFacts();
        List<Mistake> mistakes = core.getDefaultBackend().judge(question.getNegativeLaws(), 
                question.getQuestionText(), facts);

        FeedbackType feedbackType = strategy.determineFeedbackType(qa);
        if (mistakes.size() > 0) {
            
            interaction.setMistakes(mistakes);
            long domainId = qa.getExerciseAttempt().getExercise().getDomain().getId();
            
            explanation = core.getDomain(domainId).makeExplanation(mistakes,
                    feedbackType);
        }
        
        interaction.setQuestionAttempt(qa);
        
        Feedback feedback = new Feedback();
        feedback.setHyperText(explanation.getText());
        feedback.setFeedBackType(feedbackType);
        
        interaction.setFeedback(feedback);
        interactionService.saveInteraction(interaction);

        //Сохранить ошибки в бд
        for (Mistake m : mistakes) { mistakeService.saveMistake(m); }
                
        //Сохранить все ответы в бд
        for (Response r : responses) { responseService.saveResponse(r); }
                
        DisplayingFeedbackType dft = strategy.determineDisplayingFeedbackType(qa);
        model.addAttribute("feedback", feedback);
        model.addAttribute("displayingFeedbackType", dft);

        //Если стратегия решила показать студент объяснение
        if (dft == DisplayingFeedbackType.SHOW) {
            //Создаем интеракцию о том, что студент увидит фидбек 
            interaction = new Interaction();
            interaction.setQuestionAttempt(qa);
            interaction.setFeedback(feedback);
            interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
            interactionService.saveInteraction(interaction);
        }
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/questionAttempt/{questionAttempt_id}/explanation")
    public ResponseEntity recordQuestionExplanation(@PathVariable Long questionAttempt_id,
                                                    @RequestParam Feedback feedback) {

        QuestionAttempt qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        Interaction interaction = new Interaction();
        interaction.setQuestionAttempt(qa);
        interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
        interaction.setFeedback(feedback);
        interactionService.saveInteraction(interaction);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/questionAttempt/{questionAttempt_id}/reaction")
    public ResponseEntity recordStudentReaction(@PathVariable Long questionAttempt_id,
                                                @RequestParam InteractionType reaction) {

        QuestionAttempt qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        Interaction interaction = new Interaction();
        interaction.setQuestionAttempt(qa);
        interaction.setInteractionType(reaction);
        interactionService.saveInteraction(interaction);
        
        return ResponseEntity.ok().build();
    }

    
    @GetMapping("/questionAttempt/{questionAttempt_id}")
    public ResponseEntity getNewQuestion(@PathVariable Long questionAttempt_id,
                                         @RequestParam FrontEndInfo frontEndInfo) {

        ExerciseAttempt exerciseAttempt = questionAttemptService.getQuestionAttempt(
                questionAttempt_id).getExerciseAttempt();
        
        //Создаем попытку выполнения вопроса
        QuestionAttempt questionAttempt = new QuestionAttempt();
        questionAttempt.setExerciseAttempt(exerciseAttempt);

        //Генерируем вопрос        
        Question newQuestion = questionService.generateBusinessLogicQuestion(
                exerciseAttempt);

        questionAttempt.setQuestion(newQuestion.getQuestionData());
        exerciseAttempt.getQuestionAttempts().add(questionAttempt);

        //Сохраняем получившиеся попытки в базу (вместе с этим сохраняется и вопрос)
        exerciseAttemptService.saveExerciseAttempt(exerciseAttempt);
        questionAttemptService.saveQuestionAttempt(questionAttempt);
        
        return ResponseEntity.ok().build();
    }
}

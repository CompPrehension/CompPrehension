package com.example.demo.controllers;

import com.example.demo.Service.*;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.BackendFactEntity;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.InteractionType;
import com.example.demo.utils.HyperText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
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

    @Autowired
    private Strategy strategy ;
    
    @GetMapping("/questionAttempt/{questionAttempt_id}/explanation")
    public org.springframework.http.ResponseEntity getQuestionFeedback(@PathVariable Long questionAttempt_id,
                                                                       Model model) {
        FeedbackEntity feedback = new FeedbackEntity();
        QuestionAttemptEntity qa = questionAttemptService.getQuestionAttempt(
                questionAttempt_id);
        DisplayingFeedbackType dft = strategy.determineDisplayingFeedbackType(qa);
        List<MistakeEntity> mistakesInLastResponse = null;
        List<InteractionEntity> interactions = qa.getInteractions();
        
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

            InteractionEntity interaction = new InteractionEntity();
            
            FeedbackType feedbackType = strategy.determineFeedbackType(qa);
            interaction.setInteractionType(InteractionType.REQUEST_EXPLANATION);
            interaction.setQuestionAttempt(qa);
            
            String domainId = qa.getExerciseAttempt().getExercise().getDomain().getName();

            feedback.setHyperText(core.getDomain(domainId).makeExplanation(
                    mistakesInLastResponse, feedbackType).get(0).getText());
            
            interaction.setFeedback(feedback);
            interactionService.saveInteraction(interaction);
            
            model.addAttribute("feedback", feedback);
            model.addAttribute("displayingFeedbackType", dft);
        }
        
        //Если стратегия решила показать студент объяснение
        if (dft == DisplayingFeedbackType.SHOW) {
            //Создаем интеракцию о том, что студент увидит фидбек 
            InteractionEntity interaction = new InteractionEntity();
            interaction.setQuestionAttempt(qa);
            interaction.setFeedback(feedback);
            interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
            interactionService.saveInteraction(interaction);
        }
        
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/questionAttempt/{questionAttempt_id}")
    public org.springframework.http.ResponseEntity recordQuestionResponse(@PathVariable Long questionAttempt_id,
                                                                          @RequestParam List<ResponseEntity> responses,
                                                                          Model model) {

        HyperText explanation = null;
        
        InteractionEntity interaction = new InteractionEntity();
        interaction.setResponses(responses);
        interaction.setInteractionType(InteractionType.SEND_RESPONSE);
        
        QuestionAttemptEntity qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        Question question = questionService.generateBusinessLogicQuestion(qa.getQuestion());
        question.addFullResponse(responses);
        List<BackendFactEntity> facts = question.responseToFacts();
        List<BackendFactEntity> statementFacts = question.getStatementFacts();
        List<BackendFactEntity> solution = core.getDefaultBackend().solve(List.of(/*TODO*/), statementFacts, List.of(/*TODO*/));
        List<BackendFactEntity> sentence = core.getDefaultBackend().judge(List.of(/*TODO*/), statementFacts, solution, facts, List.of(/*TODO*/));
        List<MistakeEntity> mistakes = new ArrayList<>(); //TODO

        FeedbackType feedbackType = strategy.determineFeedbackType(qa);
        if (mistakes.size() > 0) {
            
            interaction.setMistakes(mistakes);
            String domainId = qa.getExerciseAttempt().getExercise().getDomain().getName();
            
            explanation = core.getDomain(domainId).makeExplanation(mistakes,
                    feedbackType).get(0);
        }
        
        interaction.setQuestionAttempt(qa);
        
        FeedbackEntity feedback = new FeedbackEntity();
        feedback.setHyperText(explanation.getText());
        feedback.setFeedBackType(feedbackType);
        
        interaction.setFeedback(feedback);
        interactionService.saveInteraction(interaction);

        //Сохранить ошибки в бд
        for (MistakeEntity m : mistakes) { mistakeService.saveMistake(m); }
                
        //Сохранить все ответы в бд
        for (ResponseEntity r : responses) { responseService.saveResponse(r); }
                
        DisplayingFeedbackType dft = strategy.determineDisplayingFeedbackType(qa);
        model.addAttribute("feedback", feedback);
        model.addAttribute("displayingFeedbackType", dft);

        //Если стратегия решила показать студент объяснение
        if (dft == DisplayingFeedbackType.SHOW) {
            //Создаем интеракцию о том, что студент увидит фидбек 
            interaction = new InteractionEntity();
            interaction.setQuestionAttempt(qa);
            interaction.setFeedback(feedback);
            interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
            interactionService.saveInteraction(interaction);
        }
        
        return org.springframework.http.ResponseEntity.ok().build();
    }
    
    @PostMapping("/questionAttempt/{questionAttempt_id}/explanation")
    public org.springframework.http.ResponseEntity recordQuestionExplanation(@PathVariable Long questionAttempt_id,
                                                                             @RequestParam FeedbackEntity feedback) {

        QuestionAttemptEntity qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        InteractionEntity interaction = new InteractionEntity();
        interaction.setQuestionAttempt(qa);
        interaction.setInteractionType(InteractionType.VIEWED_EXPLANATION);
        interaction.setFeedback(feedback);
        interactionService.saveInteraction(interaction);
        
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/questionAttempt/{questionAttempt_id}/reaction")
    public org.springframework.http.ResponseEntity recordStudentReaction(@PathVariable Long questionAttempt_id,
                                                                         @RequestParam InteractionType reaction) {

        QuestionAttemptEntity qa = questionAttemptService.getQuestionAttempt(questionAttempt_id);
        InteractionEntity interaction = new InteractionEntity();
        interaction.setQuestionAttempt(qa);
        interaction.setInteractionType(reaction);
        interactionService.saveInteraction(interaction);
        
        return org.springframework.http.ResponseEntity.ok().build();
    }

    
    @GetMapping("/questionAttempt/{questionAttempt_id}")
    public org.springframework.http.ResponseEntity getNewQuestion(@PathVariable Long questionAttempt_id,
                                                                  @RequestParam FrontEndInfo frontEndInfo) {

        ExerciseAttemptEntity exerciseAttempt = questionAttemptService.getQuestionAttempt(
                questionAttempt_id).getExerciseAttempt();
        
        //Создаем попытку выполнения вопроса
        QuestionAttemptEntity questionAttempt = new QuestionAttemptEntity();
        questionAttempt.setExerciseAttempt(exerciseAttempt);

        //Генерируем вопрос        
        Question newQuestion = questionService.generateBusinessLogicQuestion(
                exerciseAttempt);

        questionAttempt.setQuestion(newQuestion.getQuestionData());
        exerciseAttempt.getQuestionAttempts().add(questionAttempt);

        //Сохраняем получившиеся попытки в базу (вместе с этим сохраняется и вопрос)
        exerciseAttemptService.saveExerciseAttempt(exerciseAttempt);
        questionAttemptService.saveQuestionAttempt(questionAttempt);
        
        return org.springframework.http.ResponseEntity.ok().build();
    }
}

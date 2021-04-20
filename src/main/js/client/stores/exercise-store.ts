import { action, makeObservable, observable, runInAction, toJS } from "mobx";
import { ExerciseController } from "../controllers/exercise-controller";
import { Interaction } from "../types/interaction";
import { Question } from "../types/question";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { Feedback } from "../types/feedback";
import { SessionInfo } from "../types/session-info";


export class ExerciseStore {
    @observable isSessionLoading: boolean = false;
    @observable sessionInfo?: SessionInfo = undefined;
    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion?: Question = undefined;
    @observable answersHistory: [number, number][] = [];    
    @observable isQuestionLoading?: boolean = false;
    @observable isFeedbackLoading?: boolean = false;
    @observable feedback?: Feedback = undefined;

    constructor() {
        makeObservable(this);        
    }

    @action 
    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }
        if (this.isSessionLoading) {
            return;
        }

        this.isSessionLoading = true;
        const dataEither = await ExerciseController.loadSessionInfo();                                
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isSessionLoading = false;
            this.sessionInfo = data;
        });
    }

    @action
    loadExistingExerciseAttempt = async (): Promise<boolean> => {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }
       
        const exerciseId = sessionInfo.exerciseId;
        const resultEither = await ExerciseController.getExistingExerciseAttempt(exerciseId);
        const result = E.getOrElseW(() => undefined)(resultEither);

        if (!result) {
            return false;
        }

        runInAction(() => {
            this.currentAttempt = result;
        })
        return true;
    }


    @action
    createExerciseAttempt = async (): Promise<void> => {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }

        const { exerciseId } = sessionInfo;        
        const resultEither = await ExerciseController.createExerciseAttempt(+exerciseId);
        const result = E.getOrElseW(() => undefined)(resultEither);

        runInAction(() => {
            this.currentAttempt = result;
        })
    }

    @action 
    loadQuestion = async (questionId: number): Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isQuestionLoading = true;
        const dataEither = await ExerciseController.getQuestion(questionId);
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isQuestionLoading = false;
            this.currentQuestion = data;
            this.feedback = data?.feedback ?? undefined;
            this.answersHistory = data?.responses ?? [];            
        });        
    }

    @action
    generateQuestion = async (): Promise<void> => {
        const { currentAttempt } = this;
        if (!currentAttempt) {
            throw new Error("Attempt not found");
        }

        const { attemptId } = currentAttempt;
        this.isQuestionLoading = true;
        const dataEither = await ExerciseController.generateQuestion(attemptId);            
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isQuestionLoading = false;
            this.currentQuestion = data;
            this.feedback = data?.feedback ?? undefined;
            this.answersHistory = data?.responses ?? [];
            if (data) {
                this.currentAttempt?.questionIds.push(data?.questionId)
            }
        });        
    }

    @action
    generateNextCorrectAnswer = async (): Promise<void> => {
        const { currentQuestion } = this;
        if (!currentQuestion) {
            throw new Error("Current question not found");
        }

        this.isFeedbackLoading = true;
        const feedbackEither = await ExerciseController.generateNextCorrectAnswer(currentQuestion.questionId);
        const feedback = E.getOrElseW(_ => undefined)(feedbackEither);

        runInAction(() => {
            this.isFeedbackLoading = false;
            this.feedback = feedback;
            if (feedback && feedback.correctAnswers && this.isHistoryChanged(feedback.correctAnswers)) {
                this.answersHistory = feedback.correctAnswers;                    
            }            
        });
    }
     
    @action 
    sendAnswers = async () : Promise<void> => {
        const { currentQuestion, answersHistory } = this;      
        if (!currentQuestion) {
            return;
        }
        
        const body: Interaction = toJS({
            attemptId: currentQuestion.attemptId,
            questionId: currentQuestion.questionId,
            answers: answersHistory,
        })

        this.isFeedbackLoading = true;
        const feedbackEither = await ExerciseController.addQuestionAnswer(body);
        const feedback = E.getOrElseW(_ => undefined)(feedbackEither)

        runInAction(() => {
            this.isFeedbackLoading = false;
            this.feedback = feedback;
            if (feedback && feedback.correctAnswers && this.isHistoryChanged(feedback.correctAnswers)) {
                this.answersHistory = feedback.correctAnswers;                    
            }            
        });
    }

    @action 
    onAnswersChanged = (answer: [number, number]): void => {
        this.answersHistory.push(answer);
        this.sendAnswers();
    }

    @action
    updateAnswersHistory = (newHistory: [number, number][]): void => {
        this.answersHistory = [ ...newHistory ];
        this.sendAnswers();
    }

    isHistoryChanged = (newHistory: [number, number][]): boolean => {
        const { answersHistory } = this;
        return newHistory.length !== answersHistory.length || JSON.stringify(newHistory.sort()) !== JSON.stringify(answersHistory.sort());
    }
}

export const exerciseStore = new ExerciseStore();

import { action, makeObservable, observable, runInAction, toJS } from "mobx";
import { Api } from "../api";
import { Interaction } from "../types/interaction";
import { Question } from "../types/question";
import { SessionStore, sessionStore } from "./session-store";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { Feedback } from "../types/feedback";


export class ExerciseStore {
    @observable session: SessionStore;

    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion?: Question = undefined;
    @observable answersHistory: [number, number][] = [];    
    @observable isQuestionLoading?: boolean = false;
    @observable isFeedbackLoading?: boolean = false;
    @observable feedback?: Feedback = undefined;

    constructor(session: SessionStore) {
        this.session = session;

        makeObservable(this);        
    }

    @action
    loadExistingExerciseAttempt = async (): Promise<boolean> => {
        const { sessionInfo } = this.session;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }

        const urlParams = new URLSearchParams(window.location.search);
        const exerciseId = urlParams.get('exerciseId');
        if (exerciseId === null || Number.isNaN(+exerciseId)) {
            throw new Error("Invalid exerciseId url param");
        }

        const resultEither = await Api.getExistingExerciseAttempt(+exerciseId);
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
        const { sessionInfo } = this.session;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }

        const urlParams = new URLSearchParams(window.location.search);
        const exerciseId = urlParams.get('exerciseId');
        if (exerciseId === null || Number.isNaN(+exerciseId)) {
            throw new Error("Invalid exerciseId url param");
        }

        const resultEither = await Api.createExerciseAttempt(+exerciseId);
        const result = E.getOrElseW(() => undefined)(resultEither);

        runInAction(() => {
            this.currentAttempt = result;
        })
    }

    @action 
    loadQuestion = async (questionId: number): Promise<void> => {
        if (!this.session.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isQuestionLoading = true;
        const dataEither = await Api.getQuestion(questionId);
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
        const dataEither = await Api.generateQuestion(attemptId);            
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
        const feedbackEither = await Api.addQuestionAnswer(body);
        const feedback = E.getOrElseW(_ => undefined)(feedbackEither)

        runInAction(() => {
            this.isFeedbackLoading = false;
            this.feedback = feedback;
            if (this.feedback?.errors?.length) {                
                this.answersHistory.pop();
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

export const exerciseStore = new ExerciseStore(sessionStore);

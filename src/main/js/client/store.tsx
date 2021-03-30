import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question, TQuestion } from "./types/question";
import { SessionInfo, TSessionInfo } from "./types/session-info";
import { ajaxGet, ajaxPost } from "./utils/ajax";
import * as E from "fp-ts/lib/Either";
import { Feedback, TFeedback } from "./types/feedback";
import { Interaction } from "./types/interaction";



export class Store {
    @observable sessionInfo?: SessionInfo = undefined;
    @observable questionData?: Question = undefined;
    @observable answersHistory: [number, number][] = [];
    @observable isSessionLoading: boolean = true;
    @observable isQuestionLoading: boolean = false;
    @observable isFeedbackLoading: boolean = false;
    @observable feedback?: Feedback = undefined;

    constructor() {
        makeObservable(this);
    }

    @action 
    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }

        this.isSessionLoading = true;
        const dataEither = await ajaxGet('loadSessionInfo', TSessionInfo);                                
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isSessionLoading = false;
            this.sessionInfo = data;
        });
    }

    @action 
    loadQuestion = async (questionId: number): Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isQuestionLoading = true;
        const dataEither = await ajaxGet(`getQuestion?questionId=${questionId}`, TQuestion);            
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isQuestionLoading = false;
            this.questionData = data;
            this.feedback = undefined;        
            this.answersHistory = [];
        });        
    }

    @action
    generateQuestion = async (): Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        const { attemptId } = this.sessionInfo;
        this.isQuestionLoading = true;
        const dataEither = await ajaxGet(`generateQuestion?attemptId=${attemptId}`, TQuestion);            
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isQuestionLoading = false;
            this.questionData = data;
            this.feedback = undefined;        
            this.answersHistory = [];
            if (data) {
                this.sessionInfo?.questionIds.push(data?.questionId)
            }
        });        
    }
     
    @action 
    sendAnswers = async () : Promise<void> => {
        const { questionData, answersHistory } = this;      
        if (!questionData) {
            return;
        }
        
        const body: Interaction = toJS({
            attemptId: questionData.attemptId,
            questionId: questionData.questionId,
            answers: answersHistory,
        })

        this.isFeedbackLoading = true;
        const feedbackEither = await ajaxPost('addAnswer', body, TFeedback);
        const feedback = E.getOrElseW(_ => undefined)(feedbackEither)

        runInAction(() => {
            this.isFeedbackLoading = false;
            this.feedback = feedback;
            if (this.feedback?.errors.length) {                
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


const storeInstance = new Store();
export default storeInstance;

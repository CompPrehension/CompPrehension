import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question } from "./types/question";
import { SessionInfo } from "./types/session-info";
import { ajaxGet, ajaxPost } from "./utils/ajax";
import * as E from "fp-ts/lib/Either";
import { Feedback } from "./types/feedback";



export class Store {
    @observable sessionInfo?: SessionInfo = undefined;
    @observable questionData?: Question = undefined;
    @observable answersHistory: string[] = [];
    @observable isLoading: boolean = true;
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

        this.isLoading = true;
        const dataEither = await ajaxGet<SessionInfo>('loadSessionInfo')
            .finally(() => this.isLoading = false);
        const data = E.getOrElseW(_ => undefined)(dataEither);
        
        this.sessionInfo = data;
    }

    @action 
    loadQuestion = async (attemptId: string) : Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isLoading = true;
        const dataEither =  await ajaxGet<Question>(`getQuestion?attemptId=${attemptId}`)
            .finally(() => this.isLoading = false);
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.questionData = data;
            this.feedback = undefined;        
            this.answersHistory = [];
        });        
    }
     
    @action 
    sendAnswers = async () : Promise<void> => {
        const { questionData, answersHistory } = this;
        const mergedAnswers = answersHistory.join(",");
        const body = {
            attemptId: questionData?.id,
            answers: toJS(mergedAnswers),
        }

        this.isFeedbackLoading = true;
        const feedbackEither = await ajaxPost<Feedback>('addAnswer', body)
            .finally(() => this.isFeedbackLoading = false);
        const feedback = E.getOrElseW(_ => undefined)(feedbackEither)

        runInAction(() => {
            this.feedback = feedback;
            if (this.feedback?.errors.length) {                
                this.answersHistory.pop();
            }
        });
    }

    @action 
    onAnswersChanged = (answerId: string): void => {
        this.answersHistory.push(answerId);
        this.sendAnswers();
    }
}


const storeInstance = new Store();
export default storeInstance;

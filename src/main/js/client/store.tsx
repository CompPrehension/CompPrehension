import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question, SessionInfo } from "./typings/question.d";
import { ajaxGet, ajaxPost } from "./utils/ajax";


export class Store {
    @observable sessionInfo?: SessionInfo = undefined;
    @observable questionData?: Question = undefined;
    @observable answersHistory: string[] = [];
    @observable isLoading: boolean = false;
    @observable isFeedbackLoading: boolean = false;
    @observable feedbackMessages: string[] | undefined = undefined;

    constructor() {
        makeObservable(this);
    }

    @action 
    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }

        this.isLoading = true;
        const data = await ajaxGet<SessionInfo>('loadSessionInfo')
            .finally(() => this.isLoading = false);
        
        this.sessionInfo = data;
    }

    @action 
    loadQuestion = async (attemptId: string) : Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isLoading = true;
        const data = await ajaxGet<Question>(`getQuestion?attemptId=${attemptId}`)
            .finally(() => this.isLoading = false);

        this.questionData = data;
        this.feedbackMessages = undefined;        
        this.answersHistory = [];
    }
     
    sendAnswers = async () : Promise<void> => {
        const { questionData, answersHistory } = this;
        const mergedAnswers = answersHistory.join(",");
        const body = {
            attemptId: questionData?.id,
            answers: toJS(mergedAnswers),
        }

        this.isFeedbackLoading = true;
        const feedback = await ajaxPost<string[]>('addAnswer', body)
            .finally(() => this.isFeedbackLoading = false);
        this.feedbackMessages = feedback ?? [];
        if (this.feedbackMessages.length) {                
            this.answersHistory.pop();
        }
    }

    @action 
    onAnswersChanged = (answerId: string): void => {
        this.answersHistory.push(answerId);
        this.sendAnswers();
    }
}


const storeInstance = new Store();
export default storeInstance;

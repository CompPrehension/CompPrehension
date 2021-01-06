import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question, SessionInfo } from "./typings/question.d";
import { ajaxGet, ajaxPost } from "./utils/ajax";


export class Store {
    @observable sessionInfo?: SessionInfo = undefined;
    @observable questionData?: Question = undefined;
    @observable answers: any = undefined;
    @observable answersHistory: any[] = [];
    @observable isLoading: boolean = false;
    @observable isFeedbackLoading: boolean = false;
    @observable feedbackMessages: string[] = [];

    constructor() {
        makeObservable(this);
    }

    @action 
    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }

        const data = await ajaxGet<SessionInfo>('loadSessionInfo');
        runInAction(() => {
            console.log(data);
            this.sessionInfo = data;
        });
    }

    @action 
    loadQuestion = async (attemptId: string) : Promise<void> => {
        if (!this.sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.isLoading = true;
        const data = await ajaxGet<Question>(`getQuestion?attemptId=${attemptId}`);
        runInAction(() => {
            console.log(data);
            this.questionData = data;
            this.answersHistory = [];
            this.isLoading = false;
        });
    }
     
    sendAnswers = async () : Promise<void> => {
        const { answers, questionData, answersHistory } = this;
        const mergedAnswers = answersHistory.join(",");
        const body = {
            attemptId: questionData?.id,
            answers: toJS(mergedAnswers),
        }

        this.isFeedbackLoading = true;
        const feedback = await ajaxPost<string[]>('addAnswer', body);
        runInAction(() => {
            this.feedbackMessages = feedback;
            this.isFeedbackLoading = false;
        });
    }

    @action 
    onAnswersChanged = (newAnswers: any): void => {
        this.answers = newAnswers;
        this.answersHistory.push(newAnswers);
        this.sendAnswers();
    }

    getAnswers = () => this.answers;
}


const storeInstance = new Store();
export default storeInstance;
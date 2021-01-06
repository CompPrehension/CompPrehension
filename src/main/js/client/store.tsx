import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question, SessionInfo } from "./typings/question.d";
import { ajaxGet, ajaxPost } from "./utils/ajax";


export class Store {
    @observable sessionInfo?: SessionInfo = undefined;
    @observable questionData?: Question = undefined;
    @observable answers: any = undefined;
    @observable isLoading: boolean = false;

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

        //const { questionId } = this.sessionInfo;
        //if (!questionId) {
        //    throw new Error("questionId is not defined")
        //}

        this.isLoading = true;
        const data = await ajaxGet<Question>(`getQuestion?attemptId=${attemptId}`);
        runInAction(() => {
            console.log(data);
            this.questionData = data;
            this.isLoading = false;
        });
    }
     
    sendAnswers = () : Promise<void> => {
        const { answers, questionData } = this;
        const body = {
            attemptId: questionData?.id,
            answers: toJS(answers),
        }
        return ajaxPost('addAnswer', body)
    }

    @action 
    onAnswersChanged = (newAnswers: any): void => {
        this.answers = newAnswers;
        this.sendAnswers();
    }

    getAnswers = () => this.answers;
}


const storeInstance = new Store();
export default storeInstance;
import { action, makeObservable, observable, runInAction, toJS } from "mobx";
import { IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { SessionInfo } from "../types/session-info";
import { inject, injectable } from "tsyringe";
import { QuestionStore } from "./question-store";

@injectable()
export class ExerciseStore {
    @observable isSessionLoading: boolean = false;
    @observable sessionInfo?: SessionInfo = undefined;
    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion: QuestionStore;
    @observable supplementaryQuestion: QuestionStore;

    constructor(
        @inject("ExerciseController") private readonly exerciseController: IExerciseController,
        @inject(QuestionStore) currentQuestion: QuestionStore,
        @inject(QuestionStore) supplementaryQuestion: QuestionStore
    ) {
        this.currentQuestion = currentQuestion;
        this.supplementaryQuestion = supplementaryQuestion;
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
        const dataEither = await this.exerciseController.loadSessionInfo();                                
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
        const resultEither = await this.exerciseController.getExistingExerciseAttempt(exerciseId);
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
        const resultEither = await this.exerciseController.createExerciseAttempt(+exerciseId);
        const result = E.getOrElseW(() => undefined)(resultEither);

        runInAction(() => {
            this.currentAttempt = result;
        })
    }

    @action
    generateQuestion = async (): Promise<void> => {
        const { sessionInfo, currentAttempt } = this;
        if (!sessionInfo || !currentAttempt) {
            throw new Error("Session is not defined");
        }

        await this.currentQuestion.generateQuestion(currentAttempt.attemptId);
        currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
    }
}

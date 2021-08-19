import { action, makeObservable, observable, runInAction, toJS } from "mobx";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { SessionInfo } from "../types/session-info";
import { inject, injectable } from "tsyringe";
import { QuestionStore } from "./question-store";
import i18next from "i18next";

@injectable()
export class ExerciseStore {
    @observable isSessionLoading: boolean = false;
    @observable sessionInfo?: SessionInfo = undefined;
    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion: QuestionStore;
    @observable exerciseState: 'INITIAL' | 'MODAL' | 'EXERCISE' | 'COMPLETED' = 'INITIAL';

    constructor(
        @inject(ExerciseController) private readonly exerciseController: IExerciseController,
        @inject(QuestionStore) currentQuestion: QuestionStore
    ) {
        this.currentQuestion = currentQuestion;
        makeObservable(this);        
    }

    
    setExerciseState = (newState: ExerciseStore['exerciseState']) => {
        runInAction(() => {
            if (this.exerciseState !== newState) {
                this.exerciseState = newState;
            }
        })
    }

    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }
        if (this.isSessionLoading) {
            return;
        }

        runInAction(() => this.isSessionLoading = true);
        const dataEither = await this.exerciseController.loadSessionInfo();
        runInAction(() => this.isSessionLoading = false);

        if (E.isLeft(dataEither)) {
            throw dataEither.left;
        }

        this.onSessionLoaded(dataEither.right);        
    }

    private onSessionLoaded(sessionInfo: SessionInfo) {
        runInAction(() => {
            this.isSessionLoading = false;
            this.sessionInfo = sessionInfo;
            
            if (this.sessionInfo.language !== i18next.language) {
                i18next.changeLanguage(this.sessionInfo.language);
            }
        });
    }

    loadExistingExerciseAttempt = async (): Promise<boolean> => {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }
       
        const exerciseId = sessionInfo.exercise.id;
        const resultEither = await this.exerciseController.getExistingExerciseAttempt(exerciseId);
        if (E.isLeft(resultEither)) {
            throw resultEither.left;
        }
        
        const result = resultEither.right;
        if (!result) {
            return false;
        }

        runInAction(() => {
            this.currentAttempt = result;
        })
        return true;
    }


    createExerciseAttempt = async (): Promise<void> => {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }

        const exerciseId = sessionInfo.exercise.id;        
        const resultEither = await this.exerciseController.createExerciseAttempt(+exerciseId);
        if (E.isLeft(resultEither)) {
            throw resultEither.left;
        }

        runInAction(() => {
            this.currentAttempt = resultEither.right;
        })
    }
    
    generateQuestion = async (): Promise<void> => {
        const { sessionInfo, currentAttempt } = this;
        if (!sessionInfo || !currentAttempt) {
            throw new Error("Session is not defined");
        }

        await this.currentQuestion.generateQuestion(currentAttempt.attemptId);
        runInAction(() => {
            currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
        })
    }

    changeLanguage = (newLang: "EN" | "RU") => {
        runInAction(() => {
            if (this.sessionInfo && this.sessionInfo.language !== newLang) {
                this.sessionInfo.language = newLang;
                i18next.changeLanguage(newLang);
            }
        })
    }
}

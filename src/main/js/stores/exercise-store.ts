import { action, autorun, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { SessionInfo } from "../types/session-info";
import { inject, injectable } from "tsyringe";
import { QuestionStore } from "./question-store";
import i18next from "i18next";
import { Language } from "../types/language";
import { RequestError } from "../types/request-error";
@injectable()
export class ExerciseStore {
    @observable isSessionLoading: boolean = false;
    @observable sessionInfo?: SessionInfo = undefined;
    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion: QuestionStore;
    @observable exerciseState: 'LAUNCH_ERROR' | 'INITIAL' | 'MODAL' | 'EXERCISE' | 'COMPLETED';
    @observable storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError, };

    constructor(@inject(ExerciseController) private readonly exerciseController: IExerciseController,
                @inject(QuestionStore) currentQuestion: QuestionStore) {
        // calc store initial state
        if (CompPh.exerciseLaunchError) {
            this.exerciseState = 'LAUNCH_ERROR';
            this.storeState = { tag: 'ERROR', error: { message: CompPh.exerciseLaunchError } };
        } else {
            this.exerciseState = 'INITIAL';
            this.storeState = { tag: 'VALID' };
        }
        this.currentQuestion = currentQuestion;

        makeObservable(this);
        this.registerOnStrategyDecisionChangedAction();
    }

    private registerOnStrategyDecisionChangedAction = () => {
        autorun(() => {
            if (this.currentQuestion.feedback?.strategyDecision === 'FINISH' && this.exerciseState !== 'COMPLETED') {
                this.setExerciseState('COMPLETED');
            }
        })
    }

    @action
    private forceSetValidState = () => {
        if (this.storeState.tag !== 'VALID') {
            this.storeState = { tag: 'VALID' };
        }
    }
    
    @action
    setExerciseState = (newState: ExerciseStore['exerciseState']) => {        
        if (this.exerciseState !== newState) {
            this.exerciseState = newState;
        }
    }

    loadSessionInfo = flow(function* (this: ExerciseStore) {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }
        if (this.isSessionLoading) {
            return;
        }

        this.forceSetValidState();
        this.isSessionLoading = true;
        const dataEither: E.Either<RequestError, SessionInfo> = yield this.exerciseController.loadSessionInfo();
        this.isSessionLoading = false;

        if (E.isLeft(dataEither)) {
            this.storeState = { tag: 'ERROR', error: dataEither.left };
            return;
        }

        this.onSessionLoaded(dataEither.right);        
    })

    private onSessionLoaded(sessionInfo: SessionInfo) {        
        this.sessionInfo = sessionInfo;
        
        if (this.sessionInfo.language !== i18next.language) {
            i18next.changeLanguage(this.sessionInfo.language);
        }
    }

    loadExistingExerciseAttempt = flow(function* (this: ExerciseStore) {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }
       
        this.forceSetValidState();
        const exerciseId = sessionInfo.exercise.id;
        const resultEither: E.Either<RequestError, ExerciseAttempt | null> = yield this.exerciseController.getExistingExerciseAttempt(exerciseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }
        
        const result = resultEither.right;
        if (!result) {
            return false;
        }
        
        this.currentAttempt = result;
        return true;
    });


    createExerciseAttempt = flow(function* (this: ExerciseStore) {
        const { sessionInfo } = this;
        if (!sessionInfo) {
            throw new Error("Session is not defined");
        }

        this.forceSetValidState();
        const exerciseId = sessionInfo.exercise.id;        
        const resultEither: E.Either<RequestError, ExerciseAttempt> = yield this.exerciseController.createExerciseAttempt(+exerciseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }
        
        this.currentAttempt = resultEither.right;
    });
    
    generateQuestion = flow(function* (this: ExerciseStore) {
        const { sessionInfo, currentAttempt } = this;
        if (!sessionInfo || !currentAttempt) {
            throw new Error("Session is not defined");
        }
        
        this.forceSetValidState();
        yield this.currentQuestion.generateQuestion(currentAttempt.attemptId);        
        currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
    });

    @action
    changeLanguage = (newLang: Language) => {        
        if (this.sessionInfo && this.sessionInfo.language !== newLang) {
            this.sessionInfo.language = newLang;
            i18next.changeLanguage(newLang);
        }
    }
}

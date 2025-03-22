import { action, autorun, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { inject, injectable } from "tsyringe";
import { QuestionStore } from "./question-store";
import i18next from "i18next";
import { Language } from "../types/language";
import { RequestError } from "../types/request-error";
import { Survey, SurveyQuestion, SurveyResultItem } from "../types/survey";
import { SurveyController } from "../controllers/exercise/survey-controller";
import { zero } from "fp-ts/lib/OptionT";
import { getUrlParameterByName } from "../types/utils";
import { UserInfo } from "../types/user-info";
import { Exercise } from "../types/exercise";
import { IUserController, UserController } from "../controllers/exercise/user-controller";

@injectable()
export class ExerciseStore {
    @observable isSessionLoading: boolean = false;
    @observable user?: UserInfo = undefined;
    @observable exerciseId: number;
    @observable exercise?: Exercise = undefined;
    @observable currentAttemptId?: number = undefined;
    @observable currentAttempt?: ExerciseAttempt = undefined;
    @observable currentQuestion: QuestionStore;
    @observable exerciseState: 'LAUNCH_ERROR' | 'INITIAL' | 'MODAL' | 'EXERCISE' | 'COMPLETED' = 'INITIAL';
    @observable storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError, } = { tag: 'VALID' };
    @observable survey?: ExerciseSurveySettings = undefined;
    @observable isDebug = false;

    constructor(@inject(ExerciseController) private readonly exerciseController: IExerciseController,
        @inject(UserController) private readonly userController: IUserController,
        @inject(SurveyController) private readonly surveyController: SurveyController,
        @inject(QuestionStore) currentQuestion: QuestionStore) {
        // calc store initial state
        this.isDebug = getUrlParameterByName('debug') !== null;
        this.currentQuestion = currentQuestion;
        
        const rawExerciseId = getUrlParameterByName('exerciseId');
        if (rawExerciseId === null) {
            this.exerciseState = 'LAUNCH_ERROR';
            this.storeState = { tag: 'ERROR', error: { message: "Invalid exercise id" } };
        }
        this.exerciseId = rawExerciseId !== null ? +rawExerciseId : -1;

        const rawAttemptId = getUrlParameterByName('attemptId');
        if (rawAttemptId !== null) {
            this.currentAttemptId = +rawAttemptId;
        }

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

    @action
    setSurveyAnswers = (quesionId: number, answers: Record<number, string>) => {
        if (!this.survey)
            return;

        runInAction(() => {
            this.survey!.questions[quesionId].status = 'COMPLETED';            
            this.survey!.questions[quesionId].results = answers;
        })
    }

    loadSessionInfo = async () => {
        if (this.exercise) {
            throw new Error("exerciseInfo loaded");
        }
        if (this.isSessionLoading) {
            return;
        }

        runInAction(() => {
            this.forceSetValidState();
            this.isSessionLoading = true;
        })

        const [user, exercise] = await Promise.all([
            this.userController.getCurrentUser(),
            this.exerciseController.getExerciseShortInfo(this.exerciseId)
        ])
        
        if (E.isRight(user) && E.isRight(exercise)) {
            runInAction(() => {
                this.isSessionLoading = false;
                this.onSessionLoaded(user.right, exercise.right);
            })
        } else if (E.isLeft(user) && E.isLeft(exercise)) {
            runInAction(() => {
                this.isSessionLoading = false;
                this.storeState = { tag: 'ERROR', error: user.left ?? exercise.left };
            })
        }
    };

    private onSessionLoaded(user: UserInfo, exercise: Exercise) {
        runInAction(() => {
            this.user = user;
            this.exercise = exercise;

            if (user.language !== i18next.language) {
                i18next.changeLanguage(user.language);
            }
        });
    }


    loadExerciseAttempt = flow(function* (this: ExerciseStore, attemptId: number) {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exerciseInfo is not defined");
        }

        this.forceSetValidState();
        const exerciseId = exercise.id;
        const resultEither: E.Either<RequestError, ExerciseAttempt | null> = yield this.exerciseController.getExerciseAttempt(attemptId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        const result = resultEither.right;
        if (!result) {
            return false;
        }

        this.currentAttempt = result;
        yield this.onAttemptLoaded();
        return true;
    });

    loadExistingExerciseAttempt = flow(function* (this: ExerciseStore) {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const exerciseId = exercise.id;
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
        yield this.onAttemptLoaded();
        return true;
    });


    onAttemptLoaded = async () => {
        await this.loadSurvey();
    }

    createExerciseAttempt = flow(function* (this: ExerciseStore) {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const exerciseId = exercise.id;
        const resultEither: E.Either<RequestError, ExerciseAttempt> = yield this.exerciseController.createExerciseAttempt(+exerciseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        this.currentAttempt = resultEither.right;
        yield this.onAttemptLoaded();
    });

    createDebugExerciseAttempt = flow(function* (this: ExerciseStore) {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const exerciseId = exercise.id;
        const resultEither: E.Either<RequestError, ExerciseAttempt> = yield this.exerciseController.createDebugExerciseAttempt(+exerciseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        this.currentAttempt = resultEither.right;
        yield this.onAttemptLoaded();
    });

    generateQuestion = flow(function* (this: ExerciseStore) {
        const { exercise, currentAttempt } = this;
        if (!exercise || !currentAttempt) {
            throw new Error("Session is not defined");
        }

        this.forceSetValidState();
        yield this.currentQuestion.generateQuestion(currentAttempt.attemptId);
        currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
    });

    @action
    changeLanguage = (newLang: Language) => {
        if (this.user && this.user.language !== newLang) {
            this.user.language = newLang;
            i18next.changeLanguage(newLang);
        }
    }

    @action
    loadSurvey = async () => {
        if (this.survey || !this.currentAttempt || !this.exercise)
            return;
        if (!this.exercise.options.surveyOptions?.enabled || this.exercise.options.surveyOptions.surveyId.length === 0)
            return;

        const surveyId = this.exercise.options.surveyOptions.surveyId;
        const attemptId = this.currentAttempt.attemptId;
        const [survey, surveyResults] = await Promise.all([
            this.surveyController.getSurvey(surveyId),
            this.surveyController.getCurrentUserAttemptSurveyVotes(surveyId, attemptId),
        ]);

        runInAction(() => {
            if (E.isRight(survey) && E.isRight(surveyResults)) {
                const tmp = groupBy(surveyResults.right, x => x.questionId)
                this.survey = {
                    survey: survey.right,
                    questions: [...tmp.keys()].map(k => ({
                        questionId: k,
                        status: 'COMPLETED' as const,
                        questions: tmp.get(k)?.map(z => z.surveyQuestionId) ?? [],
                        results: tmp.get(k)?.reduce((acc, z) => (acc[z.surveyQuestionId] = z.answer, acc), {} as Record<number, string>) ?? {},
                    })).reduce((acc, i) => (acc[i.questionId] = i, acc), {} as Record<number, QuestionSurveyResult>),
                }
            }
        })
    }

    @action
    ensureQuestionSurveyExists = (questionId: number) => {
        if (this.survey?.questions[questionId])
            return this.survey?.questions[questionId].questions;

        const qs: SurveyQuestion[] = [];
        const currentQuestionIdx = this.currentAttempt!.questionIds.findIndex(z => z === this.currentQuestion.question?.questionId)
        for (let q of this.survey?.survey.questions || []) {
            const policy = q.policy;
            if (policy.kind === 'AFTER_EACH'
                || policy.kind === 'AFTER_FIRST' && currentQuestionIdx === 0
                || policy.kind === 'AFTER_LAST' && this.exerciseState === 'COMPLETED'
                || policy.kind === 'AFTER_SPECIFIC' && policy.numbers.includes(currentQuestionIdx + 1)) {
                qs.push(q);
            }
        }
        console.log("Selected questions")
        console.log(toJS(qs))

        var questionSurvey: QuestionSurveyResult = {
            questionId: questionId,
            status: 'ACTIVE',
            questions: qs.map(z => z.id),
            results: {},
        };

        runInAction(() => {
            this.survey!.questions[questionId] = questionSurvey;
        })
        return qs.map(z => z.id);
    }
}

function groupBy<T, K>(list: T[], keyGetter: (z: T) => K) {
    const map = new Map<K, T[]>();
    list.forEach((item) => {
        const key = keyGetter(item);
        const collection = map.get(key);
        if (!collection) {
            map.set(key, [item]);
        } else {
            collection.push(item);
        }
    });
    return map;
}

type ExerciseSurveySettings = {
    survey: Survey,
    questions: Record<number, QuestionSurveyResult>,
}

type QuestionSurveyResult = {
    questionId: number,
    status: 'ACTIVE' | 'COMPLETED',
    questions: number[],
    results: Record<number, string>,
}
